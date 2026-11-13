# syntax=docker/dockerfile:1
#
# The image Azure Container Apps runs. Three stages: Node builds the React app and the Storybook, a full JDK
# builds the jar with both inside it, and the last stage holds only the Java runtime and that jar, so Node,
# compilers and Maven never ship.
# The tests are not run here: the pipeline runs them before it builds the image.

# Stage 1: the React app and the Storybook
FROM node:22-bookworm-slim AS web
WORKDIR /web
# The Storybook tests need Playwright's Chromium, but this stage only builds, so skip the download.
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
# The package files first, so npm ci only runs again when a dependency changes.
COPY web/package.json web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY web/ ./
RUN npm run build && npm run build-storybook -- --quiet

# Stage 2: the jar
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# Spring Boot serves whatever is in static/ on the classpath: the React app at / and the Storybook at /storybook/.
COPY --from=web /web/dist src/main/resources/static
COPY --from=web /web/storybook-static src/main/resources/static/storybook
# The cache mount keeps downloaded libraries between builds, so a rebuild only downloads what changed.
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw --batch-mode --no-transfer-progress package -DskipTests

# Stage 3: run
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
