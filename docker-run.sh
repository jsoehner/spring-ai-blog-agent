#!/bin/bash

# Pull the latest image from Docker Hub
docker pull jsoehner/spring-ai-agent:latest

echo "🚀 Starting containers with docker-compose..."
docker-compose up -d

echo "✅ Supervisor-agent is running on port 8081, researcher-agent is running internally."
