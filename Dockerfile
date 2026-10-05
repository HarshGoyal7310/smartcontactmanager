# Stage 1: Build the Application with Maven & Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the Application with Lightweight JRE
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Application port
EXPOSE 8282

# Start command
ENTRYPOINT ["java", "-jar", "app.jar"]