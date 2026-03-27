/*
 * sha1_cuda_special_search.cu
 *
 * CUDA-accelerated search for DETI coins with special form (e.g., "Eduardo" in the middle).
 * Follows standard DETI coin rules with "DETI coin 2 " prefix.
 *
 * Strategy: Generate DETI coins with "DETI coin 2 " prefix, then
 * place "Eduardo" at positions 12-18 (7 bytes), followed by random content.
 * This gives us 95^35 combinations while maintaining the special pattern.
 *
 * Message format:
 * - Bytes 0-11: "DETI coin 2 " (FIXED PREFIX)
 * - Bytes 12-18: "Eduardo" (special form - FIXED)
 * - Bytes 19-53: Random (printable ASCII, no '\n')
 * - Byte 54: '\n'
 * - Byte 55: 0x80 (SHA1 padding)
 *
 * Build: 
 *   nvcc -O3 -use_fast_math -arch=sm_60 sha1_cuda_special_search.cu -o sha1_cuda_special_search -lcurand
 */ 

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <signal.h>
#include <cuda_runtime.h>
#include <curand_kernel.h>

#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1.h"
#include "aad_sha1_cpu.h"

// CUDA configuration - production settings (optimized)
#define THREADS_PER_BLOCK 256
#define BLOCKS_PER_GRID 4096
#define TOTAL_THREADS (THREADS_PER_BLOCK * BLOCKS_PER_GRID)
#define ATTEMPTS_PER_THREAD 10000

// Maximum coins storable in buffer (in u32_t words)
// Position 0 = next free index (atomicAdd counter)
// Position 1+ = coin data (14 words per coin: 9 message words + 5 hash words)
#define COINS_BUFFER_SIZE 1024

static volatile int keep_running = 1;

/* Signal handler for Ctrl+C */
static void int_handler(int signum){
  (void)signum;
  keep_running = 0;
}

// CUDA error checking macro
#define CUDA_CHECK(call) do { \
  cudaError_t err = call; \
  if (err != cudaSuccess) { \
    fprintf(stderr, "CUDA error at %s:%d - %s\n", __FILE__, __LINE__, cudaGetErrorString(err)); \
    exit(EXIT_FAILURE); \
  } \
} while(0)

// Minimal vault implementation
void save_coin(u32_t *coin) {
  static FILE *vault_file = NULL;
  
  if (coin == NULL) {
    if (vault_file) {
      fflush(vault_file);
      fclose(vault_file);
      vault_file = NULL;
    }
    return;
  }
  
  if (vault_file == NULL) {
    vault_file = fopen("deti_coins_v2_vault_special.txt", "a");
    if (vault_file == NULL) {
      fprintf(stderr, "Warning: Could not open vault file\n");
      return;
    }
  }
  
  // Calculate SHA-1 hash
  u32_t hash[5];
  sha1(coin, hash);
  
  // Count leading zeros
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
  
  // Extract readable message text (first 54 bytes, excluding \n and 0x80 padding)
  char msg_text[55];
  for (int i = 0; i < 54; i++) {
    char c = ((u08_t*)coin)[i ^ 3];
    // Replace non-printable chars with '.'
    msg_text[i] = (c >= 32 && c < 127) ? c : '.';
  }
  msg_text[54] = '\0';
  
  // Write in simple one-line format: V<value>:<message text>
  fprintf(vault_file, "V%02u:%s\n", zeros, msg_text);
  fflush(vault_file);
}

// Device function to count leading zero bits
__device__ unsigned count_leading_zero_bits_device(u32_t hash[5]){
  unsigned n = 0u;
  for(unsigned word = 1u; word <= 4u; ++word) {
    u32_t v = hash[word];
    if(v == 0u){
      n += 32u;
      continue;
    }
    n += __clz(v);
    return n;
  }
  return n;
}

// CUDA kernel for mining DETI coins with "Eduardo" pattern (atomicAdd optimized)
// coins_storage_area[0] = next free index (managed by atomicAdd)
// coins_storage_area[1+] = coin data (14 words per coin: 9 message words + 5 hash words)
__global__ void cuda_mine_special_coins(
  u32_t *coins_storage_area,
  unsigned long long seed_offset
) {
  int tid = blockIdx.x * blockDim.x + threadIdx.x;
  
  // Initialize cuRAND state with unique seed per thread
  curandState state;
  curand_init(1234567ULL + (seed_offset * 1000000ULL) + tid, 0, 0, &state);
  
  // Message buffer
  union {
    u32_t w[14];
    u08_t c[56];
  } udata;
  
  // Each thread performs multiple attempts
  for (int attempt = 0; attempt < ATTEMPTS_PER_THREAD; attempt++) {
    // Positions 0-11: "DETI coin 2 " (FIXED PREFIX - 12 bytes)
    const char prefix[] = "DETI coin 2 ";
    for (int i = 0; i < 12; i++) {
      udata.c[i ^ 3] = prefix[i];
    }
    
    // Positions 12-18: "Eduardo" (7 bytes) - FIXED (THIS IS THE SPECIAL PART!)
    udata.c[12 ^ 3] = 'E';
    udata.c[13 ^ 3] = 'd';
    udata.c[14 ^ 3] = 'u';
    udata.c[15 ^ 3] = 'a';
    udata.c[16 ^ 3] = 'r';
    udata.c[17 ^ 3] = 'd';
    udata.c[18 ^ 3] = 'o';
    
    // Positions 19-53: random (35 bytes)
    for (int i = 19; i < 54; i++) {
      unsigned char val;
      do {
        val = curand(&state) % 95 + 0x20;
      } while (val == '\n');
      udata.c[i ^ 3] = val;
    }
    
    // Position 54: newline
    udata.c[54 ^ 3] = '\n';
    
    // Position 55: SHA1 padding
    udata.c[55 ^ 3] = 0x80;
    
    // Compute SHA1 hash
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
    
    // Check if this is a valid DETI coin (signature match)
    if (hash[0] == 0xAAD20250u) {
      // Count leading zeros
      unsigned zeros = count_leading_zero_bits_device(hash);
      if (zeros > 0) {
        // Reserve space atomically (16 words per coin: 11 message words + 5 hash words)
        u32_t idx = atomicAdd(coins_storage_area, 16u);
        
        if(idx < COINS_BUFFER_SIZE - 16u) {
          // Store variable message words 3-13 (includes all random data + padding)
          for(int m = 3; m < 14; m++) {
            coins_storage_area[idx + (m - 3)] = udata.w[m];
          }
          // Store hash (5 words)
          for(int h = 0; h < 5; h++) {
            coins_storage_area[idx + 11 + h] = hash[h];
          }
        }
      }
    }
  }
}

int main(void) {
  signal(SIGINT, int_handler);
  
  printf("CUDA Special-Form DETI Coin Miner (Eduardo pattern)\n");
  printf("Searching for coins with 'Eduardo' at positions 12-18\n");
  
  // Check CUDA device
  int device_count;
  CUDA_CHECK(cudaGetDeviceCount(&device_count));
  
  if(device_count == 0) {
    fprintf(stderr, "No CUDA devices found!\n");
    return 1;
  }
  
  cudaDeviceProp prop;
  CUDA_CHECK(cudaGetDeviceProperties(&prop, 0));
  printf("Using GPU: %s\n", prop.name);
  printf("CUDA configuration: %d blocks x %d threads = %d total threads\n", 
          BLOCKS_PER_GRID, THREADS_PER_BLOCK, TOTAL_THREADS);
  
  // Allocate coins storage buffer (atomicAdd pattern)
  u32_t* d_coins_storage;
  u32_t* h_coins_storage = (u32_t*)malloc(COINS_BUFFER_SIZE * sizeof(u32_t));
  CUDA_CHECK(cudaMalloc(&d_coins_storage, COINS_BUFFER_SIZE * sizeof(u32_t)));
  
  unsigned long long total_attempts = 0;
  unsigned total_found = 0;
  unsigned long long cycle = 0;
  
  struct timespec start_time, current_time;
  clock_gettime(CLOCK_MONOTONIC_RAW, &start_time);
  
  // Run for exactly 1 hour (3600 seconds)
  const double target_runtime = 3600.0;
  
  while(keep_running) {
    // Initialize counter to 1 (atomicAdd optimization)
    h_coins_storage[0] = 1;
    CUDA_CHECK(cudaMemcpy(d_coins_storage, h_coins_storage, sizeof(u32_t), 
                          cudaMemcpyHostToDevice));
    
    // Launch kernel
    cuda_mine_special_coins<<<BLOCKS_PER_GRID, THREADS_PER_BLOCK>>>(d_coins_storage, cycle);
    
    CUDA_CHECK(cudaGetLastError());
    CUDA_CHECK(cudaDeviceSynchronize());
    
    // Read counter
    CUDA_CHECK(cudaMemcpy(h_coins_storage, d_coins_storage, sizeof(u32_t), 
                          cudaMemcpyDeviceToHost));
    u32_t next_free_idx = h_coins_storage[0];
    u32_t num_words = (next_free_idx > 1) ? (next_free_idx - 1) : 0;
    
    // Update attempt counter
    total_attempts += (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD;
    cycle++;
    
    if(num_words > 0) {
      // Copy only found coins
      u32_t words_to_copy = (num_words < COINS_BUFFER_SIZE - 1) ? num_words : (COINS_BUFFER_SIZE - 1);
      CUDA_CHECK(cudaMemcpy(&h_coins_storage[1], &d_coins_storage[1], 
                            words_to_copy * sizeof(u32_t), cudaMemcpyDeviceToHost));
      
      // Process coins (16 words each: 11 message + 5 hash)
      u32_t num_coins = words_to_copy / 16;
      
      for(u32_t c = 0; c < num_coins; c++) {
        u32_t offset = 1 + c * 16;
        
        // Reconstruct full message
        union {
          u32_t w[14];
          u08_t c[56];
        } msg;
        
        // Add prefix "DETI coin 2 " (words 0-2)
        const char prefix[] = "DETI coin 2 ";
        for (int i = 0; i < 12; i++) {
          msg.c[i ^ 3] = prefix[i];
        }
        
        // Copy variable message words 3-13 (bytes 12-55 including padding)
        for(int m = 0; m < 11; m++) {
          msg.w[3 + m] = h_coins_storage[offset + m];
        }
        
        // Extract hash
        u32_t hash[5];
        for(int h = 0; h < 5; h++) {
          hash[h] = h_coins_storage[offset + 11 + h];
        }
        
        // Count zeros
        unsigned zeros = 0;
        for(unsigned word = 1u; word <= 4u; ++word) {
          u32_t v = hash[word];
          if(v == 0u) {
            zeros += 32u;
            continue;
          }
          unsigned lz = 0;
          while((v & 0x80000000u) == 0) {
            lz++;
            v <<= 1;
          }
          zeros += lz;
          break;
        }
        
        total_found++;
        
        // Print coin (first 54 bytes, exclude padding)
        printf("\n👻 DETI COIN FOUND!\n");
        printf("Attempts: %llu\n", total_attempts);
        printf("Value: V%02u | ", zeros);
        for (int j = 0; j < 54; j++) {
          printf("%c", msg.c[j ^ 3]);
        }
        printf("\nHash: ");
        for(int j = 0; j < 5; j++) {
          printf("%08x", hash[j]);
        }
        printf("\n\n");
        
        save_coin(msg.w);
      }
    }
    
    // Progress reporting (every kernel run)
    clock_gettime(CLOCK_MONOTONIC_RAW, &current_time);
    double cur_time = (double)(current_time.tv_sec - start_time.tv_sec) + 
                      1e-9 * (double)(current_time.tv_nsec - start_time.tv_nsec);
    
    double hps = (cur_time > 0.0) ? ((double)total_attempts / cur_time) : 0.0;
    
    printf("[STATUS] Attempts=%llu, Hashes_per_second=%.0f, Found=%u\n",
           total_attempts, hps, total_found);
    fflush(stdout);
    
    // Check runtime limit
    if(cur_time >= target_runtime) {
      printf("Target runtime of %.0f seconds reached. Stopping...\n", target_runtime);
      keep_running = 0;
    }
  }
  
  // Final statistics
  clock_gettime(CLOCK_MONOTONIC_RAW, &current_time);
  double total_time = (double)(current_time.tv_sec - start_time.tv_sec) + 
                     1e-9 * (double)(current_time.tv_nsec - start_time.tv_nsec);
  
  printf("Done. Attempts=%llu Time=%.3f s Hashes_per_second=%.0f Found=%u\n", 
         total_attempts, total_time, total_attempts / total_time, total_found);
  
  save_coin(NULL);
  
  // Ensure vault file exists
  {
    FILE *fp = fopen("deti_coins_v2_vault_special.txt","a");
    if(fp != NULL)
      fclose(fp);
  }
  
  free(h_coins_storage);
  CUDA_CHECK(cudaFree(d_coins_storage));
  
  return 0;
}
