
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <sstream>

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
    outfile << "# Created by C++ rotate program\n";
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
        std::cerr << "Usage: " << argv[0] << " <input_image.ppm> <output_image.ppm> <angle>" << std::endl;
        std::cerr << "Example: " << argv[0] << " input.ppm output.ppm 90" << std::endl;
        std::cerr << "Angle must be: 90, 180, or 270" << std::endl;
        return 1;
    }

    std::string input_filename = argv[1];
    std::string output_filename = argv[2];
    int angle;

    try {
        angle = std::stoi(argv[3]);
    } catch (const std::exception& e) {
        std::cerr << "ERROR: Invalid angle. Must be 90, 180, or 270." << std::endl;
        return 1;
    }

    if (angle != 90 && angle != 180 && angle != 270) {
        std::cerr << "ERROR: Invalid angle. Please enter 90, 180, or 270." << std::endl;
        return 1;
    }

    std::ifstream infile(input_filename, std::ios::binary);
    if (!infile.is_open()) {
        std::cerr << "ERROR: Could not open '" << input_filename << "'" << std::endl;
        return 1;
    }

    std::string magic_number;
    int orig_width, orig_height, max_color_val;

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

    infile >> orig_width >> orig_height >> max_color_val;
    if (!infile.good()) {
        std::cerr << "ERROR: Invalid PPM header." << std::endl;
        return 1;
    }

    if (isBinary) {
        infile.ignore(1);
    }

    std::cout << "Loading image: " << orig_width << "x" << orig_height << " (" << magic_number << ")" << std::endl;

    std::vector<std::vector<Pixel>> original_image(orig_height, std::vector<Pixel>(orig_width));

    if (isBinary) {
        for (int y = 0; y < orig_height; ++y) {
            for (int x = 0; x < orig_width; ++x) {
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
        for (int y = 0; y < orig_height; ++y) {
            for (int x = 0; x < orig_width; ++x) {
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

    int new_width = (angle == 180) ? orig_width : orig_height;
    int new_height = (angle == 180) ? orig_height : orig_width;

    std::cout << "Rotating image " << angle << " degrees..." << std::endl;
    std::vector<std::vector<Pixel>> rotated_image(new_height, std::vector<Pixel>(new_width));

    for (int y_new = 0; y_new < new_height; ++y_new) {
        for (int x_new = 0; x_new < new_width; ++x_new) {
            
            switch (angle) {
                case 90:
                    rotated_image[y_new][x_new] = original_image[orig_height - 1 - x_new][y_new];
                    break;
                
                case 180:
                    rotated_image[y_new][x_new] = original_image[orig_height - 1 - y_new][orig_width - 1 - x_new];
                    break;
                
                case 270:
                    rotated_image[y_new][x_new] = original_image[x_new][orig_width - 1 - y_new];
                    break;
            }
        }
    }

    if (!write_ppm(output_filename, rotated_image, max_color_val)) {
        std::cerr << "ERROR: Failed to save rotated image." << std::endl;
        return 1;
    }

    std::cout << "Rotation complete." << std::endl;
    return 0;
}