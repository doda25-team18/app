# Builder image with entire jdk
FROM maven:4.0.0-rc-4-eclipse-temurin-25 AS build-stage
WORKDIR /app

# Define build arguments for GitHub Packages authentication
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

COPY pom.xml .

# Create a temporary settings.xml for authentication
# We use a custom location so we don't mess with system defaults
RUN echo "<settings><servers><server><id>github</id><username>${GITHUB_ACTOR}</username><password>${GITHUB_TOKEN}</password></server></servers></settings>" > settings.xml

# Use -s settings.xml to use our auth configuration
RUN mvn -s settings.xml dependency:go-offline -B

COPY src ./src

RUN mvn -s settings.xml package -Dskiptests

# Different image with just the runtime
FROM eclipse-temurin:25-jre-ubi10-minimal AS runtime-stage

COPY --from=build-stage /app/target/*.jar app.jar

ENV MODEL_HOST="http://localhost:8081" \
    SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java",  "-jar",  "app.jar"]