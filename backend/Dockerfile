FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace/app

COPY pom.xml ./
COPY src src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:17.0.19_10-jre-jammy AS runtime

WORKDIR /app

ENV UPLOAD_DIR=/app/uploads

RUN adduser --system --group app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/app/target/*.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
