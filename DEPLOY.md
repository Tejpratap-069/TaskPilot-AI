# Deploying TaskPilot AI to the internet

Goal: ONE public link that any device can open, with the real backend,
user accounts (register/login), and a MySQL database storing all data.
The `Dockerfile` in this folder builds the whole product — React site
bundled inside the Spring Boot service — as a single deployable unit.

## What you need

1. A **GitHub** account with this project pushed to a repository.
2. A **hosting** account. Recommended: **Railway** (can run the app and a
   MySQL database in the same project). Alternative: Render for the app +
   a free MySQL from TiDB Cloud or Aiven.
3. About 10 minutes for the first deploy.

> Free tiers are fine for demos and your first users. They usually sleep
> when idle and have small limits. When you start selling, move the app
> and database to the host's paid tier (roughly $5–10/month to start) —
> no code change is needed, only the plan and the same settings below.

## Railway steps (recommended)

1. Push this folder to a GitHub repository (keep the `Dockerfile` at the
   repository root).
2. On railway.app: **New Project → Deploy from GitHub repo** → pick the
   repository. Railway detects the Dockerfile and builds it.
3. In the same project: **New → Database → Add MySQL**. Railway creates
   the database service.
4. Open your app service → **Variables** and set:
   - `DB_URL` = `jdbc:mysql://<mysql-host>:<port>/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
     (host/port come from the MySQL service's variables: `MYSQLHOST`, `MYSQLPORT`)
   - `DB_USERNAME` = the MySQL service's `MYSQLUSER`
   - `DB_PASSWORD` = the MySQL service's `MYSQLPASSWORD`
   - `JWT_SECRET` = any long random string (32+ characters) — this signs
     login tokens; never share it or commit it to GitHub.
   - Optional demo account: `DEMO_USER_EMAIL`, `DEMO_USER_PASSWORD`, `DEMO_USER_NAME`
5. App service → **Settings → Networking → Generate Domain**. That URL is
   your public product link. Tables are created automatically on first
   start (Hibernate `ddl-auto=update`).
6. Open the link on your phone (not just your laptop): register a new
   account, create a task, log out, log back in — the task must still be
   there. That confirms accounts + database storage work end to end.

## Render alternative

1. Create a free MySQL database on TiDB Cloud (Serverless) or Aiven and
   copy its host, port, database name, username and password.
2. On render.com: **New → Web Service** → connect the GitHub repo →
   Runtime: **Docker**.
3. Set the same environment variables as above (`DB_URL` built from your
   database's host/port/name) and deploy. Render gives you an
   `https://<name>.onrender.com` link.

## Notes

- The Python NLP service stays optional in production: if `AI_SERVICE_URL`
  is not set/reachable, the Java backend's built-in parser answers AI
  Capture with equivalent rule-based results. To run it too, deploy
  `backend-ai/` as a second small service and set `AI_SERVICE_URL` to it.
- Health check: opening `/swagger-ui.html` on your domain should load.
- Custom domain (e.g. `taskpilot.yourdomain.com`): both Railway and
  Render support it under Settings → Networking/Domains; it becomes
  practical on a paid plan.
- Selling later: the accounts, passwords (BCrypt-hashed) and per-user
  data isolation are already in place. Payments/subscriptions are a
  separate feature (e.g. Stripe/Razorpay checkout) and are NOT included
  in this build — add billing only when you are ready to charge.
