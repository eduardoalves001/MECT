# Python image
FROM python:3.8-slim

# Set working directory inside the container
WORKDIR /app

# Copy only necessary files to container
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the source code
COPY . .

# Expose the port used by Uvicorn
EXPOSE 8001

# Run the FastAPI app
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
