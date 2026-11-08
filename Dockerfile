# syntax=docker/dockerfile:1
#
# The image Azure Container Apps runs. Two stages: the first builds the jar on a full JDK, the second runs it
# on an image holding only the Java runtime, so compilers and Maven never ship.
# The tests are not run here: the pipeline runs them before it builds the image.

# Stage 1: build
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# The cache mount keeps downloaded libraries between builds, so a rebuild only downloads what changed.
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw --batch-mode --no-transfer-progress package -DskipTests

# Stage 2: run
FROM eclipse-temurin:25-jre-noble
# GitHub uses this label to link the image to its repository.
LABEL org.opencontainers.image.source="https://github.com/hussenoloyede/teduh-claims"
# A user with no rights beyond its own files. If someone ever broke into the app, they would not be root.
RUN groupadd --system teduh && useradd --system --gid teduh --no-create-home teduh
WORKDIR /app
COPY --from=build /workspace/target/teduh-claims-*.jar app.jar
USER teduh
EXPOSE 8080
# Let the Java heap use three quarters of the container's memory (768 MB of the 1 GiB in Azure).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
