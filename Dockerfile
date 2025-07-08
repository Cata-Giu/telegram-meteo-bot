# Stage di build: usa l'immagine Maven con JDK 17 per compilare e fare il package
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage di runtime: immagine JRE 17 più leggera per eseguire l'app
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copia il jar "shaded" (tutte le dipendenze incluse) generato nel build stage
COPY --from=build /app/target/meteo-app-1.0-SNAPSHOT.jar app.jar
# Comando di avvio dell'applicazione
CMD ["java", "-jar", "app.jar"]
