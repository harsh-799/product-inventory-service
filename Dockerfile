# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy project definition and source code
COPY pom.xml .
COPY src ./src

# Build the executable jar without running tests
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar file
COPY --from=builder /app/target/app.jar app.jar

# Expose application port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
