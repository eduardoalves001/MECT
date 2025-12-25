# Compiler
CXX = g++

# Compiler flags
CXXFLAGS = -std=c++17 -Wall -Wextra `pkg-config --cflags opencv4`

# Linker flags
LDFLAGS = `pkg-config --libs opencv4`

# Target executables
TARGET1 = extract_channel
TARGET2 = negative
TARGET3 = mirror
TARGET4 = rotate
TARGET5 = brightness
TARGET6 = audio_codec
TARGET7 = image_codec
TARGET8 = verify_audio

# Source files
SOURCES1 = extract_channel.cpp
SOURCES2 = negative.c++
SOURCES3 = mirror.cpp
SOURCES4 = rotate.cpp
SOURCES5 = brightness.cpp
SOURCES6 = audio_codec.cpp bit_stream/src/bit_stream.cpp bit_stream/src/byte_stream.cpp
SOURCES7 = image_codec.cpp bit_stream/src/bit_stream.cpp bit_stream/src/byte_stream.cpp
SOURCES8 = verify_audio.cpp

# Object files
OBJECTS1 = $(SOURCES1:.cpp=.o)
OBJECTS2 = $(SOURCES2:.c++=.o)
OBJECTS3 = $(SOURCES3:.cpp=.o)
OBJECTS4 = $(SOURCES4:.cpp=.o)
OBJECTS5 = $(SOURCES5:.cpp=.o)
OBJECTS6 = $(patsubst %.cpp,%.o,$(SOURCES6))
OBJECTS7 = $(patsubst %.cpp,%.o,$(SOURCES7))
OBJECTS8 = $(SOURCES8:.cpp=.o)

# Link with libsndfile for audio I/O
LIBS = -lsndfile

# Default target
all: $(TARGET1) $(TARGET2) $(TARGET3) $(TARGET4) $(TARGET5) $(TARGET6) $(TARGET7) $(TARGET8)

# Build the extract_channel executable
$(TARGET1): $(OBJECTS1)
	$(CXX) $(OBJECTS1) -o $(TARGET1) $(LDFLAGS)

# Build the negative executable
$(TARGET2): $(OBJECTS2)
	$(CXX) $(OBJECTS2) -o $(TARGET2) $(LDFLAGS)

# Build the mirror executable
$(TARGET3): $(OBJECTS3)
	$(CXX) $(OBJECTS3) -o $(TARGET3) $(LDFLAGS)

# Build the rotate executable
$(TARGET4): $(OBJECTS4)
	$(CXX) $(OBJECTS4) -o $(TARGET4) $(LDFLAGS)

# Build the brightness executable
$(TARGET5): $(OBJECTS5)
	$(CXX) $(OBJECTS5) -o $(TARGET5) $(LDFLAGS)

# Build the audio codec executable
$(TARGET6): $(OBJECTS6)
	$(CXX) $(OBJECTS6) -o $(TARGET6) $(LDFLAGS) $(LIBS)

# Build the image codec executable
$(TARGET7): $(OBJECTS7)
	$(CXX) $(OBJECTS7) -o $(TARGET7) $(LDFLAGS)

# Build the verify_audio executable
$(TARGET8): $(OBJECTS8)
	$(CXX) $(OBJECTS8) -o $(TARGET8) $(LDFLAGS) $(LIBS)

# Compile source files to object files
%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

%.o: %.c++
	$(CXX) $(CXXFLAGS) -c $< -o $@

# Clean up build files
clean:
	rm -f $(OBJECTS1) $(OBJECTS2) $(OBJECTS3) $(OBJECTS4) $(OBJECTS5) $(OBJECTS6) $(OBJECTS7) $(OBJECTS8) \
		bit_stream/src/*.o \
		$(TARGET1) $(TARGET2) $(TARGET3) $(TARGET4) $(TARGET5) $(TARGET6) $(TARGET7) $(TARGET8)

# Run the program (example usage)
run: $(TARGET)
	./$(TARGET) input.jpg output.jpg 0

# Phony targets
.PHONY: all clean run