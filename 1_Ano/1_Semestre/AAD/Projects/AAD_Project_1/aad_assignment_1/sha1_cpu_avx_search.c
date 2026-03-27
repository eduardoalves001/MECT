/*
 * sha1_cpu_avx_search.c
 *
 * AVX-based CPU search for DETI coins (4 lanes).
 * Uses sha1_avx() and save_coin() vault.
 *
 * Prints progress every 50 million attempts and coin details when found.
 *
 * Build: cc -march=native -mavx -O3 -Wall -Wshadow -Werror sha1_cpu_avx_search.c aad_sha1_cpu.h aad_vault.h aad_utilities.h aad_data_types.h -o sha1_cpu_avx_search
 */ 

 #define _GNU_SOURCE
 #include <stdio.h>
 #include <stdlib.h>
 #include <string.h>
 #include <time.h>
 #include <signal.h>
 #include "aad_data_types.h"
 #include "aad_utilities.h"
 #include "aad_sha1_cpu.h"
 #include "aad_vault.h"
 
 static volatile int keep_running = 1;
 
 static void int_handler(int signum){
     (void)signum;
     keep_running = 0;
 }
 
 #define N_LANES 4
 
 static unsigned count_leading_zero_bits(u32_t hash[5]){
     unsigned n = 0u;
     for(unsigned word = 1u; word <= 4u; ++word){
         u32_t v = hash[word];
         if(v == 0u){
             n += 32u;
             continue;
         }
         for(int b = 31; b >= 0; --b){
             if(((v >> b) & 1u) == 0u)
                 ++n;
             else
                 return n;
         }
     }
     return n;
 }
 
 int main(void){
     signal(SIGINT,int_handler);
 
     // Prepare fixed template
     u08_t msg_bytes[56];
     (void)msg_bytes; // kept for reference / future use
     const char *prefix = "DETI coin 2 ";
 
     // Data structures for AVX lanes
     union { u08_t c[14*4]; u32_t i[14]; } data[N_LANES];
     u32_t interleaved_data[14][N_LANES] __attribute__((aligned(16)));
     u32_t interleaved_hash[5][N_LANES] __attribute__((aligned(16)));
 
     // Counter for deterministic generation
     union { u08_t c[8]; struct { u32_t u,v; } iv; } counter;
     counter.iv.u = 0x20202020u;
     counter.iv.v = 0x20202020u;
     for(int i=0;i<8;i++) counter.c[i] = 0x20 + (counter.c[i] % (0x7E - 0x20 + 1));
 
     unsigned long long attempts = 0ull;
     unsigned found = 0u;
     struct timespec start_wall, cur_wall;
     clock_gettime(CLOCK_MONOTONIC_RAW,&start_wall);
 
     while(keep_running){
         // Generate N_LANES candidate messages
         for(int lane=0; lane<N_LANES; ++lane){
             memset(data[lane].c,0,14*4);
             /* place prefix taking into account byte order used elsewhere (j ^ 3) */
             for(int j = 0; j < 12; ++j)
                 data[lane].c[j ^ 3] = (u08_t)prefix[j];
             data[lane].c[54 ^ 3] = (u08_t)'\n';
             data[lane].c[55 ^ 3] = 0x80u;
             // Fill bytes 12..53
             for(unsigned i=12;i<=53;i++){
                 if(i<20) data[lane].c[i^3] = counter.c[i-12];
                 else data[lane].c[i^3] = (u08_t)(0x20 + ((i*17 + (unsigned)(attempts + (unsigned long long)lane)) % (0x7E - 0x20 +1)));
             }
         }
 
         // Increment counter (ascii printable range 0x20..0x7E)
         for(int i=0;i<8;i++){
             if(++counter.c[i] <= 0x7E) break;
             counter.c[i] = 0x20;
         }
 
         // Interleave data for AVX
         for(int lane=0; lane<N_LANES; ++lane)
             for(int i=0;i<14;i++)
                 interleaved_data[i][lane] = data[lane].i[i];
 
         // Compute 4 hashes in one AVX call
         sha1_avx((v4si *)&interleaved_data[0], (v4si *)&interleaved_hash[0]);
         attempts += N_LANES;
 
         // Check each lane for DETI coins
         for(int lane=0; lane<N_LANES; ++lane){
             if(interleaved_hash[0][lane] == 0xAAD20250u){
                 // reconstruct contiguous per-lane hash[5]
                 u32_t hash_lane[5];
                 for(int w = 0; w < 5; ++w)
                     hash_lane[w] = interleaved_hash[w][lane];
 
                 unsigned zeros = count_leading_zero_bits(hash_lane);
 
                 printf("\n👻 DETI COIN FOUND in lane %d!\n", lane);
                 printf("Attempts: %llu\n", attempts);
                 printf("Value: V%02u\n", zeros);
                 printf("Hash: ");
                 for(int i=0;i<5;i++) printf("%08x", hash_lane[i]);
                 printf("\n\n");
 
                 /* save_coin expects pointer to 14 u32_t words in the same layout as data[lane].i */
                 save_coin(&data[lane].i[0]);
                 found++;
             }
         }
 
         // Print progress every 50M attempts
         if((attempts % 50000000ull) == 0ull){
             clock_gettime(CLOCK_MONOTONIC_RAW,&cur_wall);
             double cur_time = (double)(cur_wall.tv_sec - start_wall.tv_sec)
                             + 1e-9*(double)(cur_wall.tv_nsec - start_wall.tv_nsec);
             double hps = (cur_time>0.0) ? ((double)attempts / cur_time) : 0.0;
             printf("[STATUS] Attempts=%llu, Hashes_per_second=%.0f, Found=%u\n", attempts,hps,found);
             fflush(stdout);
         }
     }
 
     clock_gettime(CLOCK_MONOTONIC_RAW,&cur_wall);
     double total_time = (double)(cur_wall.tv_sec - start_wall.tv_sec)
                       + 1e-9*(double)(cur_wall.tv_nsec - start_wall.tv_nsec);
     double hps = (double)attempts / total_time;
     printf("Done. Attempts=%llu Time=%.3f s Hashes_per_second=%.0f Found=%u\n",
            attempts,total_time,hps,found);
 
     save_coin(NULL);
 
     // Ensure vault file exists
     {
       FILE *fp = fopen("deti_coins_v2_vault.txt","a");
       if(fp!=NULL) fclose(fp);
     }
 
     return 0;
 }
 