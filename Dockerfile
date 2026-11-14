# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Café runs in Bangkok: "today" on the dashboard and order times use this zone
ENV TZ=Asia/Bangkok
COPY --from=build /app/target/*.jar app.jar
# Render sets PORT (default 10000); application.yaml reads it
EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Asia/Bangkok", "-jar", "app.jar"]
