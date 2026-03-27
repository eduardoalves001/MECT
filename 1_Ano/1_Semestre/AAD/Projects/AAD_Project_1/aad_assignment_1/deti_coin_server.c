/*
 * deti_coin_server.c
 *
 * Distributed DETI Coin Mining - Server Component
 *
 * Architecture:
 * - Server coordinates work distribution across multiple mining clients
 * - Assigns unique nonce ranges to each client to avoid duplicate work
 * - Collects found coins from all clients
 * - Provides centralized statistics and monitoring
 *
 * Protocol:
 * - Client -> Server: "READY" (request work)
 * - Server -> Client: "WORK <start_nonce> <count>" (assign range)
 * - Client -> Server: "COIN <hash> <message>" (report found coin)
 * - Client -> Server: "STATS <attempts> <found>" (periodic statistics)
 * - Server -> Client: "SHUTDOWN" (graceful termination)
 *
 * Build: cc -O3 -Wall -Wshadow -Werror deti_coin_server.c -o deti_coin_server -lpthread
 *
 * Usage: ./deti_coin_server [port]
 *        Default port: 9999
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>
#include <errno.h>

// Type definitions (must be before aad_sha1_cpu.h)
typedef unsigned char u08_t;
typedef unsigned int u32_t;

#include "aad_sha1_cpu.h"

// Forward declaration
void save_coin_to_vault(u32_t message[14], u32_t hash[5], unsigned zeros, const char *msg_text);

#define DEFAULT_PORT 9999
#define MAX_CLIENTS 100
#define WORK_BATCH_SIZE 100000000ULL  // 100M attempts per work unit
#define BACKLOG 10

static volatile int keep_running = 1;

// Global statistics (protected by mutex)
typedef struct {
    unsigned long long total_attempts;
    unsigned total_coins_found;
    unsigned active_clients;
    unsigned long long next_nonce_range;
} server_stats_t;

server_stats_t stats = {0, 0, 0, 0};
pthread_mutex_t stats_mutex = PTHREAD_MUTEX_INITIALIZER;

// Client connection info
typedef struct {
    int socket_fd;
    struct sockaddr_in addr;
    unsigned long long nonce_start;
    unsigned long long nonce_count;
    time_t connected_time;
    unsigned long long attempts;
    unsigned coins_found;
} client_info_t;

static void int_handler(int signum) {
    (void)signum;
    keep_running = 0;
    printf("\n[SERVER] Shutting down gracefully...\n");
}

// Thread-safe statistics update
void update_stats(unsigned long long attempts, unsigned coins) {
    pthread_mutex_lock(&stats_mutex);
    stats.total_attempts += attempts;
    stats.total_coins_found += coins;
    pthread_mutex_unlock(&stats_mutex);
}

// Assign work to client (thread-safe)
void assign_work(client_info_t *client) {
    pthread_mutex_lock(&stats_mutex);
    client->nonce_start = stats.next_nonce_range;
    client->nonce_count = WORK_BATCH_SIZE;
    stats.next_nonce_range += WORK_BATCH_SIZE;
    pthread_mutex_unlock(&stats_mutex);
}

// Handle client connection
void *client_handler(void *arg) {
    client_info_t *client = (client_info_t *)arg;
    char buffer[4096];
    char response[256];
    
    printf("[SERVER] Client connected from %s:%d\n", 
           inet_ntoa(client->addr.sin_addr), 
           ntohs(client->addr.sin_port));
    
    pthread_mutex_lock(&stats_mutex);
    stats.active_clients++;
    pthread_mutex_unlock(&stats_mutex);
    
    while (keep_running) {
        // Receive message from client
        ssize_t bytes = recv(client->socket_fd, buffer, sizeof(buffer) - 1, 0);
        if (bytes <= 0) {
            break;  // Client disconnected
        }
        
        buffer[bytes] = '\0';
        
        // Parse client message
        if (strncmp(buffer, "READY", 5) == 0) {
            // Client requesting work
            assign_work(client);
            snprintf(response, sizeof(response), 
                     "WORK %llu %llu\n", 
                     client->nonce_start, 
                     client->nonce_count);
            send(client->socket_fd, response, strlen(response), 0);
            
        } else if (strncmp(buffer, "COIN", 4) == 0) {
            // Client found a coin
            // Parse coin message
            u32_t message[14];
            if (sscanf(buffer + 5, "%x %x %x %x %x %x %x %x %x %x %x %x %x %x",
                      &message[0], &message[1], &message[2], &message[3],
                      &message[4], &message[5], &message[6], &message[7],
                      &message[8], &message[9], &message[10], &message[11],
                      &message[12], &message[13]) == 14) {
                
                // Calculate SHA-1 hash
                u32_t hash[5];
                sha1(message, hash);
                
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
                    char c = ((u08_t*)message)[i ^ 3];
                    // Replace non-printable chars with '.'
                    msg_text[i] = (c >= 32 && c < 127) ? c : '.';
                }
                msg_text[54] = '\0';
                
                printf("\n👻 COIN FOUND by client %s:%d\n", 
                       inet_ntoa(client->addr.sin_addr), 
                       ntohs(client->addr.sin_port));
                printf("Value: V%02u\n", zeros);
                printf("Hash: %08x%08x%08x%08x%08x\n", 
                       hash[0], hash[1], hash[2], hash[3], hash[4]);
                printf("Message: %s\n", msg_text);
                
                save_coin_to_vault(message, hash, zeros, msg_text);
            }
            
            client->coins_found++;
            update_stats(0, 1);
            
            send(client->socket_fd, "ACK\n", 4, 0);
            
        } else if (strncmp(buffer, "STATS", 5) == 0) {
            // Client reporting statistics (sends total attempts, not delta)
            unsigned long long attempts;
            unsigned found;
            if (sscanf(buffer + 6, "%llu %u", &attempts, &found) == 2) {
                // Calculate delta from last report
                unsigned long long delta = attempts - client->attempts;
                client->attempts = attempts;
                update_stats(delta, 0);
            }
            send(client->socket_fd, "ACK\n", 4, 0);
            
        } else if (strncmp(buffer, "DONE", 4) == 0) {
            // Client completed work batch - acknowledge it
            // Client will send READY again to get new work
            send(client->socket_fd, "ACK\n", 4, 0);
        }
    }
    
    // Client disconnected
    printf("[SERVER] Client disconnected: %s:%d\n", 
           inet_ntoa(client->addr.sin_addr), 
           ntohs(client->addr.sin_port));
    
    pthread_mutex_lock(&stats_mutex);
    stats.active_clients--;
    pthread_mutex_unlock(&stats_mutex);
    
    close(client->socket_fd);
    free(client);
    return NULL;
}

// Statistics reporting thread
void *stats_reporter(void *arg) {
    (void)arg;
    time_t start_time = time(NULL);
    
    while (keep_running) {
        sleep(10);  // Report every 10 seconds
        
        pthread_mutex_lock(&stats_mutex);
        time_t elapsed = time(NULL) - start_time;
        double hps = elapsed > 0 ? (double)stats.total_attempts / elapsed : 0.0;
        
        printf("\n========================================\n");
        printf("SERVER STATISTICS\n");
        printf("========================================\n");
        printf("Uptime: %ld seconds\n", elapsed);
        printf("Active clients: %u\n", stats.active_clients);
        printf("Total attempts: %llu\n", stats.total_attempts);
        printf("Coins found: %u\n", stats.total_coins_found);
        printf("Hash rate: %.2f MH/s\n", hps / 1e6);
        printf("Next nonce: %llu\n", stats.next_nonce_range);
        printf("========================================\n\n");
        pthread_mutex_unlock(&stats_mutex);
    }
    
    return NULL;
}

int main(int argc, char *argv[]) {
    int port = DEFAULT_PORT;
    
    if (argc > 1) {
        port = atoi(argv[1]);
    }
    
    signal(SIGINT, int_handler);
    signal(SIGPIPE, SIG_IGN);  // Ignore broken pipe
    
    printf("========================================\n");
    printf("DETI Coin Mining Server\n");
    printf("========================================\n");
    printf("Port: %d\n", port);
    printf("Work batch size: %llu attempts\n", WORK_BATCH_SIZE);
    printf("========================================\n\n");
    
    // Create server socket
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) {
        perror("socket");
        return 1;
    }
    
    // Set socket options
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    
    // Bind to port
    struct sockaddr_in server_addr = {0};
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(port);
    
    if (bind(server_fd, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
        perror("bind");
        close(server_fd);
        return 1;
    }
    
    // Listen for connections
    if (listen(server_fd, BACKLOG) < 0) {
        perror("listen");
        close(server_fd);
        return 1;
    }
    
    printf("[SERVER] Listening on port %d...\n\n", port);
    
    // Start statistics reporter thread
    pthread_t stats_thread;
    pthread_create(&stats_thread, NULL, stats_reporter, NULL);
    
    // Accept client connections
    while (keep_running) {
        struct sockaddr_in client_addr;
        socklen_t addr_len = sizeof(client_addr);
        
        int client_fd = accept(server_fd, (struct sockaddr *)&client_addr, &addr_len);
        if (client_fd < 0) {
            if (errno == EINTR) continue;  // Interrupted by signal
            perror("accept");
            continue;
        }
        
        // Create client info structure
        client_info_t *client = malloc(sizeof(client_info_t));
        if (!client) {
            close(client_fd);
            continue;
        }
        
        client->socket_fd = client_fd;
        client->addr = client_addr;
        client->connected_time = time(NULL);
        client->attempts = 0;
        client->coins_found = 0;
        
        // Spawn thread to handle client
        pthread_t thread;
        if (pthread_create(&thread, NULL, client_handler, client) != 0) {
            perror("pthread_create");
            close(client_fd);
            free(client);
            continue;
        }
        
        pthread_detach(thread);  // Auto-cleanup when thread exits
    }
    
    // Cleanup
    printf("\n[SERVER] Closing server socket...\n");
    close(server_fd);
    
    pthread_mutex_lock(&stats_mutex);
    printf("\n========================================\n");
    printf("FINAL STATISTICS\n");
    printf("========================================\n");
    printf("Total attempts: %llu\n", stats.total_attempts);
    printf("Total coins found: %u\n", stats.total_coins_found);
    printf("========================================\n");
    pthread_mutex_unlock(&stats_mutex);
    
    return 0;
}

// Simple vault implementation (append coins to file)
void save_coin_to_vault(u32_t message[14], u32_t hash[5], unsigned zeros, const char *msg_text) {
    FILE *fp = fopen("deti_coins_v2_vault.txt", "a");
    if (!fp) return;
    
    // Write in simple one-line format: V<value>:<message text>
    fprintf(fp, "V%02u:%s\n", zeros, msg_text);
    
    fclose(fp);
}
