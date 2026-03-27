/*
 * sha1_cuda_search.cu
 *
 * CUDA-accelerated search for DETI coins using GPU parallel processing.
 * Uses the provided SHA1 implementation adapted for CUDA and save_coin() vault.
 *
 * Behavior:
 * - Launches thousands of CUDA threads in parallel, each generating candidate
 *   55-byte messages following the provided template: bytes 0..11 are fixed 
 *   to "DETI coin 2 "; bytes 12..53 are randomized (avoid '\n'); 
 *   byte 54 is '\n', byte 55 is 0x80 padding for SHA1.
 * - Each thread computes SHA1, checks signature (hash[0] == 0xAAD20250u).
 * - If signature matches, counts leading zero bits in hash[1..4] (value),
 *   stores results for host processing.
 * - Runs indefinitely until interrupted with Ctrl+C.
 *
 * Build: 
 *   nvcc -O3 -arch=sm_50 sha1_cuda_search.cu -o sha1_cuda_search
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
 
 // Minimal vault implementation for CUDA (simplified)
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
     vault_file = fopen("deti_coins_v2_vault.txt", "a");
     if (vault_file == NULL) {
       fprintf(stderr, "Warning: Could not open vault file\n");
       return;
     }
   }
 
   for (int i = 0; i < 14; i++) {
     fprintf(vault_file, "%08x", coin[i]);
   }
   fprintf(vault_file, "\n");
   fflush(vault_file);
 }
 
 // CUDA configuration - PRODUCTION SETTINGS (optimized for GTX 1650 Ti)
 #define THREADS_PER_BLOCK 256
 #define BLOCKS_PER_GRID 4096
 #define TOTAL_THREADS (THREADS_PER_BLOCK * BLOCKS_PER_GRID)
 #define ATTEMPTS_PER_THREAD 10000
 #define WARPS_PER_GRID (TOTAL_THREADS / 32)

 // Buffer configuration for atomicAdd pattern
 #define COINS_BUFFER_SIZE 1024  // u32_t words: [0]=counter, [1+]=coins (14 words each)
 
 extern "C" __global__ void sha1_cuda_kernel(u32_t *interleaved32_data, u32_t *interleaved32_hash);
 
 static volatile int keep_running = 1;
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
 
 // CUDA kernel for DETI coin mining - atomicAdd OPTIMIZED VERSION
 // coins_storage_area[0] = next free index (atomicAdd counter)
 // coins_storage_area[1+] = coins (14 words each: 9 message words + 5 hash words)
 // Skips constant prefix words 0-2 to save bandwidth!
 __global__ void cuda_mine_deti_coins(u32_t* coins_storage_area, unsigned long long base_nonce) {
   int tid = blockIdx.x * blockDim.x + threadIdx.x;
 
   if (tid >= TOTAL_THREADS) return;
 
   curandState state;
   curand_init(base_nonce + tid, tid, 0, &state);
 
   union { u08_t c[14 * 4]; u32_t i[14]; } udata;
   u32_t fixed_words[14];
   memset(fixed_words, 0, sizeof(fixed_words));
 
   const char prefix[] = "DETI coin 2 ";
   for(int i = 0; i < 12; i++) {
       ((u08_t*)fixed_words)[i ^ 3] = prefix[i];
   }
   ((u08_t*)fixed_words)[54 ^ 3] = '\n';
   ((u08_t*)fixed_words)[55 ^ 3] = 0x80;
 
   for(int attempt = 0; attempt < ATTEMPTS_PER_THREAD; attempt++) {
     // Copy fixed prefix into udata
     for(int w = 0; w < 14; w++) udata.i[w] = fixed_words[w];
 
     // Fill random bytes 12..53
     for(int i = 12; i < 54; i++) {
       u08_t rand_byte;
       do {
         rand_byte = curand(&state) % 95 + 0x20;
       } while(rand_byte == 0x0A);
       udata.c[i ^ 3] = rand_byte;
     }
 
     // Compute SHA1
     u32_t hash[5];
  #define T            u32_t
  #define C(c)         (c)
  #define ROTATE(x,n)  (((x) << (n)) | ((x) >> (32 - (n))))
  #define DATA(idx)    udata.i[idx]
  #define HASH(idx)    hash[idx]
  
      CUSTOM_SHA1_CODE();
  
  #undef T
  #undef C
  #undef ROTATE
  #undef DATA
  #undef HASH
  
      if(hash[0] == 0xAAD20250u) {
        unsigned zeros = count_leading_zero_bits_device(hash);
        if(zeros > 0) {
          // Reserve space atomically (14 words per coin) - PROFESSOR'S PATTERN!
          u32_t idx = atomicAdd(coins_storage_area, 14u);
          
          if(idx < COINS_BUFFER_SIZE - 14u) {
            // Store variable message words (skip constant prefix words 0-2)
            for(int m = 3; m < 12; m++) {
              coins_storage_area[idx + (m - 3)] = udata.i[m];
            }
            // Store hash (5 words)
            for(int h = 0; h < 5; h++) {
              coins_storage_area[idx + 9 + h] = hash[h];
            }
          }
        }
      }
    }
  }
  
int main(void) {
  signal(SIGINT, int_handler);

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
  unsigned long long nonce = 0;

  struct timespec start_time, current_time;
  clock_gettime(CLOCK_MONOTONIC_RAW, &start_time);

  while(keep_running) {
    // Initialize counter to 1 (atomicAdd optimization)
    h_coins_storage[0] = 1;
    CUDA_CHECK(cudaMemcpy(d_coins_storage, h_coins_storage, sizeof(u32_t), 
                          cudaMemcpyHostToDevice));
    
    // Launch kernel
    cuda_mine_deti_coins<<<BLOCKS_PER_GRID, THREADS_PER_BLOCK>>>(d_coins_storage, nonce);
    CUDA_CHECK(cudaGetLastError());
    CUDA_CHECK(cudaDeviceSynchronize());
    
    // Read counter
    CUDA_CHECK(cudaMemcpy(h_coins_storage, d_coins_storage, sizeof(u32_t), 
                          cudaMemcpyDeviceToHost));
    u32_t next_free_idx = h_coins_storage[0];
    u32_t num_words = (next_free_idx > 1) ? (next_free_idx - 1) : 0;
    
    if(num_words > 0) {
      // Copy only found coins
      u32_t words_to_copy = (num_words < COINS_BUFFER_SIZE - 1) ? num_words : (COINS_BUFFER_SIZE - 1);
      CUDA_CHECK(cudaMemcpy(&h_coins_storage[1], &d_coins_storage[1], 
                            words_to_copy * sizeof(u32_t), cudaMemcpyDeviceToHost));
      
      // Process coins (14 words each: 9 message + 5 hash)
      u32_t num_coins = words_to_copy / 14;
      
      for(u32_t c = 0; c < num_coins; c++) {
        u32_t offset = 1 + c * 14;
        
        // Reconstruct full message
        u32_t full_message[14] = {0};
        const char prefix[] = "DETI coin 2 ";
        for(int i = 0; i < 12; i++) {
          ((u08_t*)full_message)[i ^ 3] = prefix[i];
        }
        
        // Copy variable message words (positions 3-11)
        for(int m = 0; m < 9; m++) {
          full_message[3 + m] = h_coins_storage[offset + m];
        }
        
        // Add suffix
        ((u08_t*)full_message)[54 ^ 3] = '\n';
        ((u08_t*)full_message)[55 ^ 3] = 0x80;
        
        // Extract hash
        u32_t hash[5];
        for(int h = 0; h < 5; h++) {
          hash[h] = h_coins_storage[offset + 9 + h];
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
        printf("\n👻 DETI COIN FOUND!\n");
        printf("Attempts: %llu\n", total_attempts);
        printf("Value: V%02u\n", zeros);
        printf("Hash: ");
        for(int j = 0; j < 5; j++) printf("%08x", hash[j]);
        printf("\n\n");
        save_coin(full_message);
      }
    }

    total_attempts += (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD;
    nonce += TOTAL_THREADS;

    // Progress reporting (every kernel run for better visibility)
    clock_gettime(CLOCK_MONOTONIC_RAW, &current_time);
    double cur_time = (double)(current_time.tv_sec - start_time.tv_sec) + 
                      1e-9 * (double)(current_time.tv_nsec - start_time.tv_nsec);
    double hps = (cur_time > 0.0) ? ((double)total_attempts / cur_time) : 0.0;
    printf("[STATUS] Attempts=%llu, Hashes_per_second=%.0f, Found=%u\n",
            total_attempts, hps, total_found);
    fflush(stdout);
  }

  clock_gettime(CLOCK_MONOTONIC_RAW, &current_time);
  double total_time = (double)(current_time.tv_sec - start_time.tv_sec) + 
                      1e-9 * (double)(current_time.tv_nsec - start_time.tv_nsec);

  printf("Done. Attempts=%llu Time=%.3f s Hashes_per_second=%.0f Found=%u\n", 
          total_attempts, total_time, total_attempts / total_time, total_found);

  save_coin(NULL);
  FILE *fp = fopen("deti_coins_v2_vault.txt","a");
  if(fp != NULL) fclose(fp);

  free(h_coins_storage);
  CUDA_CHECK(cudaFree(d_coins_storage));

  return 0;
}  