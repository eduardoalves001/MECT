FROM node:24

WORKDIR /usr/app

# Copy package files first for better layer caching
COPY package*.json ./

# Copy startup script
COPY start-dev.sh ./
RUN chmod +x start-dev.sh

# Copy source code
COPY . .

# Expose port
EXPOSE 5173

CMD [ "bash", "./start-dev.sh" ]
