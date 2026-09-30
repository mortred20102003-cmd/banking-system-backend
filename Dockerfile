# =========================================================
# Build stage — builds the jar with Maven
# =========================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# =========================================================
# Run stage — minimal JRE with just the jar
# =========================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=build /app/target/banking-system-backend-1.0.0.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
