# Local Development Setup

This guide describes the intended local workflow for TaskPilot AI. Some commands may need adjustment to match the entry points and configuration files in the full source tree.

## Requirements

- Git
- Node.js and npm
- JDK (Java 21 is the intended baseline)
- Maven
- MySQL Server
- Python 3 and pip

## 1. Get the code

```bash
git clone https://github.com/Tejpratap-069/TaskPilot-AI.git
cd TaskPilot-AI
```

## 2. MySQL

Create a database for the application using your local MySQL client. Then configure the backend with the database URL, username, and password. Use a local untracked configuration file or environment variables; never put real credentials in source control.

Do not assume a database name or schema until you inspect the project's current configuration and entities.

## 3. Start the Spring Boot API

```bash
cd backend
mvn spring-boot:run
```

The default development port is commonly `8080`. Check the application configuration and startup log for the actual port and context path.

## 4. Start the React frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite, commonly `http://localhost:5173`.

If the frontend cannot reach the API, verify its API base URL, the backend port, CORS rules, and whether authentication is required.

## 5. Python AI/NLP service

Inspect `backend-ai/` to identify its application entry point and dependency file. If `requirements.txt` is present, create and activate a virtual environment before installing packages.

Windows PowerShell:

```powershell
cd backend-ai
python -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

macOS / Linux:

```bash
cd backend-ai
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

Start the service using the command specified by its source code or project configuration.

## Safe publishing checklist

- Confirm `.env` and local configuration files containing secrets are ignored.
- Confirm `node_modules/`, `target/`, virtual environments, logs, and local databases are not tracked.
- Replace all placeholder values on the deployment host with real secrets stored in its secret manager.
- Rotate any credentials that were accidentally exposed.
- Verify authentication and CORS before exposing services publicly.
