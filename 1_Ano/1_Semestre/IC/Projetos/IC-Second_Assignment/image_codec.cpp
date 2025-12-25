#include "Golomb.h"
#include "bit_stream/src/bit_stream.h"
#include <opencv2/opencv.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <cstring>
#include <vector>
#include <cmath>
#include <algorithm>
#include <numeric>

enum class PredictorType {
    LEFT = 0,           // left
    TOP = 1,            // top
    TOP_LEFT = 2,       // topLeft
    AVG = 3,            // (left + top) / 2
    PAETH = 4,          // (left + top - topLeft)
    A_PLUS_HALF_B_MINUS_C = 5, // left + (top - topLeft) / 2
    B_PLUS_HALF_A_MINUS_C = 6  // top + (left - topLeft) / 2
};

int predict(const cv::Mat& img, int row, int col, PredictorType predictor) {
    int left = (col > 0) ? img.at<uint8_t>(row, col - 1) : 128;
    int top = (row > 0) ? img.at<uint8_t>(row - 1, col) : 128;
    int topLeft = (row > 0 && col > 0) ? img.at<uint8_t>(row - 1, col - 1) : 128;
    
    switch (predictor) {
        case PredictorType::LEFT:
            return left;
            
        case PredictorType::TOP:
            return top;
            
        case PredictorType::AVG:
            return (left + top) / 2;
            
        case PredictorType::PAETH: {
            int p = left + top - topLeft;
            int pa = std::abs(p - left);
            int pb = std::abs(p - top);
            int pc = std::abs(p - topLeft);
            
            if (pa <= pb && pa <= pc) return left;
            else if (pb <= pc) return top;
            else return topLeft;
        }
        
        case PredictorType::A_PLUS_HALF_B_MINUS_C: {
            return left + (top - topLeft) / 2;
        }

        case PredictorType::B_PLUS_HALF_A_MINUS_C: {
            return top + (left - topLeft) / 2;
        }
        
        default:
            return 128;
    }
}

unsigned int estimateGolombParameter(const std::vector<int>& residuals) {
    if (residuals.empty()) return 1;
    
    double mean = std::accumulate(residuals.begin(), residuals.end(), 0.0,
                                  [](double sum, int r) { return sum + std::abs(r); }) 
                  / residuals.size();
    
    if (mean < 0.5) return 1;
    
    double p = mean / (mean + 1.0);
    unsigned int m = static_cast<unsigned int>(std::ceil(-1.0 / std::log2(p)));
    return std::clamp(m, 1u, 65535u);
}

void printUsage(const char* progName) {
    std::cout << "Image Codec - Lossless grayscale image compression using Golomb coding\n\n"
              << "Usage:\n"
              << "  Encoding: " << progName << " -e [options] <input.pgm> <output.gimg>\n"
              << "  Decoding: " << progName << " -d <input.gimg> <output.pgm>\n\n"
              << "Options:\n"
                 << "  -p <0-6>  Predictor:\n"
                 << "            0=Left, 1=Top, 2=Top-Left\n"
                 << "            3=Average, 4=Paeth [default]\n"
                 << "            5=a+(b-c)/2, 6=b+(a-c)/2\n"
              << "  -m <int>  Fixed Golomb m (default: adaptive)\n"
              << "  -n <0-1>  Negative mode: 0=Interleaved [default], 1=Sign-Magnitude\n\n"
              << "Examples:\n"
              << "  " << progName << " -e input.pgm output.gimg\n"
              << "  " << progName << " -e -n 1 input.pgm output.gimg  # use sign-magnitude\n"
              << "  " << progName << " -d output.gimg decoded.pgm\n";
}

bool encodeImage(const std::string& inputFile, const std::string& outputFile,
                 PredictorType predictor, bool adaptiveM, unsigned int fixedM,
                 GolombCoding::NegativeMode negativeMode) {
    cv::Mat img = cv::imread(inputFile, cv::IMREAD_GRAYSCALE);
    if (img.empty()) {
        std::cerr << "Error: cannot read image file '" << inputFile << "'\n";
        return false;
    }
    
    std::cout << "Input: " << img.cols << "x" << img.rows << " pixels, grayscale\n";
    
    std::ofstream headerFile(outputFile, std::ios::binary | std::ios::trunc);
    if (!headerFile.is_open()) {
        std::cerr << "Error: cannot create output file\n";
        return false;
    }
    
    headerFile.write("GIMG", 4);
    
    int width = img.cols;
    int height = img.rows;
    int predType = static_cast<int>(predictor);
    int adaptive = adaptiveM ? 1 : 0;
    int negMode = static_cast<int>(negativeMode);
    
    headerFile.write(reinterpret_cast<const char*>(&width), sizeof(int));
    headerFile.write(reinterpret_cast<const char*>(&height), sizeof(int));
    headerFile.write(reinterpret_cast<const char*>(&predType), sizeof(int));
    headerFile.write(reinterpret_cast<const char*>(&adaptive), sizeof(int));
    headerFile.write(reinterpret_cast<const char*>(&fixedM), sizeof(unsigned int));
    headerFile.write(reinterpret_cast<const char*>(&negMode), sizeof(int));
    
    headerFile.close();
    
    std::fstream fs(outputFile, std::ios::binary | std::ios::in | std::ios::out | std::ios::ate);
    BitStream bs(fs, false);
    
    const size_t BLOCK_SIZE = 256;
    size_t totalPixels = img.rows * img.cols;
    size_t pixelCount = 0;
    
    std::vector<int> residuals;
    residuals.reserve(BLOCK_SIZE);
    
    for (int row = 0; row < img.rows; row++) {
        for (int col = 0; col < img.cols; col++) {
            uint8_t pixel = img.at<uint8_t>(row, col);
            int prediction = predict(img, row, col, predictor);
            int residual = static_cast<int>(pixel) - prediction;
            
            residuals.push_back(residual);
            pixelCount++;
            
            if (residuals.size() >= BLOCK_SIZE || pixelCount >= totalPixels) {
                unsigned int m = adaptiveM ? estimateGolombParameter(residuals) : fixedM;
                
                bs.write_n_bits(m, 16);
                
                GolombCoding golomb(m, negativeMode);
                for (int res : residuals) {
                    std::vector<bool> encoded = golomb.encode(res);
                    for (bool bit : encoded) {
                        bs.write_bit(bit ? 1 : 0);
                    }
                }
                
                residuals.clear();
            }
        }
    }
    
    bs.close();
    
    size_t originalSize = img.rows * img.cols;
    
    std::ifstream compressed(outputFile, std::ios::binary | std::ios::ate);
    size_t compressedSize = compressed.tellg();
    compressed.close();
    
    double compressionRatio = static_cast<double>(originalSize) / compressedSize;
    double bitsPerPixel = (static_cast<double>(compressedSize) * 8.0) / (img.rows * img.cols);
    
    std::cout << "\nCompression statistics:\n";
    std::cout << "  Original size: " << originalSize << " bytes\n";
    std::cout << "  Compressed size: " << compressedSize << " bytes\n";
    std::cout << "  Compression ratio: " << compressionRatio << ":1\n";
    std::cout << "  Bits per pixel: " << bitsPerPixel << "\n";
    std::cout << "  Compression achieved: " 
              << (100.0 * (1.0 - 1.0/compressionRatio)) << "%\n";
    
    return true;
}

bool decodeImage(const std::string& inputFile, const std::string& outputFile) {
    std::ifstream header(inputFile, std::ios::binary);
    if (!header.is_open()) {
        std::cerr << "Error: cannot open input file\n";
        return false;
    }
    
    char magic[4];
    header.read(magic, 4);
    if (std::string(magic, 4) != "GIMG") {
        std::cerr << "Error: not a valid GIMG image file\n";
        return false;
    }
    
    int width, height, predType, adaptive, negMode;
    unsigned int m;
    
    header.read(reinterpret_cast<char*>(&width), sizeof(int));
    header.read(reinterpret_cast<char*>(&height), sizeof(int));
    header.read(reinterpret_cast<char*>(&predType), sizeof(int));
    header.read(reinterpret_cast<char*>(&adaptive), sizeof(int));
    header.read(reinterpret_cast<char*>(&m), sizeof(unsigned int));
    header.read(reinterpret_cast<char*>(&negMode), sizeof(int));
    
    PredictorType predictor = static_cast<PredictorType>(predType);
    GolombCoding::NegativeMode negativeMode = static_cast<GolombCoding::NegativeMode>(negMode);
    
    header.close();
    
    std::cout << "Decoding: " << width << "x" << height << " pixels\n";
    
    std::fstream fs(inputFile, std::ios::binary | std::ios::in);
    if (!fs.is_open()) {
        std::cerr << "Error: cannot open input file\n";
        return false;
    }
    
    fs.seekg(4 + sizeof(int) * 5 + sizeof(unsigned int));
    
    BitStream bs(fs, true);
    
    cv::Mat img(height, width, CV_8UC1);
    
    const size_t BLOCK_SIZE = 256;
    size_t pixelCount = 0;
    
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            if (pixelCount % BLOCK_SIZE == 0) {
                m = static_cast<unsigned int>(bs.read_n_bits(16));
            }
            
            GolombCoding golomb(m, negativeMode);
            std::vector<bool> bits;
            
            char bit;
            do {
                bit = bs.read_bit();
                bits.push_back(bit != 0);
            } while (bit == 0);
            
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
            
            int prediction = predict(img, row, col, predictor);
            int pixel = std::clamp(prediction + residual, 0, 255);
            
            img.at<uint8_t>(row, col) = static_cast<uint8_t>(pixel);
            pixelCount++;
        }
    }
    
    bs.close();
    
    if (!cv::imwrite(outputFile, img)) {
        std::cerr << "Error: cannot write output image\n";
        return false;
    }
    
    std::cout << "Decoding successful!\n";
    return true;
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
            std::cerr << "Usage: " << argv[0] << " -d <input.gimg> <output.pgm>\n";
            return 1;
        }
        
        std::string inputFile = argv[2];
        std::string outputFile = argv[3];
        
        if (decodeImage(inputFile, outputFile)) {
            std::cout << "Success!\n";
            return 0;
        } else {
            std::cerr << "Decoding failed!\n";
            return 1;
        }
    }
    
    PredictorType predictor = PredictorType::PAETH;
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
            if (pred < 0 || pred > 6) {
                std::cerr << "Error: invalid predictor type (must be 0-6)\n";
                return 1;
            }
            predictor = static_cast<PredictorType>(pred);
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
            if (mode == 0) {
                negativeMode = GolombCoding::INTERLEAVED;
            } else if (mode == 1) {
                negativeMode = GolombCoding::SIGN_MAGNITUDE;
            } else {
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
    
    std::cout << "Image Codec Configuration:\n";
    std::cout << "  Predictor: ";
    switch (predictor) {
        case PredictorType::LEFT: std::cout << "Left\n"; break;
        case PredictorType::TOP: std::cout << "Top\n"; break;
        case PredictorType::TOP_LEFT: std::cout << "Top-Left\n"; break;
        case PredictorType::AVG: std::cout << "Average\n"; break;
        case PredictorType::PAETH: std::cout << "Paeth (PNG)\n"; break;
        case PredictorType::A_PLUS_HALF_B_MINUS_C: std::cout << "a+(b-c)/2\n"; break;
        case PredictorType::B_PLUS_HALF_A_MINUS_C: std::cout << "b+(a-c)/2\n"; break;
    }
    std::cout << "  Golomb parameter: ";
    if (adaptiveM) {
        std::cout << "Adaptive\n";
    } else {
        std::cout << "Fixed (m=" << fixedM << ")\n";
    }
    std::cout << "  Negative mode: " 
              << (negativeMode == GolombCoding::INTERLEAVED ? "Interleaved" : "Sign-Magnitude") 
              << "\n";
    std::cout << "\nEncoding " << inputFile << " to " << outputFile << "...\n\n";
    
    if (encodeImage(inputFile, outputFile, predictor, adaptiveM, fixedM, negativeMode)) {
        std::cout << "\nEncoding successful!\n";
        return 0;
    } else {
        std::cerr << "\nEncoding failed!\n";
        return 1;
    }
}