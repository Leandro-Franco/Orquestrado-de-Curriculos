# Estágio de build: JDK e código-fonte existem SÓ aqui; a imagem final recebe apenas o jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime enxuto: JRE + somente o Chromium (ADR-001: o PDF exige um browser real).
# Alternativa simples porém ~3,3 GB: base mcr.microsoft.com/playwright/java:v1.49.0-noble,
# que traz JDK completo e TRÊS browsers (chromium, firefox e webkit).
FROM eclipse-temurin:21-jre-noble
WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
COPY --from=build /app/target/backend-core-0.1.0.jar app.jar
# A CLI do Playwright embarcada no próprio jar instala o Chromium e as
# dependências de sistema dele (--with-deps) — sem lista de pacotes na mão.
RUN java -cp app.jar -Dloader.main=com.microsoft.playwright.CLI \
      org.springframework.boot.loader.launch.PropertiesLauncher \
      install --with-deps chromium \
 && rm -rf /var/lib/apt/lists/*
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
