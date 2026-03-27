/*
 * sha1_cpu_openmp_avx2_search.c
 *
 * OpenMP + AVX2 parallel CPU search for DETI coins.
 * Uses multiple CPU threads, each processing 8 SHA1 lanes with AVX2 SIMD.
 *
 * Features:
 * - OpenMP multi-threading (uses all CPU cores)
 * - AVX2 SIMD (8 parallel SHA1 computations per thread)
 * - Thread-safe vault saving
 * - Per-thread random seeds for independent search spaces
 * - Aggregated statistics across all threads
 *
 * Build: cc -fopenmp -march=native -mavx2 -O3 -Wall -Wshadow -Werror sha1_cpu_openmp_avx2_search.c -o sha1_cpu_openmp_avx2_search
 */ 

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <signal.h>
#include <omp.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"

static volatile int keep_running = 1;

static void int_handler(int signum){
    (void)signum;
    keep_running = 0;
}

#define N_LANES 8

static unsigned count_leading_zero_bits(u32_t hash[5]){
    unsigned n = 0u;
    for(unsigned word = 1u; word <= 4u; ++word){
        u32_t v = hash[word];
        if(v == 0u){
            n += 32u;
            continue;
        }
        // Use __builtin_clz for better performance
        n += __builtin_clz(v);
        return n;
    }
    return n;
}

int main(void){
    signal(SIGINT, int_handler);

    const char *prefix = "DETI coin 2 ";
    
    // Get number of available threads
    int num_threads = omp_get_max_threads();
    printf("========================================\n");
    printf("OpenMP + AVX2 DETI Coin Miner\n");
    printf("========================================\n");
    printf("CPU Threads: %d\n", num_threads);
    printf("SIMD Lanes per thread: %d (AVX2)\n", N_LANES);
    printf("Total parallel hashes: %d\n", num_threads * N_LANES);
    printf("========================================\n\n");

    // Global counters (use reduction for better performance)
    unsigned long long total_attempts = 0ull;
    unsigned total_found = 0u;
    unsigned long long last_reported = 0ull;
    
    struct timespec start_wall;
    clock_gettime(CLOCK_MONOTONIC_RAW, &start_wall);

    // OpenMP parallel region
    #pragma omp parallel
    {
        int thread_id = omp_get_thread_num();
        
        // Per-thread data structures
        union { u08_t c[14*4]; u32_t i[14]; } data[N_LANES];
        u32_t interleaved_data[14][N_LANES] __attribute__((aligned(32)));
        u32_t interleaved_hash[5][N_LANES] __attribute__((aligned(32)));

        // Initialize random seeds (unique per thread)
        unsigned int seeds[N_LANES];
        for(int lane = 0; lane < N_LANES; ++lane){
            seeds[lane] = (unsigned int)time(NULL) + thread_id * 1000000 + lane * 12345;
        }

        // Pre-initialize fixed parts of messages (only once per thread)
        for(int lane = 0; lane < N_LANES; ++lane){
            memset(data[lane].c, 0, 14*4);
            // Fixed prefix
            for(int j = 0; j < 12; ++j)
                data[lane].c[j ^ 3] = (u08_t)prefix[j];
            // Fixed suffix
            data[lane].c[54 ^ 3] = (u08_t)'\n';
            data[lane].c[55 ^ 3] = 0x80u;
        }

        // Per-thread counters
        unsigned long long thread_attempts = 0ull;
        unsigned thread_found = 0u;

        while(keep_running){
            // Generate random bytes 12-53 for each lane
            for(int lane = 0; lane < N_LANES; ++lane){
                for(unsigned i = 12; i <= 53; ++i){
                    unsigned char val;
                    do {
                        val = 0x20 + (rand_r(&seeds[lane]) % (0x7E - 0x20 + 1));
                    } while(val == '\n');
                    data[lane].c[i ^ 3] = val;
                }
            }

            // Interleave data for AVX2
            for(int lane = 0; lane < N_LANES; ++lane)
                for(int i = 0; i < 14; ++i)
                    interleaved_data[i][lane] = data[lane].i[i];

            // Compute 8 hashes in one AVX2 call
            sha1_avx2((v8si *)&interleaved_data[0], (v8si *)&interleaved_hash[0]);
            thread_attempts += N_LANES;

            // Check each lane for DETI coins
            for(int lane = 0; lane < N_LANES; ++lane){
                if(interleaved_hash[0][lane] == 0xAAD20250u){
                    u32_t hash_lane[5];
                    for(int w = 0; w < 5; ++w)
                        hash_lane[w] = interleaved_hash[w][lane];

                    unsigned zeros = count_leading_zero_bits(hash_lane);

                    if(zeros > 0){
                        // Critical section for output and vault saving
                        #pragma omp critical
                        {
                            printf("\n👻 DETI COIN FOUND by thread %d, lane %d!\n", thread_id, lane);
                            printf("Thread attempts: %llu\n", thread_attempts);
                            printf("Value: V%02u\n", zeros);
                            printf("Hash: ");
                            for(int i = 0; i < 5; ++i) printf("%08x", hash_lane[i]);
                            printf("\n\n");
                            
                            save_coin(&data[lane].i[0]);
                            thread_found++;
                        }
                    }
                }
            }

            // Print progress periodically (thread-safe)
            if((thread_attempts % 10000000ull) == 0ull){
                #pragma omp critical
                {
                    unsigned long long current_total = 0ull;
                    #pragma omp atomic read
                    current_total = total_attempts;
                    
                    if(current_total - last_reported >= 50000000ull){
                        struct timespec cur_wall;
                        clock_gettime(CLOCK_MONOTONIC_RAW, &cur_wall);
                        double cur_time = (double)(cur_wall.tv_sec - start_wall.tv_sec)
                                        + 1e-9*(double)(cur_wall.tv_nsec - start_wall.tv_nsec);
                        
                        double hps = (cur_time > 0.0) ? ((double)current_total / cur_time) : 0.0;
                        printf("[STATUS] Attempts=%llu, Hashes_per_second=%.0f (%.2f MH/s), Found=%u\n", 
                               current_total, hps, hps / 1e6, total_found);
                        fflush(stdout);
                        last_reported = current_total;
                    }
                }
            }
            
            // Update global counter atomically (lightweight)
            #pragma omp atomic
            total_attempts += N_LANES;
        }
    }

    struct timespec end_wall;
    clock_gettime(CLOCK_MONOTONIC_RAW, &end_wall);
    double total_time = (double)(end_wall.tv_sec - start_wall.tv_sec)
                      + 1e-9*(double)(end_wall.tv_nsec - start_wall.tv_nsec);
    double hps = (double)total_attempts / total_time;
    
    printf("\n========================================\n");
    printf("Final Statistics\n");
    printf("========================================\n");
    printf("Total attempts: %llu\n", total_attempts);
    printf("Time: %.3f seconds\n", total_time);
    printf("Throughput: %.0f H/s (%.2f MH/s)\n", hps, hps / 1e6);
    printf("Coins found: %u\n", total_found);
    printf("========================================\n");

    save_coin(NULL);

    // Ensure vault file exists
    FILE *fp = fopen("deti_coins_v2_vault.txt", "a");
    if(fp != NULL) fclose(fp);

    return 0;
}
