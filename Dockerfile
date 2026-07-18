FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Cache dependencies
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user to run the app
RUN addgroup -S spring && adduser -S spring -G spring

# Install git, github cli (gh), python3, and requests inside the container
RUN apk add --no-cache git github-cli python3 py3-requests

COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

# We will run the Spring Boot app on port 8080
EXPOSE 8080

USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]
