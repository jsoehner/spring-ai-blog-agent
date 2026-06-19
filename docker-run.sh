#!/bin/bash

# Build the latest image
docker build -t jsoehner/spring-ai-agent:latest .

# Stop and remove any existing containers with the same names
docker rm -f supervisor-agent researcher-agent 2>/dev/null

# Run the researcher container
docker run -d \
  --name researcher-agent \
  -e SPRING_PROFILES_ACTIVE=researcher \
  -v "$(pwd)/config:/app/config" \
  jsoehner/spring-ai-agent:latest

# Run the supervisor container
docker run -d \
  --name supervisor-agent \
  -p 8081:8080 \
  -e GITHUB_TOKEN="${GITHUB_TOKEN}" \
  -e SPRING_PROFILES_ACTIVE=supervisor \
  -e RESEARCHER_URL="http://researcher-agent:8080/research" \
  --link researcher-agent:researcher-agent \
  -v "$(pwd)/config:/app/config" \
  -v "$(pwd)/.git:/app/.git" \
  -v "$(pwd)/blog_draft.html:/app/blog_draft.html" \
  jsoehner/spring-ai-agent:latest

echo "Supervisor-agent is running on port 8081, researcher-agent is running internally."
