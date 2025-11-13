# Builder image with entire jdk
FROM maven:4.0.0-rc-4-eclipse-temurin-25 AS build-stage
WORKDIR /app

COPY pom.xml .

# Install dependencies first so we can reuse it if the src changes but pom.xml doesn't
RUN mvn dependency:go-offline -B 

COPY src ./src

RUN mvn package -Dskiptests

# Different image with just the runtime
FROM eclipse-temurin:25-jre-ubi10-minimal AS runtime-stage

COPY --from=build-stage /app/target/*.jar app.jar

ENV MODEL_HOST="http://localhost:8081" \
    SERVER_PORT=8080


ENTRYPOINT ["sh", "-c", "MODEL_HOST=${MODEL_HOST} SERVER_PORT=${SERVER_PORT} java -jar app.jar"]



