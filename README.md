# TaskPilot AI

An AI-assisted task management application built with **React**, **Spring Boot**, **MySQL**, and a **Python AI/NLP service**.

> **Project status:** Development / local setup. Endpoint-specific documentation and deployment instructions should be verified against the complete source code before production use.

## Overview

TaskPilot AI is designed to bring task management and AI-assisted workflows together in one application. It uses a web frontend, a Java backend API, a relational database, and a Python service for AI/NLP functionality.

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, JavaScript |
| Backend API | Java, Spring Boot, Maven |
| Database | MySQL |
| AI/NLP service | Python |
| Authentication | Spring Security / JWT (verify the active configuration in source before deployment) |

## Repository Structure

```text
TaskPilot-AI/
├── frontend/       # React + Vite application
├── backend/        # Spring Boot REST API
├── backend-ai/     # Python AI/NLP service
├── docs/           # Project documentation
├── .gitignore
├── .env.example
└── README.md
```

This describes the intended layout; keep it aligned with the actual committed files.

## Prerequisites

- Git
- Node.js and npm
- A JDK compatible with the backend (Java 21 is the intended baseline)
- Maven
- MySQL Server
- Python 3 and pip

## Local Development

### 1. Clone the repository

```bash
git clone https://github.com/Tejpratap-069/TaskPilot-AI.git
cd TaskPilot-AI
```

### 2. Configure MySQL and start the backend

Create a local MySQL database and configure the backend's database URL, username, and password in your local configuration. Never commit real credentials or JWT signing keys.

```bash
cd backend
mvn spring-boot:run
```

The backend commonly runs on port `8080`, unless configured otherwise.

### 3. Start the frontend

In a second terminal from the repository root:

```bash
cd frontend
npm install
npm run dev
```

Vite commonly serves the frontend at `http://localhost:5173`. Use the URL printed in your terminal if it differs.

### 4. Start the Python AI service

Inspect `backend-ai/` to identify its dependency file and entry point. If `requirements.txt` exists, create a virtual environment first.

**Windows PowerShell**
```powershell
cd backend-ai
python -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

**macOS / Linux**
```bash
cd backend-ai
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

Start the service using the command documented by its current source/configuration.

## Documentation

- [Local setup](docs/SETUP.md)
- [Architecture overview](docs/ARCHITECTURE.md)
- [API documentation notes](docs/API.md)

## Security Notes

- Do not commit `.env`, local configuration containing secrets, database dumps, API keys, or JWT secrets.
- Use `.env.example` for safe placeholders only; update variable names to match the actual implementation.
- Rotate any credential that has already been exposed publicly.
- Configure production CORS, authentication, HTTPS, and secrets explicitly before deployment.
- Exclude `node_modules/`, Maven `target/`, Python virtual environments, logs, and local database files from version control.

## Troubleshooting

- **Backend will not start:** Check Java/Maven versions, MySQL availability, database configuration, and startup logs.
- **Frontend cannot reach the API:** Check the API base URL, backend port, CORS configuration, and authentication requirements.
- **Port already in use:** Stop the conflicting process or configure another port.
- **401 Unauthorized:** Verify the route and active Spring Security configuration; protected routes may require a valid token.

## Contributing

1. Create a feature branch.
2. Keep changes focused and documented.
3. Run the relevant frontend/backend checks.
4. Never include secrets or generated build/dependency directories in commits.
5. Include a clear summary and testing notes with pull requests.

## License

A license has not yet been selected. Until one is added, do not assume that the repository grants permission to reuse, modify, or redistribute its code.
