#
# Arquiteturas de Alto Desempenho 2025/2026
#
# makefile for the first practical assignment (A1)
#
# makefile automatic variables:
#   $@ is the name of the target
#   $< is the name of the first prerequisite
#   $^ is the list of names of all prerequisites (without duplicates)
#

#
# CUDA installation directory --- /usr/local/cuda or $(CUDA_HOME)
#

CUDA_DIR = /usr/local/cuda


#
# Emscripten compiler (for WebAssembly builds)
#

EMCC = $(HOME)/Desktop/emsdk/upstream/emscripten/emcc


#
# OpenCL installation directory (for a NVidia graphics card, sama as CUDA)
#

OPENCL_DIR = $(CUDA_DIR)


#
# CUDA device architecture
#
#   GeForce GTX 1660 Ti --- sm_75
#   RTX A2000 Ada --------- sm_86
#   RTX A6000 Ada --------- sm_86
#   RTX 4070 -------------- sm_89
#

CUDA_ARCH = sm_75


#
# clean up
#

clean:
	rm -f sha1_tests
	rm -f sha1_cuda_test sha1_cuda_kernel.cubin
	rm -f a.out
	rm -f deti_coin_wasm.js deti_coin_wasm.wasm


#
# test the CUSTOM_SHA1_CODE macro
#

sha1_tests:	aad_sha1_cpu_tests.c aad_sha1.h aad_data_types.h aad_utilities.h makefile
	cc -march=native -Wall -Wshadow -Werror -O3 $< -o $@

sha1_cuda_test:	aad_sha1_cuda_test.c sha1_cuda_kernel.cubin aad_sha1.h aad_data_types.h aad_utilities.h aad_cuda_utilities.h makefile
	cc -march=native -Wall -Wshadow -Werror -O3 $< -o $@ -lcuda

sha1_cpu_search: sha1_cpu_search.c aad_sha1_cpu.h aad_vault.h aad_utilities.h aad_data_types.h makefile
	cc -fno-tree-vectorize -fno-tree-slp-vectorize -mno-avx -mno-avx2 -Wall -Wshadow -Werror -O3 $< -o $@

sha1_cpu_avx_search: sha1_cpu_avx_search.c aad_sha1_cpu.h aad_vault.h aad_utilities.h aad_data_types.h makefile
	cc -march=native -mavx -Wall -Wshadow -Werror -O3 $< -o $@

sha1_cpu_avx2_search: sha1_cpu_avx2_search.c aad_sha1_cpu.h aad_vault.h aad_utilities.h aad_data_types.h makefile
	cc -march=native -mavx -Wall -Wshadow -Werror -O3 $< -o $@

sha1_cpu_openmp_avx2_search: sha1_cpu_openmp_avx2_search.c aad_sha1_cpu.h aad_vault.h aad_utilities.h aad_data_types.h makefile
	cc -fopenmp -march=native -mavx2 -Wall -Wshadow -Werror -O3 $< -o $@

#
# Distributed mining server and clients
#

deti_coin_server: deti_coin_server.c aad_data_types.h aad_utilities.h aad_vault.h makefile
	cc -O3 -Wall -Wshadow -Werror $< -o $@ -lpthread

deti_coin_client_cpu: deti_coin_client_cpu.c aad_sha1_cpu.h aad_data_types.h aad_utilities.h makefile
	cc -march=native -mavx2 -O3 -Wall -Wshadow -Werror $< -o $@

deti_coin_client_cuda: deti_coin_client_cuda.cu aad_sha1.h aad_data_types.h aad_utilities.h makefile
	nvcc -O3 -use_fast_math -arch=sm_60 -I$(CUDA_DIR)/include -lcurand $< -o $@

#
# WebAssembly SIMD miner for browser
#

deti_coin_wasm: deti_coin_wasm.c makefile
	$(EMCC) -O3 $< -o deti_coin_wasm.js \
		-msimd128 \
		-s EXPORTED_FUNCTIONS='["_mine_deti_coins","_get_found_count","_get_coin","_reset_coins","_malloc","_free"]' \
		-s EXPORTED_RUNTIME_METHODS='["ccall","cwrap","UTF8ToString"]' \
		-s ALLOW_MEMORY_GROWTH=1 \
		-s MODULARIZE=1 \
		-s EXPORT_NAME='DETICoinMiner'


#
# compile the CUDA kernels
#

sha1_cuda_kernel.cubin:			aad_sha1_cuda_kernel.cu aad_sha1.h makefile
	nvcc -arch=$(CUDA_ARCH) --compiler-options -O2,-Wall -I$(CUDA_DIR)/include --cubin $< -o $@

#
# compile the CUDA DETI coin miner
#

sha1_cuda_search:			sha1_cuda_search.cu aad_sha1.h aad_data_types.h aad_utilities.h makefile
	nvcc -O3 -arch=sm_50 -I$(CUDA_DIR)/include -L$(CUDA_DIR)/lib64 -lcurand $< -o $@

#
# compile the CUDA special-form DETI coin miner (optimized for cache efficiency)
#

sha1_cuda_special_search:		sha1_cuda_special_search.cu aad_sha1.h aad_data_types.h aad_utilities.h makefile
	nvcc -O3 -use_fast_math -arch=sm_60 --ptxas-options=-v -I$(CUDA_DIR)/include $< -o $@

#
# compile the CUDA kernel histogram tool (measures kernel execution time distribution)
#

sha1_cuda_kernel_histogram:		sha1_cuda_kernel_histogram.cu aad_sha1.h aad_data_types.h aad_utilities.h makefile
	nvcc -O3 -use_fast_math -arch=sm_60 -I$(CUDA_DIR)/include $< -o $@

#
# compile the CUDA coins histogram tool (measures coins found per kernel run distribution)
#

sha1_cuda_coins_histogram:		sha1_cuda_coins_histogram.cu aad_sha1.h aad_data_types.h aad_utilities.h makefile
	nvcc -O3 -use_fast_math -arch=sm_60 -I$(CUDA_DIR)/include -lcurand $< -o $@
