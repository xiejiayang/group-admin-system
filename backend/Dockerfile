# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace/app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN chmod +x ./mvnw \
    && find .mvn -type f -path '*/bin/mvn' -exec chmod +x {} \;

COPY src src

RUN ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

ENV UPLOAD_DIR=/app/uploads

RUN adduser --system --group app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/app/target/*.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
