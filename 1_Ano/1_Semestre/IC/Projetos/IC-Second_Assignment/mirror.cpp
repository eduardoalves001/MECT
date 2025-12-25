#include <iostream>
#include <fstream>
#include <string>
#include <vector>

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
    outfile << "# Created by C++ mirror program\n";
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


int main(int argc, char** argv) {
    
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0] << " <-h|-v> <input_image.ppm> <output_image.ppm>" << std::endl;
        std::cerr << "Example: " << argv[0] << " -h input.ppm output.ppm" << std::endl;
        std::cerr << "  -h : Horizontal mirror" << std::endl;
        std::cerr << "  -v : Vertical mirror" << std::endl;
        return 1;
    }

    std::string mode = argv[1];
    std::string input_filename = argv[2];
    std::string output_filename = argv[3];

    
    bool horizontal = false;
    bool vertical = false;
    
    if (mode == "-h") {
        horizontal = true;
    } else if (mode == "-v") {
        vertical = true;
    } else {
        std::cerr << "ERROR: Invalid mode. Use -h for horizontal or -v for vertical." << std::endl;
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

    std::vector<std::vector<Pixel>> original_image(height, std::vector<Pixel>(width));

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
                
                original_image[y][x].r = r;
                original_image[y][x].g = g;
                original_image[y][x].b = b;
            }
        }
    } else {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                infile >> original_image[y][x].r 
                       >> original_image[y][x].g 
                       >> original_image[y][x].b;
                if (infile.fail()) {
                    std::cerr << "ERROR: Failed to read pixel data." << std::endl;
                    return 1;
                }
            }
        }
    }
    infile.close();
    std::cout << "Image loaded successfully." << std::endl;

    std::vector<std::vector<Pixel>> mirrored_image(height, std::vector<Pixel>(width));

    if (horizontal) {
        std::cout << "Creating horizontal mirror..." << std::endl;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                mirrored_image[y][x] = original_image[y][width - 1 - x];
            }
        }
    } else if (vertical) {
        std::cout << "Creating vertical mirror..." << std::endl;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                mirrored_image[y][x] = original_image[height - 1 - y][x];
            }
        }
    }

    write_ppm(output_filename, mirrored_image, max_color_val);

    std::cout << "Mirror operation complete." << std::endl;
    return 0;
}