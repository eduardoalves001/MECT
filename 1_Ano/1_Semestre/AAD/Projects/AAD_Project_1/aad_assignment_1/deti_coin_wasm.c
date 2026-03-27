/*
 * deti_coin_wasm.c
 *
 * WebAssembly DETI Coin Miner with SIMD Optimization
 * 
 * This C code compiles to WebAssembly and runs in the browser.
 * Uses WASM SIMD instructions for 4x parallelism.
 * Uses Emscripten to compile C to WASM.
 *
 * Build:
 *   emcc -O3 deti_coin_wasm.c -o deti_coin_wasm.js \
 *     -msimd128 \
 *     -s EXPORTED_FUNCTIONS='["_mine_deti_coins","_get_found_count","_get_coin","_reset_coins","_malloc","_free"]' \
 *     -s EXPORTED_RUNTIME_METHODS='["ccall","cwrap","UTF8ToString"]' \
 *     -s ALLOW_MEMORY_GROWTH=1 \
 *     -s MODULARIZE=1 \
 *     -s EXPORT_NAME='DETICoinMiner'
 *
 * Usage from JavaScript:
 *   const module = await DETICoinMiner();
 *   const attempts = module.ccall('mine_deti_coins', 'number', ['number'], [1000000]);
 */

#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <wasm_simd128.h>

typedef uint8_t u08_t;
typedef uint32_t u32_t;

// Maximum coins we can store
#define MAX_COINS 100

// Coin storage
static struct {
    u32_t message[14];
    u32_t hash[5];
    unsigned zeros;
} coins[MAX_COINS];

static unsigned coin_count = 0;

// SHA-1 rotate left
#define ROTATE_LEFT(x, n) (((x) << (n)) | ((x) >> (32 - (n))))

// SIMD SHA-1 implementation - process 4 messages in parallel
static void sha1_simd_x4(u32_t *data0, u32_t *data1, u32_t *data2, u32_t *data3,
                         u32_t *hash0, u32_t *hash1, u32_t *hash2, u32_t *hash3) {
    // SHA-1 constants (same for all 4 lanes)
    const u32_t K[] = {0x5a827999u, 0x6ed9eba1u, 0x8f1bbcdcu, 0xca62c1d6u};
    
    // Initialize hash values (vectorized - 4 lanes)
    v128_t h0_vec = wasm_i32x4_splat(0x67452301u);
    v128_t h1_vec = wasm_i32x4_splat(0xefcdab89u);
    v128_t h2_vec = wasm_i32x4_splat(0x98badcfeu);
    v128_t h3_vec = wasm_i32x4_splat(0x10325476u);
    v128_t h4_vec = wasm_i32x4_splat(0xc3d2e1f0u);
    
    // Message schedule for 4 parallel lanes
    v128_t w[80];
    
    // Load input data (14 words per message)
    for (int i = 0; i < 14; i++) {
        w[i] = wasm_i32x4_make(data0[i], data1[i], data2[i], data3[i]);
    }
    w[14] = wasm_i32x4_splat(0u);
    w[15] = wasm_i32x4_splat(56u * 8u); // Message length in bits
    
    // Expand message schedule (same logic for all 4 lanes)
    for (int i = 16; i < 80; i++) {
        v128_t temp = wasm_v128_xor(w[i-3], w[i-8]);
        temp = wasm_v128_xor(temp, w[i-14]);
        temp = wasm_v128_xor(temp, w[i-16]);
        // Rotate left by 1
        v128_t left = wasm_i32x4_shl(temp, 1);
        v128_t right = wasm_u32x4_shr(temp, 31);
        w[i] = wasm_v128_or(left, right);
    }
    
    // Working variables
    v128_t a = h0_vec, b = h1_vec, c = h2_vec, d = h3_vec, e = h4_vec;
    
    // Main compression loop (80 rounds)
    for (int i = 0; i < 80; i++) {
        v128_t f, k_vec;
        
        if (i < 20) {
            // f = (b & c) | ((~b) & d)
            f = wasm_v128_or(wasm_v128_and(b, c), 
                             wasm_v128_and(wasm_v128_not(b), d));
            k_vec = wasm_i32x4_splat(K[0]);
        } else if (i < 40) {
            // f = b ^ c ^ d
            f = wasm_v128_xor(wasm_v128_xor(b, c), d);
            k_vec = wasm_i32x4_splat(K[1]);
        } else if (i < 60) {
            // f = (b & c) | (b & d) | (c & d)
            f = wasm_v128_or(wasm_v128_or(wasm_v128_and(b, c),
                                          wasm_v128_and(b, d)),
                             wasm_v128_and(c, d));
            k_vec = wasm_i32x4_splat(K[2]);
        } else {
            // f = b ^ c ^ d
            f = wasm_v128_xor(wasm_v128_xor(b, c), d);
            k_vec = wasm_i32x4_splat(K[3]);
        }
        
        // temp = ROTATE_LEFT(a, 5) + f + e + k + w[i]
        v128_t a_rot5_left = wasm_i32x4_shl(a, 5);
        v128_t a_rot5_right = wasm_u32x4_shr(a, 27);
        v128_t a_rot5 = wasm_v128_or(a_rot5_left, a_rot5_right);
        
        v128_t temp = wasm_i32x4_add(a_rot5, f);
        temp = wasm_i32x4_add(temp, e);
        temp = wasm_i32x4_add(temp, k_vec);
        temp = wasm_i32x4_add(temp, w[i]);
        
        // Rotate
        e = d;
        d = c;
        // c = ROTATE_LEFT(b, 30)
        v128_t c_left = wasm_i32x4_shl(b, 30);
        v128_t c_right = wasm_u32x4_shr(b, 2);
        c = wasm_v128_or(c_left, c_right);
        b = a;
        a = temp;
    }
    
    // Add to initial hash values
    v128_t final_h0 = wasm_i32x4_add(h0_vec, a);
    v128_t final_h1 = wasm_i32x4_add(h1_vec, b);
    v128_t final_h2 = wasm_i32x4_add(h2_vec, c);
    v128_t final_h3 = wasm_i32x4_add(h3_vec, d);
    v128_t final_h4 = wasm_i32x4_add(h4_vec, e);
    
    // Extract results for each of the 4 lanes
    hash0[0] = wasm_i32x4_extract_lane(final_h0, 0);
    hash0[1] = wasm_i32x4_extract_lane(final_h1, 0);
    hash0[2] = wasm_i32x4_extract_lane(final_h2, 0);
    hash0[3] = wasm_i32x4_extract_lane(final_h3, 0);
    hash0[4] = wasm_i32x4_extract_lane(final_h4, 0);
    
    hash1[0] = wasm_i32x4_extract_lane(final_h0, 1);
    hash1[1] = wasm_i32x4_extract_lane(final_h1, 1);
    hash1[2] = wasm_i32x4_extract_lane(final_h2, 1);
    hash1[3] = wasm_i32x4_extract_lane(final_h3, 1);
    hash1[4] = wasm_i32x4_extract_lane(final_h4, 1);
    
    hash2[0] = wasm_i32x4_extract_lane(final_h0, 2);
    hash2[1] = wasm_i32x4_extract_lane(final_h1, 2);
    hash2[2] = wasm_i32x4_extract_lane(final_h2, 2);
    hash2[3] = wasm_i32x4_extract_lane(final_h3, 2);
    hash2[4] = wasm_i32x4_extract_lane(final_h4, 2);
    
    hash3[0] = wasm_i32x4_extract_lane(final_h0, 3);
    hash3[1] = wasm_i32x4_extract_lane(final_h1, 3);
    hash3[2] = wasm_i32x4_extract_lane(final_h2, 3);
    hash3[3] = wasm_i32x4_extract_lane(final_h3, 3);
    hash3[4] = wasm_i32x4_extract_lane(final_h4, 3);
}

// SHA-1 implementation (scalar fallback for single messages)
static void sha1(u32_t *data, u32_t *hash) {
    // SHA-1 constants
    const u32_t K[] = {0x5a827999u, 0x6ed9eba1u, 0x8f1bbcdcu, 0xca62c1d6u};
    
    // Initialize hash values
    u32_t h0 = 0x67452301u;
    u32_t h1 = 0xefcdab89u;
    u32_t h2 = 0x98badcfeu;
    u32_t h3 = 0x10325476u;
    u32_t h4 = 0xc3d2e1f0u;
    
    // Prepare message schedule
    u32_t w[80];
    for (int i = 0; i < 14; i++) {
        w[i] = data[i];
    }
    w[14] = 0u;
    w[15] = 56u * 8u; // Message length in bits
    
    for (int i = 16; i < 80; i++) {
        u32_t temp = w[i-3] ^ w[i-8] ^ w[i-14] ^ w[i-16];
        w[i] = (temp << 1) | (temp >> 31);
    }
    
    // Main loop
    u32_t a = h0, b = h1, c = h2, d = h3, e = h4;
    
    for (int i = 0; i < 80; i++) {
        u32_t f, k;
        if (i < 20) {
            f = (b & c) | ((~b) & d);
            k = K[0];
        } else if (i < 40) {
            f = b ^ c ^ d;
            k = K[1];
        } else if (i < 60) {
            f = (b & c) | (b & d) | (c & d);
            k = K[2];
        } else {
            f = b ^ c ^ d;
            k = K[3];
        }
        
        u32_t temp = ((a << 5) | (a >> 27)) + f + e + k + w[i];
        e = d;
        d = c;
        c = (b << 30) | (b >> 2);
        b = a;
        a = temp;
    }
    
    // Add to initial hash values
    hash[0] = h0 + a;
    hash[1] = h1 + b;
    hash[2] = h2 + c;
    hash[3] = h3 + d;
    hash[4] = h4 + e;
}

// Count leading zero bits
static unsigned count_leading_zeros(u32_t hash[5]) {
    // Check signature
    if (hash[0] != 0xaad20250u) return 0;
    
    unsigned zeros = 0;
    for (unsigned word = 1; word <= 4; word++) {
        u32_t v = hash[word];
        if (v == 0) {
            zeros += 32;
            continue;
        }
        // Count leading zeros in this word
        while ((v & 0x80000000u) == 0) {
            zeros++;
            v <<= 1;
        }
        break;
    }
    return zeros;
}

// Simple LCG random number generator
static u32_t rng_state = 12345;

static void seed_rng(u32_t seed) {
    rng_state = seed;
}

static u32_t rand_u32(void) {
    rng_state = rng_state * 1664525u + 1013904223u;
    return rng_state;
}

// Generate random printable ASCII character (excluding newline)
static char rand_char(void) {
    char c;
    do {
        c = (rand_u32() % 95) + 32; // ASCII 32-126
    } while (c == '\n');
    return c;
}

// Mine DETI coins with SIMD optimization (4 hashes in parallel)
// Returns number of attempts performed
unsigned long mine_deti_coins(unsigned long max_attempts) {
    // Seed RNG with current time (passed from JavaScript)
    seed_rng((u32_t)(max_attempts ^ 0xDEADBEEFu));
    
    union {
        u32_t w[14];
        u08_t c[56];
    } message[4]; // 4 parallel messages
    
    const char prefix[] = "DETI coin 2 ";
    
    unsigned long attempt = 0;
    
    // Process in batches of 4 for SIMD
    for (; attempt + 3 < max_attempts; attempt += 4) {
        // Generate 4 messages in parallel
        for (int lane = 0; lane < 4; lane++) {
            // Set prefix
            for (int i = 0; i < 12; i++) {
                message[lane].c[i ^ 3] = prefix[i];
            }
            
            // Generate random content (bytes 12-53)
            for (int i = 12; i < 54; i++) {
                message[lane].c[i ^ 3] = rand_char();
            }
            
            // Set padding
            message[lane].c[54 ^ 3] = '\n';
            message[lane].c[55 ^ 3] = 0x80;
        }
        
        // Calculate SHA-1 for all 4 messages in SIMD
        u32_t hash[4][5];
        sha1_simd_x4(message[0].w, message[1].w, message[2].w, message[3].w,
                     hash[0], hash[1], hash[2], hash[3]);
        
        // Check each result
        for (int lane = 0; lane < 4; lane++) {
            unsigned zeros = count_leading_zeros(hash[lane]);
            if (zeros > 0) {
                // Store coin if we have space
                if (coin_count < MAX_COINS) {
                    for (int i = 0; i < 14; i++) {
                        coins[coin_count].message[i] = message[lane].w[i];
                    }
                    for (int i = 0; i < 5; i++) {
                        coins[coin_count].hash[i] = hash[lane][i];
                    }
                    coins[coin_count].zeros = zeros;
                    coin_count++;
                }
            }
        }
    }
    
    // Handle remaining attempts (scalar fallback)
    for (; attempt < max_attempts; attempt++) {
        union {
            u32_t w[14];
            u08_t c[56];
        } msg;
        
        // Set prefix
        for (int i = 0; i < 12; i++) {
            msg.c[i ^ 3] = prefix[i];
        }
        
        // Generate random content
        for (int i = 12; i < 54; i++) {
            msg.c[i ^ 3] = rand_char();
        }
        
        // Set padding
        msg.c[54 ^ 3] = '\n';
        msg.c[55 ^ 3] = 0x80;
        
        // Calculate SHA-1
        u32_t hash[5];
        sha1(msg.w, hash);
        
        // Check if it's a valid coin
        unsigned zeros = count_leading_zeros(hash);
        if (zeros > 0) {
            // Store coin if we have space
            if (coin_count < MAX_COINS) {
                for (int i = 0; i < 14; i++) {
                    coins[coin_count].message[i] = msg.w[i];
                }
                for (int i = 0; i < 5; i++) {
                    coins[coin_count].hash[i] = hash[i];
                }
                coins[coin_count].zeros = zeros;
                coin_count++;
            }
        }
    }
    
    return max_attempts;
}

// Get number of coins found
unsigned get_found_count(void) {
    return coin_count;
}

// Get coin data (returns pointer to coin structure)
// JavaScript needs to read: message[14], hash[5], zeros
void* get_coin(unsigned index) {
    if (index >= coin_count) return NULL;
    return &coins[index];
}

// Reset coin storage
void reset_coins(void) {
    coin_count = 0;
}
