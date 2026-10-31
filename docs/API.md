# API Documentation

This page is a placeholder for the verified TaskPilot AI API contract. Exact endpoint paths, request fields, response schemas, and authentication requirements should be copied from the actual controller and route definitions rather than guessed.

## Backend base URL

Local development commonly uses:

```text
http://localhost:8080
```

Check the Spring Boot configuration and startup logs to confirm the effective port and any context path.

## How to document an endpoint

For every endpoint, record:

| Field | What to include |
|---|---|
| Method and path | HTTP method and exact route |
| Purpose | What the endpoint does |
| Authentication | Public, authenticated, or role-restricted |
| Request | Headers, path/query parameters, JSON body |
| Success response | Status code and response schema |
| Error responses | Validation, authentication, authorization, and server errors |

## Authentication

Do not assume that JWT authentication is fully enabled merely because a JWT filter exists. Verify the active Spring Security configuration and test login, token validation, and protected routes.

## Verification checklist

- [ ] List routes from the Spring Boot controller classes.
- [ ] Document request and response DTOs.
- [ ] Confirm which routes require a token.
- [ ] Test validation and error responses.
- [ ] Document the Python service separately if it exposes its own API.
- [ ] Add example requests only after they have been tested against the running application.
