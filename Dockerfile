FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S assetdock && adduser -S assetdock -G assetdock
COPY --chown=assetdock:assetdock --from=builder /app/build/libs/*.jar app.jar
USER assetdock
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
