// Ficheiro: desafio_poema_coin_fixed.c

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <immintrin.h>
#include <stdint.h>
#include <time.h>
#include <unistd.h>
#include <ctype.h>     // isprint

#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"

#define N_MESSAGES 8
#define N_WORDS_PER_MSG 14
#define N_HASH_WORDS 5

#define RANDOM_START_BYTE 12
#define RANDOM_END_BYTE 53

#define FIXED_CUSTOM_BYTES 35
#define RANDOM_TAIL_BYTES (RANDOM_END_BYTE - RANDOM_START_BYTE + 1 - FIXED_CUSTOM_BYTES)
#define RANDOM_TAIL_START (RANDOM_START_BYTE + FIXED_CUSTOM_BYTES)

/* Poema (cada linha 35 chars) */
const char *PoemLines[] = {
    "Para ser grande, se inteiro: nada ",
    "Teu exagera ou exclui.             ",
    "Se todo em cada coisa. Poe quanto es",
    "No minimo que fazes.                ",
    "Assim em cada lago a lua toda       ",
    "Brilha, porque alta vive.           ",
    "                                   "
};
#define NUM_POEM_LINES (sizeof(PoemLines) / sizeof(PoemLines[0]))
#define CURRENT_LINE_INDEX 0

typedef struct { uint32_t words[N_WORDS_PER_MSG]; } scalar_coin_t;

/* de_interleave unchanged */
static void de_interleave_coin(v8si *interleaved8_data, int coin_index, uint32_t *scalar_coin)
{
    for (int w = 0; w < N_WORDS_PER_MSG; w++) {
        scalar_coin[w] = ((uint32_t *)&interleaved8_data[w])[coin_index];
    }
}

/* gerar 6 chars aleatórios imprimíveis */
static void generate_random_chars(char *buffer, int length)
{
    const char charset[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?/~`";
    const int charset_len = (int)(sizeof(charset) - 1);
    for (int i = 0; i < length; i++) buffer[i] = charset[rand() % charset_len];
    buffer[length] = '\0';
}

static char byte_to_printable(uint8_t byte)
{
    if (isprint(byte) && byte != '\n' && byte != '\r' && byte != '\t') return (char)byte;
    return ' ';
}

static void save_to_vault_format(uint32_t coin[N_WORDS_PER_MSG], int version)
{
    FILE *vault = fopen("vault.txt", "a");
    if (!vault) {
        vault = fopen("vault.txt", "w");
        if (!vault) { fprintf(stderr,"Erro ao criar vault.txt\n"); return; }
    }

    char random_suffix[7];
    generate_random_chars(random_suffix, 6);

    char coin_chars[56]; // 55 visíveis + '\0'
    uint8_t *byte_ptr = (uint8_t *)coin;

    // Reconstituir bytes correctos usando idx ^ 3
    for (int i = 0; i < 55; i++) {
        uint8_t b = byte_ptr[i ^ 3];
        coin_chars[i] = byte_to_printable(b);
    }
    coin_chars[55] = '\0';

    fprintf(vault, "V%02d:DETI coin 2 %s %s\n", version, coin_chars, random_suffix);
    fclose(vault);
    printf("Moeda adicionada ao vault.txt \n");
}

/* byte aleatório */
static uint8_t random_byte_custom(void) { return (uint8_t)(rand() % 256); }

int main(void)
{
    scalar_coin_t temp_coins[N_MESSAGES];
    v8si interleaved_data[N_WORDS_PER_MSG];
    v8si interleaved_hash[N_HASH_WORDS];
    uint32_t found_coin[N_WORDS_PER_MSG];

    printf("MINERAÇÃO DE MOEDAS DETICOIN - FERNANDO PESSOA\n");

    srand((unsigned int)time(NULL));
    unsigned long n_hashes = 0u;
    long attempts = 0;
    int version = 0;

    if (CURRENT_LINE_INDEX >= NUM_POEM_LINES) {
        fprintf(stderr,"ERRO: CURRENT_LINE_INDEX fora do range\n"); return 1;
    }
    const char *custom_pattern = PoemLines[CURRENT_LINE_INDEX];
    const uint32_t TARGET_SIGNATURE = 0x5002D2AAu;

    while (1) {
        attempts++;

        for (int m = 0; m < N_MESSAGES; m++) {
            uint8_t temp_msg[N_WORDS_PER_MSG * 4];
            memset(temp_msg, 0, sizeof(temp_msg));

            memcpy(temp_msg, "DETI coin 2 ", 12);
            memcpy(temp_msg + RANDOM_START_BYTE, custom_pattern, FIXED_CUSTOM_BYTES);

            for (uint32_t i = RANDOM_TAIL_START; i <= RANDOM_END_BYTE; i++) {
                uint8_t byte;
                do { byte = random_byte_custom(); } while (byte == '\n' || byte == 0);
                temp_msg[i] = byte;
            }

            temp_msg[54] = '\n';
            temp_msg[55] = 0x80;

            for (int w = 0; w < N_WORDS_PER_MSG; w++) {
                uint32_t word = 0;
                word = ((uint32_t)temp_msg[w*4 + 0] << 24) |
                       ((uint32_t)temp_msg[w*4 + 1] << 16) |
                       ((uint32_t)temp_msg[w*4 + 2] << 8) |
                       ((uint32_t)temp_msg[w*4 + 3] << 0);
                temp_coins[m].words[w] = word;
            }
        }

        for (int w = 0; w < N_WORDS_PER_MSG; w++) {
            interleaved_data[w] = _mm256_set_epi32(
                temp_coins[7].words[w], temp_coins[6].words[w],
                temp_coins[5].words[w], temp_coins[4].words[w],
                temp_coins[3].words[w], temp_coins[2].words[w],
                temp_coins[1].words[w], temp_coins[0].words[w]
            );
        }

        sha1_avx2(interleaved_data, interleaved_hash);
        n_hashes += N_MESSAGES;

        
        uint32_t *h0_scalar = (uint32_t *)&interleaved_hash[0];

        for (int i = 0; i < N_MESSAGES; i++) {
            if (h0_scalar[i] == TARGET_SIGNATURE) {
                printf("\n*** MOEDA FOI ENCONTRADA!!! ***\nAttempts: %ld  Hashes: %lu  Hash0: 0x%08X\n",
                       attempts, n_hashes, h0_scalar[i]);

                de_interleave_coin(interleaved_data, i, found_coin);

                uint8_t *found_bytes = (uint8_t *)found_coin;
                printf("MOEDA (55 bytes visíveis, depois padding):\n");
                for (int b = 0; b < 55; b++) {
                    uint8_t cb = found_bytes[b ^ 3];
                    if (isprint(cb) && cb != '\n') putchar(cb);
                    else if (cb == '\n') putchar(' ');
                    else putchar('.');
                }
                putchar('\n');

                save_to_vault_format(found_coin, version);

                char hex_filename[64];
                sprintf(hex_filename, "moeda_linha%d_hex.txt", CURRENT_LINE_INDEX + 1);
                FILE *hex_file = fopen(hex_filename, "w");
                if (hex_file) {
                    for (int w = 0; w < N_WORDS_PER_MSG; w++) fprintf(hex_file, "%08X ", found_coin[w]);
                    fclose(hex_file);
                    printf("Hex salvo em %s\n", hex_filename);
                }

                save_coin(found_coin);
                return 0;
            }
        }

        if ((attempts & 0x1FFFF) == 0) { putchar('.'); fflush(stdout); }
    }
    return 0;
}