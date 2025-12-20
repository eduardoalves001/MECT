#!/bin/bash
set -e

# Parse arguments
DOMAIN="$1"
CERT_DIR="./certs"
DAYS_VALID=365

if [ -z "$DOMAIN" ]; then
    echo "❌ Error: DOMAIN is required"
    echo "Usage: $0 <DOMAIN>"
    echo "Example: $0 deti-engsoft-05.ua.pt"
    exit 1
fi

# Check if certificates already exist
if [ -f "$CERT_DIR/fullchain.pem" ] && [ -f "$CERT_DIR/privkey.pem" ]; then
    echo "✅ SSL certificates already exist"
    echo "   Certificate: $CERT_DIR/fullchain.pem"
    echo "   Private Key: $CERT_DIR/privkey.pem"
    exit 0
fi

echo "� Generating self-signed SSL certificates for $DOMAIN"

# Create certs directory if it doesn't exist
mkdir -p "$CERT_DIR"

# Generate private key and certificate
openssl req -x509 -nodes -days $DAYS_VALID \
  -newkey rsa:2048 \
  -keyout "$CERT_DIR/privkey.pem" \
  -out "$CERT_DIR/fullchain.pem" \
  -subj "/C=PT/ST=Aveiro/L=Aveiro/O=University of Aveiro/OU=DETI/CN=$DOMAIN" \
  -addext "subjectAltName=DNS:$DOMAIN,DNS:*.ua.pt"

# Set proper permissions
chmod 644 "$CERT_DIR/fullchain.pem"
chmod 600 "$CERT_DIR/privkey.pem"

echo "✅ SSL certificates generated successfully!"
echo "   Certificate: $CERT_DIR/fullchain.pem"
echo "   Private Key: $CERT_DIR/privkey.pem"
echo ""
echo "⚠️  Note: These are self-signed certificates."
echo "   Browsers will show a security warning on first access."
