#!/bin/bash

TOPIC=$1

if [ -z "$TOPIC" ]; then
  echo "Usage: ./run-and-submit.sh \"<blog_topic>\""
  echo "Example: ./run-and-submit.sh \"AI code tech debt\""
  exit 1
fi

# We build the image locally to include any recent code changes
# docker pull jsoehner/spring-ai-agent:latest || echo "⚠️ Could not pull image. Proceeding with local image/build if necessary."

if docker-compose ps -q > /dev/null 2>&1; then
  read -p "⚠️ Containers may already be running. Do you want to stop them before starting? (y/N) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🛑 Stopping existing containers..."
    docker-compose down
  fi
fi

echo "🚀 Starting containers with docker-compose..."
docker-compose up -d --build

echo "⏳ Waiting for Supervisor Agent API to become available..."
# Try up to 30 times (approx 30 seconds)
max_attempts=30
attempt=1
api_ready=false

while [ $attempt -le $max_attempts ]; do
  if curl -s http://localhost:8081/health > /dev/null; then
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
  echo "Submitting topic..."
  
  # URL encode the topic using jq (or just basic curl urlencode)
  response=$(curl -s -G --data-urlencode "topics=${TOPIC}" "http://localhost:8081/blog")
  
  echo "🎉 Success!"
  echo "Response: $response"
  echo "Topic: \"$TOPIC\" has been queued."
  echo ""
  echo "👉 Monitoring logs in real-time:"
  docker-compose logs -f
else
  echo "❌ Error: Timed out waiting for the Supervisor Agent to start on port 8081."
  exit 1
fi
