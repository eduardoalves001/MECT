/*
 * deti_coin_client_cuda.cu
 *
 * Distributed DETI Coin Mining - CUDA Client Component
 *
 * Features:
 * - Connects to mining server for work coordination
 * - Uses CUDA GPU for high-performance mining
 * - Reports found coins to server
 * - Automatic work batch management
 *
 * Build: nvcc -O3 -use_fast_math -arch=sm_60 deti_coin_client_cuda.cu -o deti_coin_client_cuda -lcurand
 *
 * Usage: ./deti_coin_client_cuda <server_ip> [port]
 *        Example: ./deti_coin_client_cuda 192.168.1.100 9999
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>
#include <cuda_runtime.h>
#include <curand_kernel.h>

#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1.h"

#define DEFAULT_PORT 9999
#define THREADS_PER_BLOCK 256
#define BLOCKS_PER_GRID 4096
#define TOTAL_THREADS (THREADS_PER_BLOCK * BLOCKS_PER_GRID)
#define ATTEMPTS_PER_THREAD 10000
#define COINS_BUFFER_SIZE (1024 * 1024)

static volatile int keep_running = 1;

#define CUDA_CHECK(call) do { \
    cudaError_t err = call; \
    if (err != cudaSuccess) { \
        fprintf(stderr, "CUDA error at %s:%d - %s\n", __FILE__, __LINE__, cudaGetErrorString(err)); \
        exit(EXIT_FAILURE); \
    } \
} while(0)

static void int_handler(int signum) {
    (void)signum;
    keep_running = 0;
}

__device__ unsigned count_leading_zero_bits_device(u32_t hash[5]) {
    unsigned n = 0u;
    for (unsigned word = 1u; word <= 4u; ++word) {
        u32_t v = hash[word];
        if (v == 0u) {
            n += 32u;
            continue;
        }
        n += __clz(v);
        return n;
    }
    return n;
}

__global__ void cuda_mine_deti_coins(
    u32_t *coins_storage_area,
    unsigned long long seed_offset
) {
    int tid = blockIdx.x * blockDim.x + threadIdx.x;
    
    curandState state;
    curand_init(1234567ULL + (seed_offset * 1000000ULL) + tid, 0, 0, &state);
    
    union {
        u32_t w[14];
        u08_t c[56];
    } udata;
    
    const char prefix[] = "DETI coin 2 ";
    for (int i = 0; i < 12; i++) {
        udata.c[i ^ 3] = prefix[i];
    }
    
    udata.c[54 ^ 3] = '\n';
    udata.c[55 ^ 3] = 0x80;
    
    for (int attempt = 0; attempt < ATTEMPTS_PER_THREAD; attempt++) {
        for (int i = 12; i < 54; i++) {
            unsigned char val;
            do {
                val = curand(&state) % 95 + 0x20;
            } while (val == '\n');
            udata.c[i ^ 3] = val;
        }
        
        u32_t hash[5];
        
        #define T            u32_t
        #define C(c)         (c)
        #define ROTATE(x,n)  (((x) << (n)) | ((x) >> (32 - (n))))
        #define DATA(idx)    udata.w[idx]
        #define HASH(idx)    hash[idx]
        
        CUSTOM_SHA1_CODE();
        
        #undef T
        #undef C
        #undef ROTATE
        #undef DATA
        #undef HASH
        
        if (hash[0] == 0xAAD20250u) {
            unsigned zeros = count_leading_zero_bits_device(hash);
            if (zeros > 0) {
                u32_t current = coins_storage_area[0];
                if (current + 16u <= COINS_BUFFER_SIZE - 1u) {
                    u32_t idx = atomicAdd(coins_storage_area, 16u);
                    
                    if (idx + 16u <= COINS_BUFFER_SIZE - 1u) {
                        // Store variable message words 3-13 (11 words)
                        for (int m = 3; m < 14; m++) {
                            coins_storage_area[1 + idx + (m - 3)] = udata.w[m];
                        }
                        // Store hash (5 words)
                        for (int h = 0; h < 5; h++) {
                            coins_storage_area[1 + idx + 11 + h] = hash[h];
                        }
                    }
                }
            }
        }
    }
}

int send_message(int sock, const char *msg) {
    return send(sock, msg, strlen(msg), 0) > 0;
}

int recv_message(int sock, char *buffer, size_t size) {
    ssize_t bytes = recv(sock, buffer, size - 1, 0);
    if (bytes <= 0) return 0;
    buffer[bytes] = '\0';
    return 1;
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        printf("Usage: %s <server_ip> [port]\n", argv[0]);
        return 1;
    }
    
    const char *server_ip = argv[1];
    int port = (argc > 2) ? atoi(argv[2]) : DEFAULT_PORT;
    
    signal(SIGINT, int_handler);
    
    cudaDeviceProp prop;
    CUDA_CHECK(cudaGetDeviceProperties(&prop, 0));
    
    printf("========================================\n");
    printf("DETI Coin Mining Client (CUDA)\n");
    printf("========================================\n");
    printf("Server: %s:%d\n", server_ip, port);
    printf("GPU: %s\n", prop.name);
    printf("Configuration: %d × %d = %d threads\n", 
           BLOCKS_PER_GRID, THREADS_PER_BLOCK, TOTAL_THREADS);
    printf("Attempts per kernel: %llu\n",
           (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD);
    printf("========================================\n\n");
    
    // Connect to server
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        perror("socket");
        return 1;
    }
    
    struct sockaddr_in server_addr = {0};
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(port);
    inet_pton(AF_INET, server_ip, &server_addr.sin_addr);
    
    if (connect(sock, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
        perror("connect");
        close(sock);
        return 1;
    }
    
    printf("[CLIENT] Connected to server!\n\n");
    
    // Allocate GPU memory
    u32_t *d_coins_storage_area;
    u32_t *h_coins_storage_area = (u32_t *)malloc(COINS_BUFFER_SIZE * sizeof(u32_t));
    CUDA_CHECK(cudaMalloc(&d_coins_storage_area, COINS_BUFFER_SIZE * sizeof(u32_t)));
    
    unsigned long long total_attempts = 0ull;
    unsigned total_found = 0u;
    unsigned long long kernel_count = 0ull;
    
    struct timespec start_time;
    clock_gettime(CLOCK_MONOTONIC_RAW, &start_time);
    
    while (keep_running) {
        // Request work
        char buffer[256];
        if (!send_message(sock, "READY\n")) {
            printf("[CLIENT] Failed to send READY\n");
            break;
        }
        
        if (!recv_message(sock, buffer, sizeof(buffer))) {
            printf("[CLIENT] Server disconnected\n");
            break;
        }
        
        unsigned long long nonce_start, nonce_count;
        if (sscanf(buffer, "WORK %llu %llu", &nonce_start, &nonce_count) != 2) {
            printf("[CLIENT] Invalid work assignment\n");
            break;
        }
        
        printf("[CLIENT] Received work: %llu attempts\n", nonce_count);
        
        // Mine the assigned range
        unsigned long long work_attempts = 0ull;
        unsigned long long attempts_per_kernel = 
            (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD;
        
        while (work_attempts < nonce_count && keep_running) {
            // Clear counter
            CUDA_CHECK(cudaMemset(d_coins_storage_area, 0, sizeof(u32_t)));
            
            // Run kernel
            cuda_mine_deti_coins<<<BLOCKS_PER_GRID, THREADS_PER_BLOCK>>>(
                d_coins_storage_area, kernel_count++);
            CUDA_CHECK(cudaDeviceSynchronize());
            
            work_attempts += attempts_per_kernel;
            total_attempts += attempts_per_kernel;
            
            // Check for coins
            CUDA_CHECK(cudaMemcpy(h_coins_storage_area, d_coins_storage_area,
                                 sizeof(u32_t), cudaMemcpyDeviceToHost));
            
            u32_t num_coins = h_coins_storage_area[0] / 16;
            
            if (num_coins > 0) {
                // Transfer coin data
                u32_t transfer_size = h_coins_storage_area[0] + 1;
                if (transfer_size > COINS_BUFFER_SIZE) transfer_size = COINS_BUFFER_SIZE;
                
                CUDA_CHECK(cudaMemcpy(h_coins_storage_area, d_coins_storage_area,
                                     transfer_size * sizeof(u32_t),
                                     cudaMemcpyDeviceToHost));
                
                // Report coins to server
                for (u32_t c = 0; c < num_coins; c++) {
                    u32_t offset = 1 + (c * 16);
                    if (offset + 16 > transfer_size) break;
                    
                    // Kernel stores: [msg[3-13], hash[0-4]] = 16 words total
                    // We need to send: [msg[0-13]] = full message
                    
                    u32_t message[14];
                    
                    // Fixed prefix "DETI coin 2 " (words 0-2)
                    const char prefix[] = "DETI coin 2 ";
                    for (int i = 0; i < 12; i++) {
                        ((u08_t*)message)[i ^ 3] = prefix[i];
                    }
                    
                    // Variable message words 3-13 (11 words from storage offset 0-10)
                    for (int m = 0; m < 11; m++) {
                        message[3 + m] = h_coins_storage_area[offset + m];
                    }
                    
                    // Get hash for display (stored at offset 11-15)
                    u32_t hash[5];
                    for (int h = 0; h < 5; h++) {
                        hash[h] = h_coins_storage_area[offset + 11 + h];
                    }
                    unsigned zeros = 0;
                    for (unsigned word = 1; word <= 4; word++) {
                        u32_t v = hash[word];
                        if (v == 0) {
                            zeros += 32;
                            continue;
                        }
                        zeros += __builtin_clz(v);
                        break;
                    }
                    
                    char coin_msg[512];
                    snprintf(coin_msg, sizeof(coin_msg),
                            "COIN %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x\n",
                            message[0], message[1], message[2], message[3],
                            message[4], message[5], message[6], message[7],
                            message[8], message[9], message[10], message[11],
                            message[12], message[13]);
                    
                    if (send_message(sock, coin_msg)) {
                        printf("👻 COIN FOUND! Value: V%02u\n", zeros);
                        printf("Hash: %08x%08x%08x%08x%08x\n", 
                               hash[0], hash[1], hash[2], hash[3], hash[4]);
                        total_found++;
                        recv_message(sock, buffer, sizeof(buffer));
                    }
                }
            }
            
            // Periodic status
            if (kernel_count % 10 == 0) {
                struct timespec cur_time;
                clock_gettime(CLOCK_MONOTONIC_RAW, &cur_time);
                double elapsed = (double)(cur_time.tv_sec - start_time.tv_sec) +
                                1e-9 * (double)(cur_time.tv_nsec - start_time.tv_nsec);
                double hps = elapsed > 0.0 ? (double)total_attempts / elapsed : 0.0;
                
                printf("[CLIENT] Attempts: %llu, Hash rate: %.2f GH/s, Found: %u\n",
                       total_attempts, hps / 1e9, total_found);
            }
        }
        
        // Report statistics to server after completing work batch
        char stats_msg[128];
        snprintf(stats_msg, sizeof(stats_msg), "STATS %llu %u\n",
                total_attempts, total_found);
        if (!send_message(sock, stats_msg)) {
            printf("[CLIENT] Failed to send STATS\n");
            break;
        }
        if (!recv_message(sock, buffer, sizeof(buffer))) {
            printf("[CLIENT] Server disconnected after STATS\n");
            break;
        }
        
        // Notify server work is complete and wait for acknowledgment
        if (!send_message(sock, "DONE\n")) {
            printf("[CLIENT] Failed to send DONE\n");
            break;
        }
        if (!recv_message(sock, buffer, sizeof(buffer))) {
            printf("[CLIENT] Server disconnected after DONE\n");
            break;
        }
    }
    
    // Cleanup
    free(h_coins_storage_area);
    cudaFree(d_coins_storage_area);
    close(sock);
    
    return 0;
}
