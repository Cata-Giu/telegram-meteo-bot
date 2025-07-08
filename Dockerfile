# Costruisce il progetto usando Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Esegue l'app usando un'immagine JRE leggera
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/meteo-app-1.0-SNAPSHOT-shaded.jar app.jar
CMD ["java", "-jar", "app.jar"]

