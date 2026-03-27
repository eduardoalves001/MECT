/*
 * sha1_cuda_coins_histogram.cu
 *
 * Measures and displays a histogram of DETI coins found per kernel execution.
 * 
 * This program:
 * - Runs the CUDA mining kernel multiple times
 * - Records how many valid DETI coins are found in each run
 * - Computes statistics (min, max, average, median, std deviation)
 * - Displays a histogram of coins found per kernel
 *
 * Build:
 *   make sha1_cuda_coins_histogram
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <cuda_runtime.h>
#include <curand_kernel.h>

#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1.h"

// CUDA configuration - Match production configuration for reliable statistics
#define THREADS_PER_BLOCK 256
#define BLOCKS_PER_GRID 4096
#define TOTAL_THREADS (THREADS_PER_BLOCK * BLOCKS_PER_GRID)
#define ATTEMPTS_PER_THREAD 10000  // Higher for better coin statistics

// Histogram configuration
#define NUM_KERNEL_RUNS 200        // Number of kernel runs to collect statistics
#define COINS_BUFFER_SIZE (1024 * 1024)  // 1M words buffer (~75K coins max)

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

// CUDA kernel for mining DETI coins (atomicAdd optimized)
__global__ void cuda_mine_deti_coins(
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
  
  // Fixed prefix "DETI coin 2 "
  const char prefix[] = "DETI coin 2 ";
  for(int i = 0; i < 12; i++) {
    udata.c[i ^ 3] = prefix[i];
  }
  
  // Fixed suffix
  udata.c[54 ^ 3] = '\n';
  udata.c[55 ^ 3] = 0x80;
  
  // Each thread performs multiple attempts
  for (int attempt = 0; attempt < ATTEMPTS_PER_THREAD; attempt++) {
    // Generate random bytes 12-53
    for (int i = 12; i < 54; i++) {
      unsigned char val;
      do {
        val = curand(&state) % 95 + 0x20;
      } while (val == '\n');
      udata.c[i ^ 3] = val;
    }
    
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
    
    // Check if this is a valid DETI coin
    if (hash[0] == 0xAAD20250u) {
      unsigned zeros = count_leading_zero_bits_device(hash);
      if (zeros > 0) {
        // Check if buffer has space before reserving (counter + current coins + new coin)
        u32_t current = coins_storage_area[0];
        if(current + 14u <= COINS_BUFFER_SIZE - 1u) {
          // Reserve space atomically (14 words per coin)
          u32_t idx = atomicAdd(coins_storage_area, 14u);
          
          // Double-check we got valid space (race condition possible)
          if(idx + 14u <= COINS_BUFFER_SIZE - 1u) {
            // Store variable message words (skip constant prefix words 0-2)
            // Data starts at index 1 (index 0 is counter)
            for(int m = 3; m < 12; m++) {
              coins_storage_area[1 + idx + (m - 3)] = udata.w[m];
            }
            // Store hash (5 words)
            for(int h = 0; h < 5; h++) {
              coins_storage_area[1 + idx + 9 + h] = hash[h];
            }
          }
        }
      }
    }
  }
}

// Comparison function for qsort (integers)
int compare_int(const void *a, const void *b) {
  return (*(int*)a - *(int*)b);
}

// Print histogram of coins found
void print_coins_histogram(int *coins_per_run, int n_runs) {
  // Find min and max
  int min_coins = coins_per_run[0];
  int max_coins = coins_per_run[0];
  for (int i = 1; i < n_runs; i++) {
    if (coins_per_run[i] < min_coins) min_coins = coins_per_run[i];
    if (coins_per_run[i] > max_coins) max_coins = coins_per_run[i];
  }
  
  // Determine number of bins (one per unique value in range)
  int range = max_coins - min_coins + 1;
  int *bins = (int*)calloc(range, sizeof(int));
  
  // Fill bins
  for (int i = 0; i < n_runs; i++) {
    int bin = coins_per_run[i] - min_coins;
    bins[bin]++;
  }
  
  // Find max count for scaling
  int max_count = 0;
  for (int i = 0; i < range; i++) {
    if (bins[i] > max_count) max_count = bins[i];
  }
  
  // Print histogram
  printf("\n========================================\n");
  printf("HISTOGRAM OF COINS FOUND PER KERNEL RUN\n");
  printf("========================================\n\n");
  
  const int bar_width = 60;
  for (int i = 0; i < range; i++) {
    int coin_count = min_coins + i;
    
    printf("%4d coins [%3d runs] ", coin_count, bins[i]);
    
    int bar_len = (max_count > 0) ? (bins[i] * bar_width / max_count) : 0;
    for (int j = 0; j < bar_len; j++) {
      printf("█");
    }
    printf("\n");
  }
  
  printf("\n========================================\n\n");
  
  free(bins);
}

// Compute statistics
void compute_statistics(int *coins_per_run, int n_runs) {
  // Sort for median calculation
  int *sorted = (int*)malloc(n_runs * sizeof(int));
  memcpy(sorted, coins_per_run, n_runs * sizeof(int));
  qsort(sorted, n_runs, sizeof(int), compare_int);
  
  // Calculate statistics
  int min_coins = sorted[0];
  int max_coins = sorted[n_runs - 1];
  int median_coins = sorted[n_runs / 2];
  
  double sum = 0.0;
  for (int i = 0; i < n_runs; i++) {
    sum += coins_per_run[i];
  }
  double mean_coins = sum / n_runs;
  
  double variance = 0.0;
  for (int i = 0; i < n_runs; i++) {
    double diff = coins_per_run[i] - mean_coins;
    variance += diff * diff;
  }
  double std_dev = sqrt(variance / n_runs);
  
  // Calculate total coins
  int total_coins = 0;
  for (int i = 0; i < n_runs; i++) {
    total_coins += coins_per_run[i];
  }
  
  // Print statistics
  printf("========================================\n");
  printf("COINS PER KERNEL RUN STATISTICS\n");
  printf("========================================\n");
  printf("Number of runs:    %d\n", n_runs);
  printf("Total coins found: %d\n", total_coins);
  printf("Min coins/run:     %d\n", min_coins);
  printf("Max coins/run:     %d\n", max_coins);
  printf("Mean coins/run:    %.2f\n", mean_coins);
  printf("Median coins/run:  %d\n", median_coins);
  printf("Std deviation:     %.2f\n", std_dev);
  printf("========================================\n");
  
  free(sorted);
}

int main(void) {
  printf("========================================\n");
  printf("CUDA Coins Per Kernel Histogram\n");
  printf("========================================\n\n");
  
  // Check CUDA device
  int device_count;
  CUDA_CHECK(cudaGetDeviceCount(&device_count));
  
  if(device_count == 0) {
    fprintf(stderr, "No CUDA devices found!\n");
    return 1;
  }
  
  cudaDeviceProp prop;
  CUDA_CHECK(cudaGetDeviceProperties(&prop, 0));
  printf("GPU: %s\n", prop.name);
  printf("Configuration: %d blocks × %d threads = %d total threads\n", 
         BLOCKS_PER_GRID, THREADS_PER_BLOCK, TOTAL_THREADS);
  printf("Attempts per thread: %d\n", ATTEMPTS_PER_THREAD);
  printf("Total attempts per kernel: %llu\n\n", 
         (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD);
  
  // Allocate device memory
  u32_t *d_coins_storage_area;
  u32_t *h_coins_storage_area = (u32_t*)malloc(COINS_BUFFER_SIZE * sizeof(u32_t));
  CUDA_CHECK(cudaMalloc(&d_coins_storage_area, COINS_BUFFER_SIZE * sizeof(u32_t)));
  
  // Array to store coins found per run
  int *coins_per_run = (int*)malloc(NUM_KERNEL_RUNS * sizeof(int));
  
  printf("Running %d kernel executions to collect coin statistics...\n", NUM_KERNEL_RUNS);
  printf("(Each kernel: %llu total attempts = %.2f billion hashes)\n",
         (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD,
         ((double)TOTAL_THREADS * ATTEMPTS_PER_THREAD) / 1e9);
  printf("Expected time: ~%.0f seconds\n", NUM_KERNEL_RUNS * 12.5);  // Estimate based on your ~12.5s per kernel
  printf("Progress: ");
  fflush(stdout);
  
  struct timespec start_total, end_total;
  clock_gettime(CLOCK_MONOTONIC_RAW, &start_total);
  
  // Warm-up run
  CUDA_CHECK(cudaMemset(d_coins_storage_area, 0, sizeof(u32_t)));
  cuda_mine_deti_coins<<<BLOCKS_PER_GRID, THREADS_PER_BLOCK>>>(d_coins_storage_area, 0);
  CUDA_CHECK(cudaDeviceSynchronize());
  
  // Collect coin counts for each kernel run
  for (int run = 0; run < NUM_KERNEL_RUNS; run++) {
    // Clear counter
    CUDA_CHECK(cudaMemset(d_coins_storage_area, 0, sizeof(u32_t)));
    
    // Run kernel
    cuda_mine_deti_coins<<<BLOCKS_PER_GRID, THREADS_PER_BLOCK>>>(d_coins_storage_area, run + 1);
    CUDA_CHECK(cudaDeviceSynchronize());
    
    // Copy counter back
    CUDA_CHECK(cudaMemcpy(h_coins_storage_area, d_coins_storage_area, sizeof(u32_t),
                          cudaMemcpyDeviceToHost));
    
    // Count coins found (counter value / 14 since each coin is 14 words)
    int coins_found = h_coins_storage_area[0] / 14;
    coins_per_run[run] = coins_found;
    
    // Progress indicator
    if ((run + 1) % 5 == 0) {
      printf(".");
      fflush(stdout);
    }
  }
  
  clock_gettime(CLOCK_MONOTONIC_RAW, &end_total);
  double total_time = (double)(end_total.tv_sec - start_total.tv_sec) + 
                      1e-9 * (double)(end_total.tv_nsec - start_total.tv_nsec);
  
  printf(" Done!\n\n");
  printf("Total collection time: %.1f seconds\n", total_time);
  printf("Average hash rate: %.2f GH/s\n\n", 
         ((double)TOTAL_THREADS * ATTEMPTS_PER_THREAD * NUM_KERNEL_RUNS) / total_time / 1e9);
  
  // Compute and display statistics
  compute_statistics(coins_per_run, NUM_KERNEL_RUNS);
  
  // Display histogram
  print_coins_histogram(coins_per_run, NUM_KERNEL_RUNS);
  
  // Calculate expected coins per hour
  int total_coins = 0;
  for (int i = 0; i < NUM_KERNEL_RUNS; i++) {
    total_coins += coins_per_run[i];
  }
  double avg_coins_per_run = (double)total_coins / NUM_KERNEL_RUNS;
  double avg_time_per_run = total_time / NUM_KERNEL_RUNS;
  double coins_per_hour = avg_coins_per_run * (3600.0 / avg_time_per_run);
  
  printf("Expected coins per hour: %.0f coins/hour\n", coins_per_hour);
  printf("Average time per kernel: %.2f seconds\n\n", avg_time_per_run);
  
  // Cleanup
  free(coins_per_run);
  free(h_coins_storage_area);
  cudaFree(d_coins_storage_area);
  
  return 0;
}
