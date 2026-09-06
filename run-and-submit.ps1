$build = $false
$argsList = @()

for ($i = 0; $i -lt $args.Count; $i++) {
    if ($args[$i] -eq "--build") {
        $build = $true
    } else {
        $argsList += $args[$i]
    }
}

$topic = $argsList[0]

if (-not $topic) {
    Write-Host 'Usage: .\run-and-submit.ps1 [--build] "<blog_topic>"'
    Write-Host 'Example: .\run-and-submit.ps1 "<AI code tech debt>"'
    exit 1
}

# Get expected services count
$expectedServices = (docker compose config --services 2>$null | Measure-Object).Count
# Get running services count
$runningServices = (docker compose ps --services --status=running 2>$null | Measure-Object).Count

if (-not $build -and $expectedServices -eq $runningServices -and $runningServices -gt 0) {
    Write-Host "✅ All $runningServices containers are fully operational. Skipping environment restart..."
} else {
    Write-Host "⚠️ Environment not fully operational. $runningServices out of $expectedServices running. Re-initializing..."

    docker compose down 2>$null
    docker rm -f supervisor-agent researcher-agent 2>$null

    if ($build) {
        Write-Host "🛠️ Building local image instead of pulling..."
        docker compose up -d --build
    } else {
        Write-Host "🔄 Pulling the latest image from ghcr.io/jsoehner/spring-ai-blog-agent:latest..."
        docker pull ghcr.io/jsoehner/spring-ai-blog-agent:latest
        Write-Host "🚀 Starting containers with docker-compose..."
        # Attempt to bypass credential helper by clearing the config env var for this call
        $env:DOCKER_CONFIG = ""
        docker compose up -d
    }

    Write-Host "⏳ Waiting for Supervisor Agent API to become available..."
    $maxAttempts = 60
    $attempt = 1
    $apiReady = $false

    while ($attempt -le $maxAttempts) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -UseBasicParsing -Method Get -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                $apiReady = $true
                break
            }
        } catch {
            # Request failed, try again
        }
        Write-Host -NoNewline "."
        Start-Sleep -Seconds 2
        $attempt++
    }
    Write-Host ""

    if ($apiReady) {
        Write-Host "✅ Supervisor Agent is up!"
        Write-Host "⏳ Waiting 10 seconds for RabbitMQ to fully start..."
        Start-Sleep -Seconds 10
    } else {
        Write-Host "❌ Error: Timed out waiting for the Supervisor Agent to start on port 8081."
        Write-Host "Note: Check if Docker is running correctly and if there are any credential helper issues in your .docker/config.json."
        exit 1
    }
}

Write-Host "Submitting topic..."

# URL encode the topic
$encodedTopic = [uri]::EscapeDataString($topic)

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/blog?topics=$encodedTopic" -Method Get
    Write-Host "🎉 Success!"
    Write-Host "Response: $response"
    Write-Host "Topic: $topic has been queued."
} catch {
    Write-Host "❌ Error submitting topic: $_"
    exit 1
}

Write-Host ""
Write-Host "👉 To watch the progress in real-time, run:"
Write-Host "   docker compose logs -f researcher-agent"
