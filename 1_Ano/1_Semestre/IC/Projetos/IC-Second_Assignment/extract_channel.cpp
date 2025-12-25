#include <iostream>
#include <string>
#include <opencv2/opencv.hpp>

int main(int argc, char** argv) {
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0] << " <input_image> <output_image> <channel_number>" << std::endl;
        std::cerr << "Example: " << argv[0] << " photo.jpg blue_channel.jpg 0" << std::endl;
        std::cerr << "(Channel: 0=Blue, 1=Green, 2=Red)" << std::endl;
        return -1;
    }

    std::string inputPath = argv[1];
    std::string outputPath = argv[2];
    int channelToExtract = -1;

    try {
        channelToExtract = std::stoi(argv[3]);
    } catch (const std::exception& e) {
        std::cerr << "Error: Invalid channel number. Must be 0, 1, or 2." << std::endl;
        return -1;
    }

    if (channelToExtract < 0 || channelToExtract > 2) {
        std::cerr << "Error: Channel number must be 0 (Blue), 1 (Green), or 2 (Red)." << std::endl;
        return -1;
    }

    cv::Mat inputImage = cv::imread(inputPath, cv::IMREAD_COLOR);

    if (inputImage.empty()) {
        std::cerr << "Error: Could not read the input image: " << inputPath << std::endl;
        return -1;
    }

    if (inputImage.channels() != 3) {
         std::cerr << "Error: Input image is not a 3-channel color image." << std::endl;
        return -1;
    }

    cv::Mat singleChannel(inputImage.size(), CV_8UC1);

    for (int y = 0; y < inputImage.rows; ++y) {
        for (int x = 0; x < inputImage.cols; ++x) {
            cv::Vec3b bgrPixel = inputImage.at<cv::Vec3b>(y, x);
            
            unsigned char channelValue = bgrPixel[channelToExtract];
            
            singleChannel.at<unsigned char>(y, x) = channelValue;
        }
    }

    cv::Mat outputImage;
    bool success = cv::imwrite(outputPath, singleChannel);

    if (!success) {
        std::cerr << "Error: Could not save the output image: " << outputPath << std::endl;
        return -1;
    }

    std::cout << "Successfully extracted channel " << channelToExtract 
              << " from " << inputPath 
              << " and saved to " << outputPath << std::endl;

    return 0;
}