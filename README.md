# TaskPilot AI — Intelligent Personal Task Assistant

**Plan smarter. Focus better. Get things done.**

TaskPilot AI is a working task-management SaaS product that converts natural-language
instructions into structured tasks. You type a sentence; the system extracts individual
tasks, deadlines, priorities and categories; you review and approve; only approved
tasks are saved.

> Hackathon Problem Statement No. 41 — AI Personal Task Assistant

## Architecture

| Layer | Technology | Role |
|---|---|---|
| Frontend | React + Vite, React Router, Recharts, Lucide | Premium SaaS UI (landing, auth, dashboard, AI capture, tasks, My Day, calendar, Kanban, activity, notifications, Pomodoro, settings) |
| Primary backend | **Java 21 + Spring Boot 3.3** (Spring Web, Spring Data JPA/Hibernate, Spring Security, JWT, BCrypt, Bean Validation, Springdoc OpenAPI) | ALL application data, auth, business logic, dashboard analytics |
| Database | **MySQL 8** (JPA/Hibernate) | users, tasks, categories, subtasks, task_history, notifications, user_preferences |
| NLP service | Python 3 (standard library only, zero dependencies) | Supporting service ONLY: natural-language extraction + explainable prioritization |

Java Spring Boot is the primary backend and the only component that talks to MySQL.
Python is never in the data path: if the Python service is down, the Java backend
falls back to a built-in parser implementing the same rules, so task capture keeps working.

The NLP engine is **rule-based NLP** (text segmentation, date parsing, keyword
classification, priority scoring). It is deliberately labelled as such — it is not a
trained machine-learning model, and it needs no paid API key.

## Deploying to the internet

See **DEPLOY.md** — the included `Dockerfile` builds the whole product (site + API + accounts) as one service for Railway/Render with a MySQL database, giving one public link that works on any device.

## Project structure

```
TaskPilot-AI/
  backend/       Spring Boot application (pom.xml, src/main/java/com/taskpilot/...)
                 - src/main/resources/db/schema.sql  (reference MySQL DDL)
  backend-ai/    nlp_service.py  (Python NLP service, port 8000)
  frontend/      React + Vite application (port 5173)
```

## Prerequisites

- JDK 21 and Maven 3.9+
- MySQL 8 running locally
- Node.js 18+ and npm
- Python 3.10+

## 1. MySQL setup

```sql
CREATE DATABASE taskpilot;
CREATE USER 'taskpilot'@'localhost' IDENTIFIED BY 'taskpilot';
GRANT ALL PRIVILEGES ON taskpilot.* TO 'taskpilot'@'localhost';
FLUSH PRIVILEGES;
```

Tables are created automatically by Hibernate on first run
(`spring.jpa.hibernate.ddl-auto=update`). A reference DDL is provided at
`backend/src/main/resources/db/schema.sql` if you prefer manual provisioning.

Database credentials are NOT hardcoded — they come from environment variables
(defaults below are for local development only):

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/taskpilot?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | `taskpilot` |
| `DB_PASSWORD` | `taskpilot` |
| `JWT_SECRET` | dev placeholder — **set your own in production** |
| `AI_SERVICE_URL` | `http://localhost:8000` |
| `SERVER_PORT` | `8080` |

## 2. Run the Python NLP service

```bash
cd backend-ai
python nlp_service.py            # listens on http://localhost:8000
# health check:  curl http://localhost:8000/health
```

No `pip install` is needed — it uses only the Python standard library.

## 3. Run the Java backend

```bash
cd backend
mvn spring-boot:run              # API on http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Optional demo account

For the live demo, set environment variables before starting the backend and a
demo account (with a few seed tasks) is created automatically. Nothing is
hardcoded; without these variables no demo account exists.

```bash
# Windows PowerShell example
$env:DEMO_USER_EMAIL="demo@taskpilot.ai"
$env:DEMO_USER_PASSWORD="demo1234"
mvn spring-boot:run
```

## 4. Run the frontend

```bash
cd frontend
npm install
npm run dev                      # http://localhost:5173
```

The frontend reads the API base URL from `frontend/.env`
(`VITE_API_BASE_URL=http://localhost:8080/api`).

## Demo workflow (evaluation script)

1. Open http://localhost:5173 → landing page → **Get started** → register, or sign in with the demo account.
2. Open **AI Task Capture** and paste:
   *"I have to finish my Java assignment by tomorrow, revise machine learning on Sunday, complete my Japanese preparation before Monday, and submit my project next Friday. Java is the most urgent."*
3. Click **Analyze instruction** → 4 extracted tasks appear with deadlines, priorities, categories and confidence. The Java task is URGENT because of the final sentence.
4. Edit a deadline or priority in the preview, untick one task, then **Save approved tasks**.
5. Open **My Tasks** → the approved tasks are there, persisted in MySQL.
6. Change a task's status (TODO → IN_PROGRESS → COMPLETED) from the list, the board, or My Day.
7. Return to **Overview** → totals, overdue, due-today, weekly chart and completion rate have updated from real data.
8. Open **Activity** → creation, status and priority changes are in the timeline.
9. Restart the backend and sign in again → all data survives (MySQL persistence).

## Key API endpoints

```
POST /api/auth/register        POST /api/auth/login         GET /api/auth/me
POST /api/ai/parse-tasks       POST /api/ai/prioritize
POST /api/tasks                GET  /api/tasks              GET /api/tasks/{id}
PUT  /api/tasks/{id}           PATCH /api/tasks/{id}/status PATCH /api/tasks/{id}/priority
DELETE /api/tasks/{id}         GET  /api/tasks/{id}/history
POST /api/tasks/{id}/subtasks  PATCH /api/tasks/{id}/subtasks/{subId}/toggle
GET  /api/dashboard/summary    GET  /api/dashboard/weekly   GET /api/dashboard/upcoming
GET  /api/dashboard/focus      GET  /api/dashboard/breakdown
GET  /api/planner/today        GET  /api/planner/upcoming   GET  /api/activity
GET  /api/notifications        PATCH /api/notifications/{id}/read
GET  /api/preferences          PUT  /api/preferences
```

Security model: every endpoint except `/api/auth/**` and Swagger requires a JWT.
The current user is always derived from the token's security context — never from
request parameters — and every task query is scoped to that user (cross-user access
returns 404). Passwords are stored only as BCrypt hashes.

## Running without MySQL (smoke tests)

The default profile is MySQL. For a quick smoke test on a machine without MySQL,
a `dev` profile uses an in-memory H2 database in MySQL-compatibility mode:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Data in dev profile does not persist between runs. Use the default MySQL profile
for the real demonstration.

## Tests

```bash
cd backend
mvn test
```

Test coverage: registration/login/invalid-login, unauthorized access (401),
task CRUD, status completion/reopening, ownership isolation, dashboard statistics
computed from real rows (including overdue calculation), task history recording,
and the AI parse endpoint through the local fallback parser (Python service
intentionally unreachable in the test profile). The NLP extraction rules are
additionally covered by unit tests for the Java fallback parser (multiple-task
extraction, weekday/month dates, the "most urgent" comparative boost, and
missing-date warnings).

## Design

The UI follows a restrained professional SaaS design system (Linear/Notion-inspired,
not copied): navy `#243447`, blue-gray `#526579`, teal accent `#477C78`, off-white
`#F7F8FA` background, white cards with thin `#E3E7EC` borders, Inter typography,
slim dark sidebar + white topbar. No gradients, glow or glassmorphism.
