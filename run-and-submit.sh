#!/bin/bash

BUILD=false
if [ "$1" = "--build" ]; then
  BUILD=true
  shift
fi

TOPIC=$1

if [ -z "$TOPIC" ]; then
  echo "Usage: ./run-and-submit.sh [--build] \"<blog_topic>\""
  echo "Example: ./run-and-submit.sh \"AI code tech debt\""
  exit 1
fi

EXPECTED_SERVICES=$(docker-compose config --services 2>/dev/null | wc -l | tr -d ' ')
RUNNING_SERVICES=$(docker-compose ps --services --status=running 2>/dev/null | wc -l | tr -d ' ')

if [ "$EXPECTED_SERVICES" -eq "$RUNNING_SERVICES" ] && [ "$RUNNING_SERVICES" -gt 0 ]; then
  echo "✅ All $RUNNING_SERVICES containers are fully operational. Skipping environment restart..."
else
  echo "⚠️ Environment not fully operational ($RUNNING_SERVICES/$EXPECTED_SERVICES running). Re-initializing..."
  
  docker-compose down 2>/dev/null || true
  docker rm -f supervisor-agent researcher-agent 2>/dev/null || true

  if [ "$BUILD" = true ]; then
    echo "🛠️ Building local image instead of pulling..."
    docker-compose up -d --build
  else
    echo "🔄 Pulling the latest image from ghcr.io..."
    docker pull ghcr.io/jsoehner/spring-ai-blog-agent:latest
    echo "🚀 Starting containers with docker-compose..."
    docker-compose up -d
  fi

  echo "⏳ Waiting for Supervisor Agent API to become available..."
  max_attempts=30
  attempt=1
  api_ready=false

  while [ $attempt -le $max_attempts ]; do
    if curl -sf http://localhost:8081/actuator/health > /dev/null; then
      api_ready=true
      break
    fi
    echo -n "."
    sleep 2
    ((attempt++))
  done
  echo ""

  if [ "$api_ready" = true ]; then
    echo "✅ Supervisor Agent is up!"
    echo "⏳ Waiting 10 seconds for RabbitMQ to fully start..."
    sleep 10
  else
    echo "❌ Error: Timed out waiting for the Supervisor Agent to start on port 8081."
    exit 1
  fi
fi

echo "Submitting topic..."
  
  # URL encode the topic using jq (or just basic curl urlencode)
  response=$(curl -s -G --data-urlencode "topics=${TOPIC}" "http://localhost:8081/blog")
  
  echo "🎉 Success!"
  echo "Response: $response"
  echo "Topic: \"$TOPIC\" has been queued."
  echo ""
  echo "👉 To watch the progress in real-time, run:"
  echo "   docker compose logs -f researcher-agent"
