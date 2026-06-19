#!/bin/bash

echo "🚀 Building and starting containers with docker-compose..."
docker-compose up -d --build

echo "✅ Supervisor-agent is running on port 8081, researcher-agent is running internally."
