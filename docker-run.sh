#!/bin/bash

# Pull the latest image from Docker Hub
docker pull jsoehner/spring-ai-agent:latest

# Check for existing containers with the same names
containers=$(docker ps -aq -f "name=supervisor-agent" -f "name=researcher-agent")
if [ -n "$containers" ]; then
  read -p "⚠️ Existing containers found. Do you want to stop and remove them? (y/N) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🛑 Stopping and removing existing containers..."
    docker rm -f $containers 2>/dev/null
  fi
fi

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
