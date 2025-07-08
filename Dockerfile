# Stage 1: build del jar con Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: runtime con JRE
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/meteo-app-1.0-SNAPSHOT-shaded.jar app.jar
CMD ["java", "-jar", "app.jar"]
