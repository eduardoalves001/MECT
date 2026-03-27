/*
 * sha1_cuda_kernel_histogram.cu
 *
 * Measures and displays a histogram of CUDA kernel execution times.
 * 
 * This program:
 * - Runs the CUDA mining kernel multiple times
 * - Records the wall time for each kernel execution
 * - Computes statistics (min, max, average, median, std deviation)
 * - Displays a histogram of execution times
 *
 * Note: Uses simplified pseudo-random generation (LCG instead of cuRAND)
 * for faster benchmarking focused on SHA1 computation performance.
 *
 * Build:
 *   nvcc -O3 -use_fast_math -arch=sm_60 sha1_cuda_kernel_histogram.cu -o sha1_cuda_kernel_histogram
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <cuda_runtime.h>

#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1.h"

// CUDA configuration - MUST MATCH sha1_cuda_search.cu
#define THREADS_PER_BLOCK 256
#define BLOCKS_PER_GRID 4096
#define TOTAL_THREADS (THREADS_PER_BLOCK * BLOCKS_PER_GRID)
#define ATTEMPTS_PER_THREAD 10000   // Same as production configuration

// Histogram configuration
#define NUM_SAMPLES 100           // Number of kernel runs to measure
#define HISTOGRAM_BINS 30         // Number of bins for display
#define COINS_BUFFER_SIZE 65536   // Buffer size for atomicAdd pattern (in u32_t words)

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

// CUDA kernel for mining DETI coins (simplified for benchmarking - no cuRAND)
__global__ void cuda_mine_deti_coins(
  u32_t *coins_storage_area,
  unsigned long long seed_offset
) {
  int tid = blockIdx.x * blockDim.x + threadIdx.x;
  
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
  
  // Use simple pseudo-random generator (much faster than cuRAND)
  unsigned long long seed = seed_offset * TOTAL_THREADS + tid;
  
  // Each thread performs multiple attempts
  for (int attempt = 0; attempt < ATTEMPTS_PER_THREAD; attempt++) {
    // Generate pseudo-random bytes 12-53 using simple LCG
    seed = seed * 1103515245ULL + 12345ULL;
    
    for (int i = 12; i < 54; i++) {
      seed = seed * 1103515245ULL + 12345ULL;
      unsigned char val = 0x20 + ((seed >> 16) % 95);
      if (val == '\n') val = ' ';
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
        // Check if buffer has space before reserving
        u32_t current = coins_storage_area[0];
        if(current + 14u <= COINS_BUFFER_SIZE - 1u) {
          // Reserve space atomically (14 words per coin)
          u32_t idx = atomicAdd(coins_storage_area, 14u);
          
          // Double-check we got valid space
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

// Comparison function for qsort
int compare_double(const void *a, const void *b) {
  double diff = (*(double*)a - *(double*)b);
  return (diff > 0) - (diff < 0);
}

// Print histogram
void print_histogram(double *times, int n_samples, int n_bins) {
  // Find min and max
  double min_time = times[0];
  double max_time = times[0];
  for (int i = 1; i < n_samples; i++) {
    if (times[i] < min_time) min_time = times[i];
    if (times[i] > max_time) max_time = times[i];
  }
  
  // Create bins
  int *bins = (int*)calloc(n_bins, sizeof(int));
  double bin_width = (max_time - min_time) / n_bins;
  
  // Fill bins
  for (int i = 0; i < n_samples; i++) {
    int bin = (int)((times[i] - min_time) / bin_width);
    if (bin >= n_bins) bin = n_bins - 1;
    bins[bin]++;
  }
  
  // Find max count for scaling
  int max_count = 0;
  for (int i = 0; i < n_bins; i++) {
    if (bins[i] > max_count) max_count = bins[i];
  }
  
  // Print histogram
  printf("\n========================================\n");
  printf("HISTOGRAM OF KERNEL EXECUTION TIMES\n");
  printf("========================================\n\n");
  
  const int bar_width = 60;
  for (int i = 0; i < n_bins; i++) {
    double bin_start = min_time + i * bin_width;
    double bin_end = bin_start + bin_width;
    
    printf("%7.3f - %7.3f ms [%4d] ", bin_start * 1000, bin_end * 1000, bins[i]);
    
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
void compute_statistics(double *times, int n_samples) {
  // Sort for median calculation
  double *sorted_times = (double*)malloc(n_samples * sizeof(double));
  memcpy(sorted_times, times, n_samples * sizeof(double));
  qsort(sorted_times, n_samples, sizeof(double), compare_double);
  
  // Calculate statistics
  double min_time = sorted_times[0];
  double max_time = sorted_times[n_samples - 1];
  double median_time = sorted_times[n_samples / 2];
  
  double sum = 0.0;
  for (int i = 0; i < n_samples; i++) {
    sum += times[i];
  }
  double mean_time = sum / n_samples;
  
  double variance = 0.0;
  for (int i = 0; i < n_samples; i++) {
    double diff = times[i] - mean_time;
    variance += diff * diff;
  }
  double std_dev = sqrt(variance / n_samples);
  
  // Print statistics
  printf("========================================\n");
  printf("KERNEL EXECUTION TIME STATISTICS\n");
  printf("========================================\n");
  printf("Samples:           %d\n", n_samples);
  printf("Min time:          %.3f ms\n", min_time * 1000);
  printf("Max time:          %.3f ms\n", max_time * 1000);
  printf("Mean time:         %.3f ms\n", mean_time * 1000);
  printf("Median time:       %.3f ms\n", median_time * 1000);
  printf("Std deviation:     %.3f ms\n", std_dev * 1000);
  printf("========================================\n");
  
  free(sorted_times);
}

int main(void) {
  printf("========================================\n");
  printf("CUDA Kernel Execution Time Histogram\n");
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
  CUDA_CHECK(cudaMalloc(&d_coins_storage_area, COINS_BUFFER_SIZE * sizeof(u32_t)));
  
  // Array to store execution times
  double *execution_times = (double*)malloc(NUM_SAMPLES * sizeof(double));
  
  printf("Running %d kernel executions to collect timing data...\n", NUM_SAMPLES);
  printf("(Each kernel: %d threads × %d attempts = %llu total hashes)\n",
         TOTAL_THREADS, ATTEMPTS_PER_THREAD,
         (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD);
  printf("Progress: ");
  fflush(stdout);
  
  struct timespec start, end;
  
  // Warm-up run (exclude from measurements)
  CUDA_CHECK(cudaMemset(d_coins_storage_area, 0, sizeof(u32_t)));
  cuda_mine_deti_coins<<<BLOCKS_PER_GRID, THREADS_PER_BLOCK>>>(d_coins_storage_area, 0);
  CUDA_CHECK(cudaDeviceSynchronize());
  
  // Collect timing samples
  for (int i = 0; i < NUM_SAMPLES; i++) {
    // Clear counter
    CUDA_CHECK(cudaMemset(d_coins_storage_area, 0, sizeof(u32_t)));
    
    // Time the kernel execution
    clock_gettime(CLOCK_MONOTONIC_RAW, &start);
    
    cuda_mine_deti_coins<<<BLOCKS_PER_GRID, THREADS_PER_BLOCK>>>(d_coins_storage_area, i);
    CUDA_CHECK(cudaDeviceSynchronize());
    
    clock_gettime(CLOCK_MONOTONIC_RAW, &end);
    
    // Calculate elapsed time in seconds
    double elapsed = (double)(end.tv_sec - start.tv_sec) + 
                     1e-9 * (double)(end.tv_nsec - start.tv_nsec);
    execution_times[i] = elapsed;
    
    // Progress indicator (show progress every 5 samples)
    if ((i + 1) % 5 == 0) {
      printf(".");
      fflush(stdout);
    }
  }
  
  printf(" Done!\n\n");
  
  // Compute and display statistics
  compute_statistics(execution_times, NUM_SAMPLES);
  
  // Display histogram
  print_histogram(execution_times, NUM_SAMPLES, HISTOGRAM_BINS);
  
  // Calculate throughput
  unsigned long long total_hashes = (unsigned long long)TOTAL_THREADS * ATTEMPTS_PER_THREAD;
  double sum = 0.0;
  for (int i = 0; i < NUM_SAMPLES; i++) {
    sum += execution_times[i];
  }
  double avg_time = sum / NUM_SAMPLES;
  double avg_throughput = total_hashes / avg_time;
  
  printf("Average throughput: %.2f GH/s (giga-hashes per second)\n\n", 
         avg_throughput / 1e9);
  
  // Cleanup
  free(execution_times);
  cudaFree(d_coins_storage_area);
  
  return 0;
}
