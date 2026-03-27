/*
 * deti_coin_client_cpu.c
 *
 * Distributed DETI Coin Mining - CPU Client Component
 *
 * Features:
 * - Connects to mining server for work coordination
 * - Uses AVX2 SIMD for efficient CPU mining
 * - Reports found coins to server
 * - Requests new work batches automatically
 * - Provides periodic statistics updates
 *
 * Build: cc -march=native -mavx2 -O3 -Wall -Wshadow -Werror deti_coin_client_cpu.c -o deti_coin_client_cpu
 *
 * Usage: ./deti_coin_client_cpu <server_ip> [port]
 *        Example: ./deti_coin_client_cpu 192.168.1.100 9999
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"

#define DEFAULT_PORT 9999
#define N_LANES 8
#define STATS_INTERVAL 10000000ULL  // Report stats every 10M attempts

static volatile int keep_running = 1;

static void int_handler(int signum) {
    (void)signum;
    keep_running = 0;
}

static unsigned count_leading_zero_bits(u32_t hash[5]) {
    unsigned n = 0u;
    for (unsigned word = 1u; word <= 4u; ++word) {
        u32_t v = hash[word];
        if (v == 0u) {
            n += 32u;
            continue;
        }
        n += __builtin_clz(v);
        return n;
    }
    return n;
}

// Send message to server
int send_message(int sock, const char *msg) {
    return send(sock, msg, strlen(msg), 0) > 0;
}

// Receive message from server
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
    
    printf("========================================\n");
    printf("DETI Coin Mining Client (CPU/AVX2)\n");
    printf("========================================\n");
    printf("Server: %s:%d\n", server_ip, port);
    printf("SIMD Lanes: %d (AVX2)\n", N_LANES);
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
    
    const char *prefix = "DETI coin 2 ";
    
    // Data structures for AVX2
    union { u08_t c[14*4]; u32_t i[14]; } data[N_LANES];
    u32_t interleaved_data[14][N_LANES] __attribute__((aligned(32)));
    u32_t interleaved_hash[5][N_LANES] __attribute__((aligned(32)));
    
    // Initialize random seeds
    unsigned int seeds[N_LANES];
    for (int lane = 0; lane < N_LANES; ++lane) {
        seeds[lane] = (unsigned int)time(NULL) + lane * 12345;
    }
    
    // Pre-initialize fixed parts
    for (int lane = 0; lane < N_LANES; ++lane) {
        memset(data[lane].c, 0, 14*4);
        for (int j = 0; j < 12; ++j)
            data[lane].c[j ^ 3] = (u08_t)prefix[j];
        data[lane].c[54 ^ 3] = (u08_t)'\n';
        data[lane].c[55 ^ 3] = 0x80u;
    }
    
    unsigned long long total_attempts = 0ull;
    unsigned total_found = 0u;
    unsigned long long last_stats_report = 0ull;
    
    struct timespec start_time;
    clock_gettime(CLOCK_MONOTONIC_RAW, &start_time);
    
    while (keep_running) {
        // Request work from server
        char buffer[256];
        if (!send_message(sock, "READY\n")) {
            printf("[CLIENT] Failed to send READY\n");
            break;
        }
        
        if (!recv_message(sock, buffer, sizeof(buffer))) {
            printf("[CLIENT] Server disconnected\n");
            break;
        }
        
        // Parse work assignment
        unsigned long long nonce_start, nonce_count;
        if (sscanf(buffer, "WORK %llu %llu", &nonce_start, &nonce_count) != 2) {
            printf("[CLIENT] Invalid work assignment: %s\n", buffer);
            break;
        }
        
        printf("[CLIENT] Received work: nonce %llu - %llu (%llu attempts)\n",
               nonce_start, nonce_start + nonce_count, nonce_count);
        
        // Mine the assigned range
        unsigned long long work_attempts = 0ull;
        
        while (work_attempts < nonce_count && keep_running) {
            // Generate random messages
            for (int lane = 0; lane < N_LANES; ++lane) {
                for (unsigned i = 12; i <= 53; ++i) {
                    unsigned char val;
                    do {
                        val = 0x20 + (rand_r(&seeds[lane]) % (0x7E - 0x20 + 1));
                    } while (val == '\n');
                    data[lane].c[i ^ 3] = val;
                }
            }
            
            // Interleave for AVX2
            for (int lane = 0; lane < N_LANES; ++lane)
                for (int i = 0; i < 14; ++i)
                    interleaved_data[i][lane] = data[lane].i[i];
            
            // Compute hashes
            sha1_avx2((v8si *)&interleaved_data[0], (v8si *)&interleaved_hash[0]);
            work_attempts += N_LANES;
            total_attempts += N_LANES;
            
            // Check for coins
            for (int lane = 0; lane < N_LANES; ++lane) {
                if (interleaved_hash[0][lane] == 0xAAD20250u) {
                    u32_t hash_lane[5];
                    for (int w = 0; w < 5; ++w)
                        hash_lane[w] = interleaved_hash[w][lane];
                    
                    unsigned zeros = count_leading_zero_bits(hash_lane);
                    
                    if (zeros > 0) {
                        // Report coin to server
                        char coin_msg[512];
                        snprintf(coin_msg, sizeof(coin_msg),
                                "COIN %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x %08x\n",
                                data[lane].i[0], data[lane].i[1], data[lane].i[2], data[lane].i[3],
                                data[lane].i[4], data[lane].i[5], data[lane].i[6], data[lane].i[7],
                                data[lane].i[8], data[lane].i[9], data[lane].i[10], data[lane].i[11],
                                data[lane].i[12], data[lane].i[13]);
                        
                        if (send_message(sock, coin_msg)) {
                            printf("\n💎 COIN FOUND! Value: V%02u\n", zeros);
                            printf("Hash: ");
                            for (int i = 0; i < 5; ++i) printf("%08x", hash_lane[i]);
                            printf("\n\n");
                            total_found++;
                            
                            recv_message(sock, buffer, sizeof(buffer));  // Wait for ACK
                        }
                    }
                }
            }
            
            // Periodic statistics report to server
            if (total_attempts - last_stats_report >= STATS_INTERVAL) {
                struct timespec cur_time;
                clock_gettime(CLOCK_MONOTONIC_RAW, &cur_time);
                double elapsed = (double)(cur_time.tv_sec - start_time.tv_sec) +
                                1e-9 * (double)(cur_time.tv_nsec - start_time.tv_nsec);
                double hps = elapsed > 0.0 ? (double)total_attempts / elapsed : 0.0;
                
                printf("[CLIENT] Attempts: %llu, Hash rate: %.2f MH/s, Found: %u\n",
                       total_attempts, hps / 1e6, total_found);
                
                char stats_msg[128];
                snprintf(stats_msg, sizeof(stats_msg), "STATS %llu %u\n", 
                        total_attempts, total_found);
                send_message(sock, stats_msg);
                recv_message(sock, buffer, sizeof(buffer));  // Wait for ACK
                
                last_stats_report = total_attempts;
            }
        }
        
        // Report final statistics for this work batch to server
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
        
        // Report work completion and wait for acknowledgment
        if (!send_message(sock, "DONE\n")) {
            printf("[CLIENT] Failed to send DONE\n");
            break;
        }
        if (!recv_message(sock, buffer, sizeof(buffer))) {
            printf("[CLIENT] Server disconnected after DONE\n");
            break;
        }
    }
    
    // Final statistics
    struct timespec end_time;
    clock_gettime(CLOCK_MONOTONIC_RAW, &end_time);
    double total_time = (double)(end_time.tv_sec - start_time.tv_sec) +
                       1e-9 * (double)(end_time.tv_nsec - start_time.tv_nsec);
    double hps = total_time > 0.0 ? (double)total_attempts / total_time : 0.0;
    
    printf("\n========================================\n");
    printf("CLIENT FINAL STATISTICS\n");
    printf("========================================\n");
    printf("Total attempts: %llu\n", total_attempts);
    printf("Time: %.1f seconds\n", total_time);
    printf("Hash rate: %.2f MH/s\n", hps / 1e6);
    printf("Coins found: %u\n", total_found);
    printf("========================================\n");
    
    close(sock);
    return 0;
}
