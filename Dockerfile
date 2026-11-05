# TaskPilot AI — single-service deployment image.
# Builds the React frontend, bundles it into the Spring Boot jar,
# and runs everything (site + API + accounts) as ONE service.
# The database is external MySQL, configured by environment variables.

# ---- Stage 1: build the React frontend ----
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# Same-origin API: the backend serves the site, so calls go to /api.
ENV VITE_API_BASE_URL=/api
RUN npm run build

# ---- Stage 2: build the Spring Boot backend with the frontend inside ----
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline -DskipTests
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -q package -DskipTests

# ---- Stage 3: run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/backend/target/taskpilot-backend-1.0.0.jar app.jar
# Hosts inject PORT; application.properties reads it.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
