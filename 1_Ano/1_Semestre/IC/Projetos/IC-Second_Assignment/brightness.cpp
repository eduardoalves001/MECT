#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <sstream>
#include <algorithm>

struct Pixel {
    int r, g, b;
};

/**
 * @brief
 * @param filename
 * @param image
 * @param max_val
 * @return
 */
bool write_ppm(const std::string& filename,
               const std::vector<std::vector<Pixel>>& image,
               int max_val) 
{
    std::ofstream outfile(filename);
    if (!outfile.is_open()) {
        std::cerr << "ERROR: Could not create output file '" << filename << "'" << std::endl;
        return false;
    }

    int height = image.size();
    if (height == 0) return false;
    int width = image[0].size();
    if (width == 0) return false;

    outfile << "P3\n";
    outfile << "# Created by C++ brightness program\n";
    outfile << width << " " << height << "\n";
    outfile << max_val << "\n";

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            outfile << image[y][x].r << " "
                    << image[y][x].g << " "
                    << image[y][x].b << "  ";
        }
        outfile << "\n";
    }

    outfile.close();
    std::cout << "Successfully saved '" << filename << "'" << std::endl;
    return true;
}

/**
 * @brief
 */
int clamp(int value, int max_val) {
    return std::max(0, std::min(value, max_val));
}

int main(int argc, char** argv) {
    
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0] << " <input_image.ppm> <output_image.ppm> <adjustment>" << std::endl;
        std::cerr << "Example: " << argv[0] << " input.ppm output.ppm 50" << std::endl;
        std::cerr << "Adjustment: positive value for brighter, negative for darker" << std::endl;
        return 1;
    }

    std::string input_filename = argv[1];
    std::string output_filename = argv[2];
    int adjustment;

    try {
        adjustment = std::stoi(argv[3]);
    } catch (const std::exception& e) {
        std::cerr << "ERROR: Invalid adjustment value." << std::endl;
        return 1;
    }

    std::ifstream infile(input_filename, std::ios::binary);
    if (!infile.is_open()) {
        std::cerr << "ERROR: Could not open '" << input_filename << "'" << std::endl;
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

    std::cout << "Loading image: " << width << "x" << height << " (" << magic_number << ")" << std::endl;

    std::vector<std::vector<Pixel>> adjusted_image(height, std::vector<Pixel>(width));
    
    if (isBinary) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                unsigned char r, g, b;
                infile.read(reinterpret_cast<char*>(&r), 1);
                infile.read(reinterpret_cast<char*>(&g), 1);
                infile.read(reinterpret_cast<char*>(&b), 1);
                
                if (infile.fail()) {
                    std::cerr << "ERROR: Failed to read pixel data." << std::endl;
                    return 1;
                }
                
                Pixel& p_out = adjusted_image[y][x];
                p_out.r = clamp(r + adjustment, max_color_val);
                p_out.g = clamp(g + adjustment, max_color_val);
                p_out.b = clamp(b + adjustment, max_color_val);
            }
        }
    } else {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                Pixel p_in;
                infile >> p_in.r >> p_in.g >> p_in.b;
                if (infile.fail()) {
                    std::cerr << "ERROR: Failed to read pixel data." << std::endl;
                    return 1;
                }
                
                Pixel& p_out = adjusted_image[y][x];
                p_out.r = clamp(p_in.r + adjustment, max_color_val);
                p_out.g = clamp(p_in.g + adjustment, max_color_val);
                p_out.b = clamp(p_in.b + adjustment, max_color_val);
            }
        }
    }
    infile.close();
    std::cout << "Image processed successfully." << std::endl;


    if (!write_ppm(output_filename, adjusted_image, max_color_val)) {
        std::cerr << "ERROR: Failed to save adjusted image." << std::endl;
        return 1;
    }

    std::cout << "Brightness adjustment complete." << std::endl;
    return 0;
}