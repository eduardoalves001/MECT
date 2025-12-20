#!/bin/bash

echo "Installing npm dependencies..."
npm install

# Start the development server
exec npm run dev -- --host 0.0.0.0