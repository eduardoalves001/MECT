#include <iostream>
#include <fstream>
#include <string>
#include <vector>

int main(int argc, char** argv) {
    
    if (argc != 3) {
        std::cerr << "Usage: " << argv[0] << " <input_image.ppm> <output_image.ppm>" << std::endl;
        std::cerr << "Example: " << argv[0] << " input.ppm output_negative.ppm" << std::endl;
        return 1;
    }

    std::string inputPath = argv[1];
    std::string outputPath = argv[2];
    
    std::ifstream infile(inputPath);
    if (!infile.is_open()) {
        std::cerr << "ERROR: Could not open " << inputPath << std::endl;
        return 1;
    }

    std::ofstream outfile(outputPath);
    if (!outfile.is_open()) {
        std::cerr << "ERROR: Could not create " << outputPath << std::endl;
        return 1;
    }

    std::string magic_number;
    int width, height, max_color_val;

    infile >> magic_number;
    if (magic_number != "P3" && magic_number != "P6") {
        std::cerr << "ERROR: Input is not a valid PPM file (must be P3 or P6)." << std::endl;
        return 1;
    }
    
    bool isBinary = (magic_number == "P6");
    
    while (infile.peek() == '\n' || infile.peek() == ' ') infile.ignore();
    while (infile.peek() == '#') {
        std::string comment;
        std::getline(infile, comment);
        while (infile.peek() == '\n' || infile.peek() == ' ') infile.ignore();
    }

    infile >> width >> height >> max_color_val;

    if (!infile.good()) {
        std::cerr << "ERROR: Invalid PPM header." << std::endl;
        return 1;
    }

    if (isBinary) {
        infile.ignore(1);
    }

    std::cout << "Image loaded: " << width << "x" << height << " (" << magic_number << ")" << std::endl;
    std::cout << "Max color value: " << max_color_val << std::endl;

    outfile << "P6\n";
    outfile << "# Created by C++ negative program\n";
    outfile << width << " " << height << "\n";
    outfile << max_color_val << "\n";

    int pixel_count = width * height;

    if (isBinary) {
        for (int i = 0; i < pixel_count; ++i) {
            unsigned char r, g, b;
            
            infile.read(reinterpret_cast<char*>(&r), 1);
            infile.read(reinterpret_cast<char*>(&g), 1);
            infile.read(reinterpret_cast<char*>(&b), 1);

            if (infile.fail()) {
                std::cerr << "ERROR: Failed to read pixel data at pixel " << i << std::endl;
                return 1;
            }

            unsigned char neg_r = max_color_val - r;
            unsigned char neg_g = max_color_val - g;
            unsigned char neg_b = max_color_val - b;

            outfile.write(reinterpret_cast<char*>(&neg_r), 1);
            outfile.write(reinterpret_cast<char*>(&neg_g), 1);
            outfile.write(reinterpret_cast<char*>(&neg_b), 1);
        }
    } else {
        for (int i = 0; i < pixel_count; ++i) {
            int r, g, b;
            
            infile >> r >> g >> b;

            if (infile.fail()) {
                std::cerr << "ERROR: Failed to read pixel data at pixel " << i << std::endl;
                return 1;
            }

            int neg_r = max_color_val - r;
            int neg_g = max_color_val - g;
            int neg_b = max_color_val - b;

            unsigned char nr = neg_r, ng = neg_g, nb = neg_b;
            outfile.write(reinterpret_cast<char*>(&nr), 1);
            outfile.write(reinterpret_cast<char*>(&ng), 1);
            outfile.write(reinterpret_cast<char*>(&nb), 1);
        }
    }

    infile.close();
    outfile.close();

    std::cout << "Successfully created negative image: " << outputPath << std::endl;

    return 0;
}
