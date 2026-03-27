#!/bin/bash
#
# benchmark_all.sh
#
# Measures how many SHA-1 attempts each DETI coin miner implementation
# can perform in exactly 1 minute (60 seconds).
#
# Usage: ./benchmark_all.sh
#

set -e

BENCHMARK_TIME=60  # seconds
RESULTS_FILE="benchmark_results.txt"

echo "=========================================="
echo "DETI Coin Miner Benchmark Suite"
echo "Testing each implementation for ${BENCHMARK_TIME} seconds"
echo "=========================================="
echo ""

# Clear previous results
> "$RESULTS_FILE"

echo "Benchmark Results - $(date)" | tee -a "$RESULTS_FILE"
echo "Testing Duration: ${BENCHMARK_TIME} seconds per implementation" | tee -a "$RESULTS_FILE"
echo "========================================" | tee -a "$RESULTS_FILE"
echo "" | tee -a "$RESULTS_FILE"

#
# 1. CPU Non-SIMD (Reference Implementation)
#
echo "1. Testing: sha1_cpu_search (CPU, No SIMD)" | tee -a "$RESULTS_FILE"
echo "   Building..." 
make sha1_cpu_search > /dev/null 2>&1
echo "   Running for ${BENCHMARK_TIME} seconds..."

timeout ${BENCHMARK_TIME}s ./sha1_cpu_search 2>&1 | tee cpu_output.tmp &
PID=$!
sleep ${BENCHMARK_TIME}
kill -INT $PID 2>/dev/null || true
wait $PID 2>/dev/null || true

# Extract attempts and hash rate from output
CPU_ATTEMPTS=$(grep -oP 'Attempts=\K[0-9]+' cpu_output.tmp | tail -1 || echo "0")
CPU_HASHRATE=$(grep -oP 'Hashes_per_second=\K[0-9]+' cpu_output.tmp | tail -1 || echo "0")
CPU_FOUND=$(grep -oP 'Found=\K[0-9]+' cpu_output.tmp | tail -1 || echo "0")

echo "   Attempts: $(printf "%'d" $CPU_ATTEMPTS)" | tee -a "$RESULTS_FILE"
echo "   Hash Rate: $(printf "%'d" $CPU_HASHRATE) H/s" | tee -a "$RESULTS_FILE"
echo "   Coins Found: $CPU_FOUND" | tee -a "$RESULTS_FILE"
echo "" | tee -a "$RESULTS_FILE"

#
# 2. CPU AVX (4 lanes SIMD)
#
if [ -f "sha1_cpu_avx_search.c" ]; then
    echo "2. Testing: sha1_cpu_avx_search (CPU, AVX - 4 lanes)" | tee -a "$RESULTS_FILE"
    echo "   Building..."
    make sha1_cpu_avx_search > /dev/null 2>&1
    echo "   Running for ${BENCHMARK_TIME} seconds..."
    
    timeout ${BENCHMARK_TIME}s ./sha1_cpu_avx_search 2>&1 | tee avx_output.tmp &
    PID=$!
    sleep ${BENCHMARK_TIME}
    kill -INT $PID 2>/dev/null || true
    wait $PID 2>/dev/null || true
    
    AVX_ATTEMPTS=$(grep -oP 'Attempts=\K[0-9]+' avx_output.tmp | tail -1 || echo "0")
    AVX_HASHRATE=$(grep -oP 'Hashes_per_second=\K[0-9]+' avx_output.tmp | tail -1 || echo "0")
    AVX_FOUND=$(grep -oP 'Found=\K[0-9]+' avx_output.tmp | tail -1 || echo "0")
    
    echo "   Attempts: $(printf "%'d" $AVX_ATTEMPTS)" | tee -a "$RESULTS_FILE"
    echo "   Hash Rate: $(printf "%'d" $AVX_HASHRATE) H/s" | tee -a "$RESULTS_FILE"
    echo "   Coins Found: $AVX_FOUND" | tee -a "$RESULTS_FILE"
    echo "" | tee -a "$RESULTS_FILE"
fi

#
# 3. CPU AVX2 (8 lanes SIMD)
#
if [ -f "sha1_cpu_avx2_search.c" ]; then
    echo "3. Testing: sha1_cpu_avx2_search (CPU, AVX2 - 8 lanes)" | tee -a "$RESULTS_FILE"
    echo "   Building..."
    make sha1_cpu_avx2_search > /dev/null 2>&1
    echo "   Running for ${BENCHMARK_TIME} seconds..."
    
    timeout ${BENCHMARK_TIME}s ./sha1_cpu_avx2_search 2>&1 | tee avx2_output.tmp &
    PID=$!
    sleep ${BENCHMARK_TIME}
    kill -INT $PID 2>/dev/null || true
    wait $PID 2>/dev/null || true
    
    AVX2_ATTEMPTS=$(grep -oP 'Attempts=\K[0-9]+' avx2_output.tmp | tail -1 || echo "0")
    AVX2_HASHRATE=$(grep -oP 'Hashes_per_second=\K[0-9]+' avx2_output.tmp | tail -1 || echo "0")
    AVX2_FOUND=$(grep -oP 'Found=\K[0-9]+' avx2_output.tmp | tail -1 || echo "0")
    
    echo "   Attempts: $(printf "%'d" $AVX2_ATTEMPTS)" | tee -a "$RESULTS_FILE"
    echo "   Hash Rate: $(printf "%'d" $AVX2_HASHRATE) H/s" | tee -a "$RESULTS_FILE"
    echo "   Coins Found: $AVX2_FOUND" | tee -a "$RESULTS_FILE"
    echo "" | tee -a "$RESULTS_FILE"
fi

#
# 4. CPU OpenMP + AVX2 (Multi-threaded)
#
if [ -f "sha1_cpu_openmp_avx2_search.c" ]; then
    echo "4. Testing: sha1_cpu_openmp_avx2_search (CPU, OpenMP + AVX2)" | tee -a "$RESULTS_FILE"
    echo "   Building..."
    make sha1_cpu_openmp_avx2_search > /dev/null 2>&1
    echo "   Running for ${BENCHMARK_TIME} seconds..."
    
    timeout ${BENCHMARK_TIME}s ./sha1_cpu_openmp_avx2_search 2>&1 | tee openmp_output.tmp &
    PID=$!
    sleep ${BENCHMARK_TIME}
    kill -INT $PID 2>/dev/null || true
    wait $PID 2>/dev/null || true
    
    OPENMP_ATTEMPTS=$(grep -oP 'Attempts=\K[0-9]+' openmp_output.tmp | tail -1 || echo "0")
    OPENMP_HASHRATE=$(grep -oP 'Hashes_per_second=\K[0-9]+' openmp_output.tmp | tail -1 || echo "0")
    OPENMP_FOUND=$(grep -oP 'Found=\K[0-9]+' openmp_output.tmp | tail -1 || echo "0")
    
    echo "   Attempts: $(printf "%'d" $OPENMP_ATTEMPTS)" | tee -a "$RESULTS_FILE"
    echo "   Hash Rate: $(printf "%'d" $OPENMP_HASHRATE) H/s" | tee -a "$RESULTS_FILE"
    echo "   Coins Found: $OPENMP_FOUND" | tee -a "$RESULTS_FILE"
    echo "" | tee -a "$RESULTS_FILE"
fi

#
# 5. CUDA (GPU)
#
if [ -f "sha1_cuda_search.cu" ]; then
    echo "5. Testing: sha1_cuda_search (CUDA GPU)" | tee -a "$RESULTS_FILE"
    echo "   Building..."
    make sha1_cuda_search > /dev/null 2>&1
    echo "   Running for ${BENCHMARK_TIME} seconds..."
    
    timeout ${BENCHMARK_TIME}s ./sha1_cuda_search 2>&1 | tee cuda_output.tmp &
    PID=$!
    sleep ${BENCHMARK_TIME}
    kill -INT $PID 2>/dev/null || true
    wait $PID 2>/dev/null || true
    
    CUDA_ATTEMPTS=$(grep -oP 'Attempts=\K[0-9]+' cuda_output.tmp | tail -1 || echo "0")
    CUDA_HASHRATE=$(grep -oP 'Hashes_per_second=\K[0-9]+' cuda_output.tmp | tail -1 || echo "0")
    CUDA_FOUND=$(grep -oP 'Found=\K[0-9]+' cuda_output.tmp | tail -1 || echo "0")
    
    echo "   Attempts: $(printf "%'d" $CUDA_ATTEMPTS)" | tee -a "$RESULTS_FILE"
    echo "   Hash Rate: $(printf "%'d" $CUDA_HASHRATE) H/s" | tee -a "$RESULTS_FILE"
    echo "   Coins Found: $CUDA_FOUND" | tee -a "$RESULTS_FILE"
    echo "" | tee -a "$RESULTS_FILE"
fi

#
# Summary Table
#
echo "========================================" | tee -a "$RESULTS_FILE"
echo "SUMMARY" | tee -a "$RESULTS_FILE"
echo "========================================" | tee -a "$RESULTS_FILE"
echo "" | tee -a "$RESULTS_FILE"

printf "%-30s %15s %15s %10s\n" "Implementation" "Attempts (60s)" "Hash Rate" "Speedup" | tee -a "$RESULTS_FILE"
printf "%-30s %15s %15s %10s\n" "------------------------------" "---------------" "---------------" "----------" | tee -a "$RESULTS_FILE"

# CPU baseline
if [ "$CPU_ATTEMPTS" != "0" ]; then
    printf "%-30s %15s %15s %10s\n" "CPU (No SIMD)" "$(printf "%'d" $CPU_ATTEMPTS)" "$(printf "%'d" $CPU_HASHRATE) H/s" "1.00x" | tee -a "$RESULTS_FILE"
fi

# AVX
if [ "$AVX_ATTEMPTS" != "0" ]; then
    AVX_SPEEDUP=$(echo "scale=2; $AVX_ATTEMPTS / $CPU_ATTEMPTS" | bc)
    printf "%-30s %15s %15s %10s\n" "CPU AVX (4 lanes)" "$(printf "%'d" $AVX_ATTEMPTS)" "$(printf "%'d" $AVX_HASHRATE) H/s" "${AVX_SPEEDUP}x" | tee -a "$RESULTS_FILE"
fi

# AVX2
if [ "$AVX2_ATTEMPTS" != "0" ]; then
    AVX2_SPEEDUP=$(echo "scale=2; $AVX2_ATTEMPTS / $CPU_ATTEMPTS" | bc)
    printf "%-30s %15s %15s %10s\n" "CPU AVX2 (8 lanes)" "$(printf "%'d" $AVX2_ATTEMPTS)" "$(printf "%'d" $AVX2_HASHRATE) H/s" "${AVX2_SPEEDUP}x" | tee -a "$RESULTS_FILE"
fi

# OpenMP
if [ "$OPENMP_ATTEMPTS" != "0" ]; then
    OPENMP_SPEEDUP=$(echo "scale=2; $OPENMP_ATTEMPTS / $CPU_ATTEMPTS" | bc)
    printf "%-30s %15s %15s %10s\n" "CPU OpenMP+AVX2" "$(printf "%'d" $OPENMP_ATTEMPTS)" "$(printf "%'d" $OPENMP_HASHRATE) H/s" "${OPENMP_SPEEDUP}x" | tee -a "$RESULTS_FILE"
fi

# CUDA
if [ "$CUDA_ATTEMPTS" != "0" ]; then
    CUDA_SPEEDUP=$(echo "scale=2; $CUDA_ATTEMPTS / $CPU_ATTEMPTS" | bc)
    printf "%-30s %15s %15s %10s\n" "CUDA GPU" "$(printf "%'d" $CUDA_ATTEMPTS)" "$(printf "%'d" $CUDA_HASHRATE) H/s" "${CUDA_SPEEDUP}x" | tee -a "$RESULTS_FILE"
fi

echo "" | tee -a "$RESULTS_FILE"
echo "========================================" | tee -a "$RESULTS_FILE"
echo "Benchmark complete!" | tee -a "$RESULTS_FILE"
echo "Full results saved to: $RESULTS_FILE" | tee -a "$RESULTS_FILE"
echo "========================================" | tee -a "$RESULTS_FILE"

# Cleanup temp files
rm -f cpu_output.tmp avx_output.tmp avx2_output.tmp openmp_output.tmp cuda_output.tmp

echo ""
echo "Done! Check $RESULTS_FILE for complete results."
