# Stage 1: Build the application with Maven using Java 21
FROM maven:3.9-eclipse-temurin-21 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper and pom.xml to leverage Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download project dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application, skipping tests
RUN ./mvnw package -DskipTests


# Stage 2: Create the final, lightweight runtime image using Java 21
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Expose the port the application runs on
EXPOSE 8080

# Copy the executable JAR from the build stage
# Note: The JAR name might change if you updated the version in pom.xml
COPY --from=build /app/target/*.jar app.jar

# Set the entrypoint to run the application
# The JWT_SECRET environment variable must be passed to the container at runtime
ENTRYPOINT ["sh","-c","java -Dserver.port=${PORT:-8080} -Xms256m -Xmx512m -jar app.jar"]