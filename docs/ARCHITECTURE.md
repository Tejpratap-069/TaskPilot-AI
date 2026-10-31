# Architecture Overview

## High-level components

```text
User
  |
  v
React + Vite frontend
  |
  v
Spring Boot REST API  ------>  MySQL database
  |
  v
Python AI/NLP service
```

This is a conceptual view of the intended application layers. The exact request paths, service-to-service protocol, and data flow must be confirmed from the implementation.

## Frontend

The `frontend/` directory contains the React application built and served during development with Vite. It provides the user interface and communicates with backend services through configured API calls.

## Backend API

The `backend/` directory contains the Java Spring Boot application. It is responsible for API handling and application logic, and it connects to MySQL using the backend's configured persistence layer.

Authentication and authorization behavior should be verified against the active Spring Security configuration before the application is deployed.

## AI/NLP service

The `backend-ai/` directory contains the Python service intended for AI/NLP-related processing. Confirm its framework, endpoints, model dependencies, and integration contract from its actual source files.

## Data layer

MySQL stores application data. The schema, entities, relationships, migrations, and seed data should be documented after inspecting the committed backend source.

## Deployment considerations

- Keep secrets out of source control.
- Configure separate development and production settings.
- Restrict database access to trusted services.
- Use HTTPS and explicit CORS origins in production.
- Add health checks, structured logs, and monitoring before public deployment.
