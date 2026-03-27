/*
 * sha1_cpu_search.c
 *
 * Single-threaded CPU search for DETI coins (reference, non-SIMD version).
 * Uses the provided sha1() implementation and save_coin() vault.
 *
 * Behavior:
 * - Repeatedly generates candidate 55-byte messages following the provided
 *   template: bytes 0..11 are fixed to "DETI coin 2 "; bytes 12..53 are
 *   randomized (avoid '\n'); byte 54 is '\n', byte 55 is 0x80 padding for SHA1.
 * - Computes SHA1 via sha1(), checks signature (hash[0] == 0xAAD20250u).
 * - If signature matches, counts leading zero bits in hash[1..4] (value),
 *   stores coin via save_coin() and prints summary.
 * - Runs indefinitely until interrupted with Ctrl+C.
 *
 * Build: see project Makefile; compile with:
 *   cc -march=native -Wall -Wshadow -Werror -O3 sha1_cpu_search.c aad_sha1_cpu.h aad_vault.h aad_utilities.h aad_data_types.h -o sha1_cpu_search
 */ 

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <signal.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"

static volatile int keep_running = 1;


/* Helps me when I need to stop the program with Ctrl+C */
static void int_handler(int signum){
  (void)signum;
  keep_running = 0;
}


//"Let n be the number of zeros bits after the mandatory aad20250 signature. That is the coin's value.
// Given that the probability of finding a 2025 DETIcoin of value v using a file with random content is 1 in 2^(32+v),
// a 2025 DETIcoin with value v is worth 2^v coins of value 0."

// count leading zero bits in hash[1..4], used to determine the value of a DETI coin
// First 32 bytes have to be 0xAAD20250, remaining 128 bits (hash[1] to hash[4] count consecutive leading zeros).
// u32_t hash[5] -> hash[0] = Contains 0xAAD20250, hash[1] to hash[4] = The remaining 128 bits where we count zeros.

static unsigned count_leading_zero_bits(u32_t hash[5]){
  unsigned n = 0u;
  // Loops through 1-4, skips hash[0] because the first 32 bytes have to be 0xAAD20250.
  // Processes each 32-bit word: hash[1], hash[2], hash[3], hash[4]
  for(unsigned word = 1u; word <= 4u; ++word)
  {
    // Checks if the word is 0.
    // If the entire 32 bit word is 0, add 32 to our zero count aand move to the next word.
    u32_t v = hash[word];
    if(v == 0u){
      n += 32u;
      continue;
    }
    // Count individual Bits in Non-Zero words.
    // Goes from most to least significant bit (32 to 0).
    // Right Shifts the word by b positions.
    // Isolates the bit we are currently testing.
    // If bit is 0 increment counter and continue
    // If bit is 1 stop immediately and return the count
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
  /* Uses the above function to stop the Program with Control+C */
  signal(SIGINT,int_handler);
  // prepare template: 56 bytes (55 message bytes + 0x80)
  u08_t msg_bytes[56];
  memset(msg_bytes,0,56);
  // mandatory prefix "DETI coin 2 " (12 bytes)
  const char *prefix = "DETI coin 2 ";
  memcpy(&msg_bytes[0],prefix,12);
  // byte 54 must be '\n'
  msg_bytes[54] = (u08_t)'\n';
  // byte 55 is SHA1 padding 0x80
  msg_bytes[55] = 0x80u;

  // Counter for deterministic generation (much faster than random)
  union { u08_t c[8]; struct { u32_t u, v; } iv; } counter;
  
  // Initialize counter with a fixed start point (deterministic approach)
  counter.iv.u = 0x20202020u; 
  counter.iv.v = 0x20202020u;

  u32_t hash[5];
  // copy fixed bytes into data[] in the endian/byte-swap used by other code
  union { u08_t c[14 * 4]; u32_t i[14]; } udata;
  
  // Ensure all counter bytes start in valid ASCII range
  for(int i = 0; i < 8; i++) {
    counter.c[i] = 0x20 + (counter.c[i] % (0x7E - 0x20 + 1));
  }

  unsigned long long attempts = 0ull;
  unsigned found = 0u;

  struct timespec start_wall, cur_wall;
  clock_gettime(CLOCK_MONOTONIC_RAW,&start_wall);

  while(keep_running){
    // Use counter for positions 12..19 (8 bytes), fill rest with fixed pattern
    for(unsigned i = 12u; i <= 53u; ++i){
      if(i < 20u) {
        msg_bytes[i] = counter.c[i - 12u];  // Use counter bytes 0-7
      } else {
        // Fill remaining bytes with a predictable pattern to avoid randomness
        msg_bytes[i] = (u08_t)(0x20 + ((i * 17 + attempts) % (0x7E - 0x20 + 1)));
      }
    }
    
    // Increment counter (optimization).
    for(int i = 0; i < 8; i++){
      if(++counter.c[i] <= 0x7E)
        break;
      counter.c[i] = 0x20;
    }

    // prepare udata.c with byte-swapped ordering used elsewhere (index ^ 3)
    for(unsigned i = 0u; i < 56u; ++i)
      udata.c[i ^ 3] = msg_bytes[i];
    // set remaining bytes to zero (w[14]=0 is expected)
    for(unsigned i = 56u; i < 14u * 4u; ++i)
      udata.c[i ^ 3] = 0;

    // compute sha1
    sha1(&udata.i[0],&hash[0]);
    ++attempts;

    // When a DETI coin is found  
    if(hash[0] == 0xAAD20250u){
      // Counter guarantees no '\n' chars
      unsigned zeros = count_leading_zero_bits(hash);
      
      printf("\n👻 DETI COIN FOUND!\n");
      printf("Attempts: %llu\n", attempts);
      printf("Value: V%02u\n", zeros);
      printf("Hash: ");
      for(int i = 0; i < 5; i++)
        printf("%08x", hash[i]);
      printf("\n\n");
      
      save_coin(&udata.i[0]);
      ++found;
    }

    /* Check progress for each 50000000 attempts */
    if ((attempts % 50000000ull) == 0ull){
      clock_gettime(CLOCK_MONOTONIC_RAW,&cur_wall);
      double cur_time = (double)(cur_wall.tv_sec - start_wall.tv_sec) + 1e-9 * (double)(cur_wall.tv_nsec - start_wall.tv_nsec);
      double hps = (cur_time > 0.0) ? ((double)attempts / cur_time) : 0.0;
      printf("[STATUS] Attempts=%llu, Hashes_per_second=%.0f, Found=%u\n", attempts, hps, found);
      fflush(stdout);
    }
  }

  // When we exit the loop, print final stats about the run
  clock_gettime(CLOCK_MONOTONIC_RAW,&cur_wall);
  double total_time = (double)(cur_wall.tv_sec - start_wall.tv_sec) + 1e-9 * (double)(cur_wall.tv_nsec - start_wall.tv_nsec);
  double hps = (double)attempts / total_time;
  printf("Done. Attempts=%llu Time=%.3f s Hashes_per_second=%.0f Found=%u\n", attempts, total_time, hps, found);

  save_coin(NULL);

  // Ensure vault file exists even if no coins were found
  {
    FILE *fp = fopen("deti_coins_v2_vault.txt","a");
    if(fp != NULL)
      fclose(fp);
  }

  return 0;
}
