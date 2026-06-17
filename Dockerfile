FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install git and github cli (gh) inside the container so it can commit and open PRs
RUN apk add --no-cache git github-cli

COPY --from=builder /app/build/libs/*.jar app.jar

# We will run the Spring Boot app on port 8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
