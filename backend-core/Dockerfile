# Imagem com Chromium + dependências do Playwright já provisionadas (ADR-001).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM mcr.microsoft.com/playwright/java:v1.49.0-noble
WORKDIR /app
COPY --from=build /app/target/backend-core-0.1.0.jar app.jar
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=0
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
