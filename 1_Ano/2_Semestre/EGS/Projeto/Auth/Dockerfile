FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .

RUN apt-get update --allow-releaseinfo-change && \
    apt-get install -y curl && \
    pip install --no-cache-dir -r requirements.txt && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY . .

# Dá permissões ao novo entrypoint
RUN chmod +x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
