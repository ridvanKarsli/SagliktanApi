# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Once sadece pom.xml kopyalanip bagimliliklar indiriliyor: pom degismedigi
# surece bu katman cache'den gelir, src degistiginde yeniden indirme olmaz.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/SagliktanApi-0.0.1-SNAPSHOT.jar app.jar

# Render PORT env degiskenini kendisi veriyor (application.properties'teki
# server.port=${PORT:8080} bunu okuyor), EXPOSE sadece dokumantasyon amacli.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
