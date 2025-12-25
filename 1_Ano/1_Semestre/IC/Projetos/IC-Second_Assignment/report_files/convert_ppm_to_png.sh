#!/bin/bash

# Script to convert all PPM files to PNG format
# Usage: ./convert_ppm_to_png.sh [directory]
# If no directory specified, uses current directory

# Set the directory to search (default: current directory)
DIR="${1:-.}"

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null && ! command -v magick &> /dev/null; then
    echo "Error: ImageMagick is not installed."
    echo "Please install it with: sudo apt-get install imagemagick"
    exit 1
fi

# Check if directory exists
if [ ! -d "$DIR" ]; then
    echo "Error: Directory '$DIR' does not exist."
    exit 1
fi

# Counter for converted files
count=0

# Find all .ppm files in the directory (not recursive)
echo "Converting PPM files in: $DIR"
echo "-----------------------------------"

for ppm_file in "$DIR"/*.ppm; do
    # Check if any .ppm files exist
    if [ ! -f "$ppm_file" ]; then
        echo "No PPM files found in $DIR"
        exit 0
    fi
    
    # Get the base filename without extension
    base_name=$(basename "$ppm_file" .ppm)
    
    # Create the output PNG filename
    png_file="$DIR/${base_name}.png"
    
    # Convert using ImageMagick
    echo "Converting: $ppm_file -> $png_file"
    
    # Use magick if available (IMv7), otherwise use convert (IMv6)
    if command -v magick &> /dev/null; then
        magick "$ppm_file" "$png_file"
    else
        convert "$ppm_file" "$png_file"
    fi
    
    # Check if conversion was successful
    if [ $? -eq 0 ]; then
        echo "  ✓ Success"
        ((count++))
    else
        echo "  ✗ Failed"
    fi
done

echo "-----------------------------------"
echo "Converted $count file(s) to PNG format."
