#include "Golomb.h"
#include "bit_stream/src/bit_stream.h"
#include <sndfile.hh>
#include <iostream>
#include <string>
#include <cstring>
#include <vector>
#include <cmath>
#include <algorithm>
#include <numeric>

// Predictor types
enum class PredictorType {
    ORDER_1 = 0,
    ORDER_2 = 1,
    ORDER_3 = 2
};

// Stereo modes
enum class StereoMode {
    INDEPENDENT = 0,
    MID_SIDE = 1
};

// Predict next sample based on previous samples
int16_t predict(const std::vector<int16_t>& samples, size_t index, PredictorType predictor) {
    if (index == 0) return 0;
    
    auto clamp = [](int32_t val) { 
        return static_cast<int16_t>(std::clamp(val, -32768, 32767)); 
    };
    
    switch (predictor) {
        case PredictorType::ORDER_1:
            return samples[index - 1];
            
        case PredictorType::ORDER_2:
            if (index < 2) return samples[index - 1];
            return clamp(2 * samples[index - 1] - samples[index - 2]);
            
        case PredictorType::ORDER_3:
            if (index < 2) return samples[index - 1];
            if (index < 3) return clamp(2 * samples[index - 1] - samples[index - 2]);
            return clamp(3 * samples[index - 1] - 3 * samples[index - 2] + samples[index - 3]);
            
        default:
            return 0;
    }
}

// Estimate optimal Golomb parameter m for a block of residuals
// For geometric distribution: optimal m ≈ ceil(-1/log2(p)) where p = mean/(mean+1)
unsigned int estimateGolombParameter(const std::vector<int>& residuals) {
    if (residuals.empty()) return 1;
    
    double mean = std::accumulate(residuals.begin(), residuals.end(), 0.0,
                                  [](double sum, int r) { return sum + std::abs(r); }) 
                  / residuals.size();
    
    if (mean < 0.5) return 1;
    
    // Optimal m for geometric distribution: m ≈ ceil(-1 / log2(p))
    // where p = mean / (mean + 1)
    double p = mean / (mean + 1.0);
    unsigned int m = static_cast<unsigned int>(std::ceil(-1.0 / std::log2(p)));
    return std::clamp(m, 1u, 65535u);
}

// Convert stereo to mid-side
// Using the lossless formulation: mid = (L+R)/2, side = L-R
// Then recover: L = mid + (side+1)/2, R = mid - (side+1)/2 when side is odd
void convertToMidSide(const std::vector<int16_t>& left, const std::vector<int16_t>& right,
                      std::vector<int16_t>& mid, std::vector<int16_t>& side) {
    size_t n = left.size();
    mid.resize(n);
    side.resize(n);
    
    for (size_t i = 0; i < n; i++) {
        int32_t l = left[i];
        int32_t r = right[i];
        mid[i] = static_cast<int16_t>((l + r) >> 1);
        side[i] = static_cast<int16_t>(l - r);
    }
}

// Convert mid-side to stereo
void convertFromMidSide(const std::vector<int16_t>& mid, const std::vector<int16_t>& side,
                        std::vector<int16_t>& left, std::vector<int16_t>& right) {
    size_t n = mid.size();
    left.resize(n);
    right.resize(n);
    
    for (size_t i = 0; i < n; i++) {
        int32_t m = mid[i];
        int32_t s = side[i];
        // Lossless recovery
        left[i] = static_cast<int16_t>(m + (s >> 1) + (s & 1));
        right[i] = static_cast<int16_t>(m - (s >> 1));
    }
}

void printUsage(const char* progName) {
    std::cout << "Audio Codec - Lossless audio compression using Golomb coding\n\n"
              << "Usage:\n"
              << "  Encoding: " << progName << " -e [options] <input.wav> <output.agol>\n"
              << "  Decoding: " << progName << " -d <input.agol> <output.wav>\n\n"
              << "Options:\n"
              << "  -p <0-2>  Predictor: 0=Order-1, 1=Order-2 [default], 2=Order-3\n"
              << "  -s <0-1>  Stereo: 0=Independent, 1=Mid-Side [default]\n"
              << "  -m <int>  Fixed Golomb m (default: adaptive)\n"
              << "  -n <0-1>  Negative mode: 0=Interleaved [default], 1=Sign-Magnitude\n\n"
              << "Examples:\n"
              << "  " << progName << " -e input.wav output.agol\n"
              << "  " << progName << " -e -n 1 input.wav output.agol  # use sign-magnitude\n"
              << "  " << progName << " -d output.agol decoded.wav\n";
}

// Encode a single channel
void encodeChannel(const std::vector<int16_t>& samples, BitStream& bs,
                   PredictorType predictor, bool adaptiveM, unsigned int fixedM,
                   GolombCoding::NegativeMode negativeMode) {
    const size_t BLOCK_SIZE = 1024;
    size_t numSamples = samples.size();
    size_t pos = 0;
    
    while (pos < numSamples) {
        size_t blockEnd = std::min(pos + BLOCK_SIZE, numSamples);
        size_t blockSize = blockEnd - pos;
        
        std::vector<int> residuals;
        residuals.reserve(blockSize);
        
        for (size_t i = pos; i < blockEnd; i++) {
            int16_t prediction = predict(samples, i, predictor);
            int residual = static_cast<int>(samples[i]) - static_cast<int>(prediction);
            residuals.push_back(residual);
        }
        
        unsigned int m = adaptiveM ? estimateGolombParameter(residuals) : fixedM;
        
        bs.write_n_bits(m, 16);
        
        GolombCoding golomb(m, negativeMode);
        
        for (int residual : residuals) {
            std::vector<bool> encoded = golomb.encode(residual);
            for (bool bit : encoded) {
                bs.write_bit(bit ? 1 : 0);
            }
        }
        
        pos = blockEnd;
    }
}

// Decode a single channel
std::vector<int16_t> decodeChannel(BitStream& bs, size_t numSamples, PredictorType predictor,
                                    GolombCoding::NegativeMode negativeMode) {
    const size_t BLOCK_SIZE = 1024;
    std::vector<int16_t> samples;
    samples.reserve(numSamples);
    
    size_t pos = 0;
    
    while (pos < numSamples) {
        unsigned int m = static_cast<unsigned int>(bs.read_n_bits(16));
        size_t blockEnd = std::min(pos + BLOCK_SIZE, numSamples);
        size_t blockSize = blockEnd - pos;
        
        GolombCoding golomb(m, negativeMode);
        
        for (size_t i = 0; i < blockSize; i++) {
            std::vector<bool> bits;
            
            while (bits.push_back(bs.read_bit() != 0), !bits.back()) {}
            
            unsigned int b = static_cast<unsigned int>(std::floor(std::log2(m)));
            unsigned int cutoff = (1 << (b + 1)) - m;
            
            for (unsigned int j = 0; j < b; j++) {
                bits.push_back(bs.read_bit() != 0);
            }
            
            unsigned int r = 0;
            for (unsigned int j = 0; j < b; j++) {
                r = (r << 1) | (bits[bits.size() - b + j] ? 1 : 0);
            }
            if (r >= cutoff) {
                bits.push_back(bs.read_bit() != 0);
            }
            
            auto [residual, bitsUsed] = golomb.decode(bits, 0);
            int16_t prediction = predict(samples, samples.size(), predictor);
            int32_t sample = std::clamp(static_cast<int32_t>(prediction) + residual, 
                                        static_cast<int32_t>(-32768), 
                                        static_cast<int32_t>(32767));
            samples.push_back(static_cast<int16_t>(sample));
        }
        
        pos = blockEnd;
    }
    
    return samples;
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        printUsage(argv[0]);
        return 1;
    }
    
    bool decodeMode = false;
    
    if (std::strcmp(argv[1], "-e") == 0) {
    } else if (std::strcmp(argv[1], "-d") == 0) {
        decodeMode = true;
    } else {
        std::cerr << "Error: first argument must be -e (encode) or -d (decode)\n\n";
        printUsage(argv[0]);
        return 1;
    }
    
    if (decodeMode) {
        if (argc != 4) {
            std::cerr << "Error: decoding requires input and output files\n";
            std::cerr << "Usage: " << argv[0] << " -d <input.agol> <output.wav>\n";
            return 1;
        }
        
        std::string inputFile = argv[2];
        std::string outputFile = argv[3];
        
        try {
            std::ifstream header(inputFile, std::ios::binary);
            if (!header.is_open()) {
                std::cerr << "Error: cannot open input file\n";
                return 1;
            }
            
            char magic[4];
            header.read(magic, 4);
            if (std::string(magic, 4) != "AGOL") {
                std::cerr << "Error: not a valid AGOL audio file\n";
                return 1;
            }
            
            int channels, sampleRate;
            int64_t frames;
            
            header.read(reinterpret_cast<char*>(&channels), sizeof(int));
            header.read(reinterpret_cast<char*>(&sampleRate), sizeof(int));
            header.read(reinterpret_cast<char*>(&frames), sizeof(int64_t));
            
            int predType, stereoType, adaptive, negMode;
            unsigned int m;
            
            header.read(reinterpret_cast<char*>(&predType), sizeof(int));
            header.read(reinterpret_cast<char*>(&stereoType), sizeof(int));
            header.read(reinterpret_cast<char*>(&adaptive), sizeof(int));
            header.read(reinterpret_cast<char*>(&m), sizeof(unsigned int));
            header.read(reinterpret_cast<char*>(&negMode), sizeof(int));
            
            PredictorType predictor = static_cast<PredictorType>(predType);
            StereoMode stereoMode = static_cast<StereoMode>(stereoType);
            GolombCoding::NegativeMode negativeMode = static_cast<GolombCoding::NegativeMode>(negMode);

            header.close();
            
            std::cout << "Decoding: " << channels << " channel(s), "
                      << sampleRate << " Hz, " << frames << " frames\n";
            
            std::fstream fs(inputFile, std::ios::binary | std::ios::in);
            if (!fs.is_open()) {
                std::cerr << "Error: cannot open input file\n";
                return 1;
            }
            
            // Skip header
            fs.seekg(4 + sizeof(int) * 6 + sizeof(int64_t) + sizeof(unsigned int));
            
            BitStream bs(fs, true);
            
            std::vector<int16_t> samples;
            
            if (channels == 1) {
                std::cout << "Decoding mono channel...\n";
                samples = decodeChannel(bs, frames, predictor, negativeMode);
            } else if (channels == 2) {
                std::cout << "Decoding stereo channels...\n";
                std::vector<int16_t> ch1 = decodeChannel(bs, frames, predictor, negativeMode);
                std::vector<int16_t> ch2 = decodeChannel(bs, frames, predictor, negativeMode);
                
                if (stereoMode == StereoMode::MID_SIDE) {
                    std::cout << "Converting from mid-side...\n";
                    std::vector<int16_t> left, right;
                    convertFromMidSide(ch1, ch2, left, right);
                    
                    samples.reserve(frames * 2);
                    for (size_t i = 0; i < static_cast<size_t>(frames); i++) {
                        samples.push_back(left[i]);
                        samples.push_back(right[i]);
                    }
                } else {
                    samples.reserve(frames * 2);
                    for (size_t i = 0; i < static_cast<size_t>(frames); i++) {
                        samples.push_back(ch1[i]);
                        samples.push_back(ch2[i]);
                    }
                }
            }
            
            bs.close();
            
            SndfileHandle sfhOut(outputFile, SFM_WRITE, SF_FORMAT_WAV | SF_FORMAT_PCM_16,
                                 channels, sampleRate);
            
            if (sfhOut.error()) {
                std::cerr << "Error: cannot create output WAV file\n";
                std::cerr << sfhOut.strError() << "\n";
                return 1;
            }
            
            sfhOut.writef(samples.data(), frames);
            
            std::cout << "Decoding successful!\n";
            return 0;
            
        } catch (const std::exception& e) {
            std::cerr << "Error during decoding: " << e.what() << "\n";
            return 1;
        }
    }
    
    PredictorType predictor = PredictorType::ORDER_2;
    StereoMode stereoMode = StereoMode::MID_SIDE;
    bool adaptiveM = true;
    unsigned int fixedM = 16;
    GolombCoding::NegativeMode negativeMode = GolombCoding::INTERLEAVED;
    
    std::string inputFile, outputFile;
    
    int i = 2;
    while (i < argc) {
        if (std::strcmp(argv[i], "-p") == 0) {
            if (i + 1 >= argc) {
                std::cerr << "Error: -p requires a value\n";
                return 1;
            }
            int pred = std::atoi(argv[++i]);
            switch (pred) {
                case 0: predictor = PredictorType::ORDER_1; break;
                case 1: predictor = PredictorType::ORDER_2; break;
                case 2: predictor = PredictorType::ORDER_3; break;
                default:
                    std::cerr << "Error: invalid predictor type (must be 0-2)\n";
                    return 1;
            }
        } else if (std::strcmp(argv[i], "-s") == 0) {
            if (i + 1 >= argc) {
                std::cerr << "Error: -s requires a value\n";
                return 1;
            }
            int mode = std::atoi(argv[++i]);
            switch (mode) {
                case 0: stereoMode = StereoMode::INDEPENDENT; break;
                case 1: stereoMode = StereoMode::MID_SIDE; break;
                default:
                    std::cerr << "Error: invalid stereo mode (must be 0 or 1)\n";
                    return 1;
            }
        } else if (std::strcmp(argv[i], "-m") == 0) {
            if (i + 1 >= argc) {
                std::cerr << "Error: -m requires a value\n";
                return 1;
            }
            fixedM = std::atoi(argv[++i]);
            if (fixedM < 1) {
                std::cerr << "Error: m must be at least 1\n";
                return 1;
            }
            adaptiveM = false;
        } else if (std::strcmp(argv[i], "-n") == 0) {
            if (i + 1 >= argc) {
                std::cerr << "Error: -n requires a value\n";
                return 1;
            }
            int mode = std::atoi(argv[++i]);
            switch (mode) {
                case 0: negativeMode = GolombCoding::INTERLEAVED; break;
                case 1: negativeMode = GolombCoding::SIGN_MAGNITUDE; break;
                default:
                    std::cerr << "Error: invalid negative mode (must be 0 or 1)\n";
                    return 1;
            }
        } else {
            if (inputFile.empty()) {
                inputFile = argv[i];
            } else if (outputFile.empty()) {
                outputFile = argv[i];
            } else {
                std::cerr << "Error: unexpected argument: " << argv[i] << "\n";
                return 1;
            }
        }
        i++;
    }
    
    if (inputFile.empty() || outputFile.empty()) {
        std::cerr << "Error: both input and output files must be specified\n";
        printUsage(argv[0]);
        return 1;
    }
    
    std::cout << "Audio Codec Configuration:\n";
    std::cout << "  Predictor: ";
    switch (predictor) {
        case PredictorType::ORDER_1: std::cout << "Order-1\n"; break;
        case PredictorType::ORDER_2: std::cout << "Order-2\n"; break;
        case PredictorType::ORDER_3: std::cout << "Order-3\n"; break;
    }
    std::cout << "  Stereo mode: ";
    switch (stereoMode) {
        case StereoMode::INDEPENDENT: std::cout << "Independent\n"; break;
        case StereoMode::MID_SIDE: std::cout << "Mid-Side\n"; break;
    }
    std::cout << "  Golomb parameter: ";
    if (adaptiveM) {
        std::cout << "Adaptive\n";
    } else {
        std::cout << "Fixed (m=" << fixedM << ")\n";
    }
    std::cout << "  Negative mode: ";
    switch (negativeMode) {
        case GolombCoding::INTERLEAVED: std::cout << "Interleaved\n"; break;
        case GolombCoding::SIGN_MAGNITUDE: std::cout << "Sign-Magnitude\n"; break;
    }
    std::cout << "\nEncoding " << inputFile << " to " << outputFile << "...\n\n";
    
    try {
        SndfileHandle sfhIn(inputFile);
        if (sfhIn.error()) {
            std::cerr << "Error: cannot open input file '" << inputFile << "'\n";
            std::cerr << sfhIn.strError() << "\n";
            return 1;
        }
        
        int channels = sfhIn.channels();
        int sampleRate = sfhIn.samplerate();
        int64_t frames = sfhIn.frames();
        
        std::cout << "Input: " << channels << " channel(s), " 
                  << sampleRate << " Hz, " 
                  << frames << " frames\n";
        
        std::ofstream headerFile(outputFile, std::ios::binary | std::ios::trunc);
        if (!headerFile.is_open()) {
            std::cerr << "Error: cannot create output file\n";
            return 1;
        }
        
        headerFile.write("AGOL", 4);
        
        headerFile.write(reinterpret_cast<const char*>(&channels), sizeof(int));
        headerFile.write(reinterpret_cast<const char*>(&sampleRate), sizeof(int));
        headerFile.write(reinterpret_cast<const char*>(&frames), sizeof(int64_t));
        
        int predType = static_cast<int>(predictor);
        int stereoType = static_cast<int>(stereoMode);
        int adaptive = adaptiveM ? 1 : 0;
        int negMode = static_cast<int>(negativeMode);
        
        headerFile.write(reinterpret_cast<const char*>(&predType), sizeof(int));
        headerFile.write(reinterpret_cast<const char*>(&stereoType), sizeof(int));
        headerFile.write(reinterpret_cast<const char*>(&adaptive), sizeof(int));
        headerFile.write(reinterpret_cast<const char*>(&fixedM), sizeof(unsigned int));
        headerFile.write(reinterpret_cast<const char*>(&negMode), sizeof(int));
        
        headerFile.close();
        
        std::vector<int16_t> samples(frames * channels);
        sfhIn.readf(samples.data(), frames);
        
        std::fstream fs(outputFile, std::ios::binary | std::ios::in | std::ios::out | std::ios::ate);
        BitStream bs(fs, false);
        
        if (channels == 1) {
            std::cout << "Encoding mono channel...\n";
            encodeChannel(samples, bs, predictor, adaptiveM, fixedM, negativeMode);
        } else if (channels == 2) {
            std::vector<int16_t> left, right;
            left.reserve(frames);
            right.reserve(frames);
            
            for (int64_t i = 0; i < frames; i++) {
                left.push_back(samples[i * 2]);
                right.push_back(samples[i * 2 + 1]);
            }
            
            if (stereoMode == StereoMode::MID_SIDE) {
                std::cout << "Encoding with mid-side stereo...\n";
                std::vector<int16_t> mid, side;
                convertToMidSide(left, right, mid, side);
                
                encodeChannel(mid, bs, predictor, adaptiveM, fixedM, negativeMode);
                encodeChannel(side, bs, predictor, adaptiveM, fixedM, negativeMode);
            } else {
                std::cout << "Encoding left and right channels independently...\n";
                encodeChannel(left, bs, predictor, adaptiveM, fixedM, negativeMode);
                encodeChannel(right, bs, predictor, adaptiveM, fixedM, negativeMode);
            }
        } else {
            std::cerr << "Error: only mono and stereo audio supported\n";
            bs.close();
            return 1;
        }
        
        bs.close();
        
        size_t originalSize = frames * channels * sizeof(int16_t);
        
        std::ifstream compressed(outputFile, std::ios::binary | std::ios::ate);
        size_t compressedSize = compressed.tellg();
        compressed.close();
        
        double compressionRatio = static_cast<double>(originalSize) / compressedSize;
        double bitsPerSample = (static_cast<double>(compressedSize) * 8.0) / (frames * channels);
        
        std::cout << "\nCompression statistics:\n";
        std::cout << "  Original size: " << originalSize << " bytes\n";
        std::cout << "  Compressed size: " << compressedSize << " bytes\n";
        std::cout << "  Compression ratio: " << compressionRatio << ":1\n";
        std::cout << "  Bits per sample: " << bitsPerSample << "\n";
        std::cout << "  Compression achieved: " 
                  << (100.0 * (1.0 - 1.0/compressionRatio)) << "%\n";
        
        std::cout << "\nEncoding successful!\n";
        return 0;
        
    } catch (const std::exception& e) {
        std::cerr << "Error during encoding: " << e.what() << "\n";
        return 1;
    }
}
