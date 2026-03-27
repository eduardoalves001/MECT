# AAD_Project_1
First Project of the course High-Performance Architectures.

## Grade
Project grade: 18.5/20

## Theme
The theme of this project is based on the mining of DETI Coins, digital tokens that are created by constructing 55-byte files whose SHA-1 hash begins with a specific signature. The task involves generating valid coins by carefully crafting file contents and searching for combinations that produce increasingly rare hash patterns, effectively simulating a proof-of-work system. To achive this, we are challenge to try different approaches like Cpiu without SIMD, use of AVX, CUDA, etc ...

## Group Members

| NMec | Name | Email |
|:---:|:---|:---:|
| 104179 | EDUARDO ALVES  | eduardoalves@ua.pt |
| 104090 | TOMÁS RODRIGUES | tcercarodrigues@ua.pt |

## Work objectives per category

- (mandatory) Search for DETI coins using the CPU (without using SIMD instructions). ✅
- (mandatory) Search for DETI coins using AVX or NEON instructions. ✅
- (mandatory) Search for DETI coins using CUDA.✅
- (required) Search for DETI coins with a special form(say,with part of your name embedded in it). ✅
- (required) Compute an histogram of the wall time it takes to run a CUDA search kernel. ✅
- (required) Compute an histogram of the number of DETI coins found in each CUDA kernel run. ✅
- (required) For each of the above, measure how many attempts (SHA1 secure hash computations) you were
able to do in one minute. ✅
- (recommended) Do it with SIMD instructions and OpenMP. ✅
- (recommended) Do it using a server and many clients. ✅
- (recommended) Do it using Web Assembly. ✅
- (recommended) Compare the performance,i.e.,the numbers of attempts per minute,of as many computing
devices as possible(recent and old computers,micro-controllers,smartphones,anything where you
were ableto run the program). ✅
- (awesome) Do it using WebAssembly and SIMD instructions. ✅
- (optional) Search for DETI coins using AVX2 instructions. ✅
- (optional) Search for DETI coins using AVX512F instructions. (not possible to implement in our context) ❌ 
- (optional) How about an OpenCL implementation? oneAPI implementation? ROCm implementation? ❌
- (optional) Something else (surprise the teacher). ✅

# Performance

| Approaches | Attempts (60s) | Hash Rate | Speedup | 
|:---:|:---|:---:|:---:|
| CPU (No SIMD) | 450000000 | 7592433 H/s | 1.00x |
| AVX | 600000000 | 10678584 H/s | 1.33x |
| AVX2 | 900000000 | 15246241 H/s | 2.00x |
| AVX2 (SIMD) and OpenMP | 2678844336 | 44940279 H/s | 5.95x |
| CUDA | 41943040000 | 812302465 H/s | 93.20x |
| Web Assembly (No SIMD)| 172500000 | 2875000 H/s | 0.38x |
| Web Assembly and SIMD | 365000000 | 6000000 H/s | 0.79x |

Disclaimer: Web Assembly (No SIMD) was removed after SIMD version was implemented. But the results were noted. Since the assignment's task: recommended Do it using WebAssembly didnt specify if it wanted the non SIMD version.
So we decided to merge these two tasks as one implementation:
- (recommended) Do it using Web Assembly.
- (awesome) Do it using Web Assembly and SIMD instructions.


