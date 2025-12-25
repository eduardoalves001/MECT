#ifndef GOLOMB_H
#define GOLOMB_H

#include <vector>
#include <string>
#include <cmath>
#include <stdexcept>

class GolombCoding {
public:
    enum NegativeMode {
        SIGN_MAGNITUDE,
        INTERLEAVED
    };

private:
    unsigned int m;
    unsigned int b;
    unsigned int cutoff;
    NegativeMode mode;

    void calculateParameters() {
        if (m == 0) {
            throw std::invalid_argument("Golomb parameter m must be positive");
        }
        b = static_cast<unsigned int>(std::floor(std::log2(m))); // floor(log2(m))
        cutoff = (1 << (b + 1)) - m;  // 2^(b+1) - m
    }

    unsigned int mapToUnsigned(int value) const {
        if (mode == SIGN_MAGNITUDE) {
            return static_cast<unsigned int>(std::abs(value));
        } else {
            if (value >= 0) {
                return 2 * value;
            } else {
                return 2 * (-value) - 1;
            }
        }
    }

    int mapToSigned(unsigned int value, bool isNegative = false) const {
        if (mode == SIGN_MAGNITUDE) {
            return isNegative ? -static_cast<int>(value) : static_cast<int>(value);
        } else {
            if (value % 2 == 0) {
                return value / 2;
            } else {
                return -(static_cast<int>(value + 1) / 2);
            }
        }
    }

    std::vector<bool> encodeUnsigned(unsigned int n) const {
        std::vector<bool> bits;
        
        unsigned int q = n / m;
        unsigned int r = n % m;
        
        for (unsigned int i = 0; i < q; i++) {
            bits.push_back(false);
        }
        bits.push_back(true);
        
        if (r < cutoff) {
            for (int i = b - 1; i >= 0; i--) {
                bits.push_back((r >> i) & 1);
            }
        } else {
            unsigned int adjusted = r + cutoff;
            for (int i = b; i >= 0; i--) {
                bits.push_back((adjusted >> i) & 1);
            }
        }
        
        return bits;
    }

    std::pair<unsigned int, size_t> decodeUnsigned(const std::vector<bool>& bits, size_t start) const {
        if (start >= bits.size()) {
            throw std::invalid_argument("Start position out of bounds");
        }
        
        unsigned int q = 0;
        size_t pos = start;
        while (pos < bits.size() && !bits[pos]) {
            q++;
            pos++;
        }
        
        if (pos >= bits.size()) {
            throw std::invalid_argument("Incomplete unary code");
        }
        pos++;
        
        if (pos + b > bits.size()) {
            throw std::invalid_argument("Incomplete remainder code");
        }
        
        unsigned int r = 0;
        for (unsigned int i = 0; i < b; i++) {
            r = (r << 1) | bits[pos++];
        }
        
        if (r < cutoff) {
            unsigned int n = q * m + r;
            return {n, pos - start};
        } else {
            if (pos >= bits.size()) {
                throw std::invalid_argument("Incomplete remainder code");
            }
            r = (r << 1) | bits[pos++];
            unsigned int n = q * m + r - cutoff;
            return {n, pos - start};
        }
    }

public:
    GolombCoding(unsigned int m_param, NegativeMode neg_mode = INTERLEAVED)
        : m(m_param), mode(neg_mode) {
        calculateParameters();
    }

    std::vector<bool> encode(int value) const {
        std::vector<bool> bits;
        
        if (mode == SIGN_MAGNITUDE) {
            bits.push_back(value < 0);
            unsigned int absValue = mapToUnsigned(value);
            std::vector<bool> encoded = encodeUnsigned(absValue);
            bits.insert(bits.end(), encoded.begin(), encoded.end());
        } else {
            unsigned int mapped = mapToUnsigned(value);
            bits = encodeUnsigned(mapped);
        }
        
        return bits;
    }

    std::pair<int, size_t> decode(const std::vector<bool>& bits, size_t start = 0) const {
        if (start >= bits.size()) {
            throw std::invalid_argument("Start position out of bounds");
        }
        
        size_t bitsUsed = 0;
        int result;
        
        if (mode == SIGN_MAGNITUDE) {
            bool isNegative = bits[start];
            bitsUsed++;
            
            auto [magnitude, magnitudeBits] = decodeUnsigned(bits, start + 1);
            bitsUsed += magnitudeBits;
            
            result = mapToSigned(magnitude, isNegative);
        } else {
            auto [mapped, usedBits] = decodeUnsigned(bits, start);
            bitsUsed = usedBits;
            result = mapToSigned(mapped);
        }
        
        return {result, bitsUsed};
    }

    static std::string bitsToString(const std::vector<bool>& bits) {
        std::string result;
        for (bool bit : bits) {
            result += bit ? '1' : '0';
        }
        return result;
    }

    unsigned int getM() const { return m; }
    NegativeMode getMode() const { return mode; }
    
    void setM(unsigned int new_m) {
        m = new_m;
        calculateParameters();
    }
};

#endif