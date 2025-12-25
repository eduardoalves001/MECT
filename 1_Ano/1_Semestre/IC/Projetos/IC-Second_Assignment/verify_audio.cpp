#include <sndfile.hh>
#include <iostream>
#include <vector>

int main(int argc, char* argv[]) {
    if (argc != 3) {
        std::cerr << "Usage: " << argv[0] << " file1.wav file2.wav\n";
        return 1;
    }
    
    SndfileHandle sf1(argv[1]);
    SndfileHandle sf2(argv[2]);
    
    if (sf1.error() || sf2.error()) {
        std::cerr << "Error opening files\n";
        return 1;
    }
    
    if (sf1.frames() != sf2.frames() || sf1.channels() != sf2.channels()) {
        std::cout << "Files have different dimensions\n";
        std::cout << "File 1: " << sf1.frames() << " frames, " << sf1.channels() << " channels\n";
        std::cout << "File 2: " << sf2.frames() << " frames, " << sf2.channels() << " channels\n";
        return 1;
    }
    
    std::vector<int16_t> buf1(sf1.frames() * sf1.channels());
    std::vector<int16_t> buf2(sf2.frames() * sf2.channels());
    
    sf1.readf(buf1.data(), sf1.frames());
    sf2.readf(buf2.data(), sf2.frames());
    
    bool identical = true;
    int differences = 0;
    for (size_t i = 0; i < buf1.size(); i++) {
        if (buf1[i] != buf2[i]) {
            identical = false;
            differences++;
            if (differences <= 5) {
                std::cout << "Diff at sample " << i << ": " << buf1[i] << " vs " << buf2[i] << "\n";
            }
        }
    }
    
    if (identical) {
        std::cout << "✓ Audio samples are IDENTICAL - Lossless compression verified!\n";
        return 0;
    } else {
        std::cout << "✗ Found " << differences << " different samples out of " << buf1.size() << "\n";
        return 1;
    }
}
