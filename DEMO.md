# TaskPilot AI — 3-Minute Live Demo Script

**Live site:** https://taskpilot-ai-gules.vercel.app
**Stack in one line:** React + Vite frontend on Vercel, Java 21 / Spring Boot backend on
Railway, PostgreSQL on Supabase. Real accounts, real database — nothing is mocked.

**Before you start:** open the site once a minute early and sign in, so the backend is warm.
Have a test account ready (or register a fresh one live — registration works and takes seconds).

**Fallback (say it once, confidently, if the site is slow):** "The frontend is on Vercel and
the API is on Railway's trial tier, so the first hit can be slow — the same full app is also
served directly from the backend at https://taskpilot-ai-production-7a32feca.up.railway.app,
so let me show it there." Then continue the same script on that link.

---

## The script (3 minutes)

### 0:00–0:30 — The problem and the dashboard
1. Open https://taskpilot-ai-gules.vercel.app and sign in. You land on the dashboard.
2. Say: "TaskPilot turns one plain sentence into an organised plan — tasks, deadlines and
   priorities — and nothing is saved until I approve it."
3. Point at the dashboard numbers (Total / Pending / High Priority / completion). Say:
   "Every number here is calculated live from my saved tasks in the database — no
   placeholders, no hardcoded stats."

### 0:30–1:30 — AI Capture (the centrepiece)
1. Open **AI Capture**.
2. Type this sentence exactly:

   > Finish my Java assignment by tomorrow, revise machine learning on Sunday, and submit my project next Friday. Java is the most urgent.

3. Run the extraction. Point at the result: three separate tasks, each with a resolved date
   (tomorrow, Sunday, next Friday), a category, and a priority — the Java assignment is
   flagged most urgent, with the reason shown.
4. Point at the approval step and say the rule out loud: **"Nothing is saved until I review
   and approve each task."** Edit one title or date to show it's fully editable, then approve.

### 1:30–2:15 — The same data, three views
1. **My Tasks:** show the approved tasks in the list. Use the status/priority filter and the
   search box — point out the list is queried from the backend, per account.
2. **Board:** show the same tasks grouped by status; move one card.
3. **Calendar:** show the deadlines sitting on their dates. Say: "One dataset, three views —
   they can't disagree, because there's a single source of truth in the database."

### 2:15–2:45 — Notifications, insights, settings
1. Open **Notifications**: deadline notifications (overdue / due today / due tomorrow) are
   generated from real task due dates, deduplicated so the same reminder never appears twice.
2. Back on the dashboard, point at **Today's Focus** — tasks are scored (urgency, due date)
   so the most important thing surfaces first, and at the weekly productivity view.
3. If time allows, open **Settings**: show the deadline-reminders toggle and the password
   change — real account controls, not decoration.

### 2:45–3:00 — Close
1. Sign out and sign back in (or just reload). Say: "Everything persists, because it's a real
   server application — Spring Boot, JWT sessions, PostgreSQL — not a front-end demo."
2. Closing line: "The code, the CI, and this live link are all in the GitHub repo — the
   README's Live Demo section is exactly what you just saw."

---

## Hard questions — and honest answers

### "How does the AI extraction actually work?"
Be precise; do not oversell it as machine learning.
- Extraction is **rule-based NLP**, labelled as such in the README on purpose: text
  segmentation splits one instruction into tasks; date parsing resolves relative dates
  ("tomorrow", "next Friday"); keyword classification assigns categories; a scoring step
  assigns priorities and can explain them ("priority raised because you marked it most
  urgent"). Each task carries a confidence value.
- There is a **fallback chain**: the Java backend first calls the Python NLP service
  (`backend-ai/`, configured via the `AI_SERVICE_URL` environment variable); if that service
  is unavailable, a built-in Java parser implementing the same rules takes over, so capture
  never breaks. In the current production deployment the Python service is not running, so
  live extraction is served by the Java fallback — the API response says so honestly
  (`engine: local-fallback`).
- The planned next step (roadmap Phase 2) is an LLM extraction path (Gemini) behind the same
  review-and-approve flow, keeping the rule engine as fallback — planned, not yet live.
- Whatever the engine, the safety rule never changes: **extracted tasks are drafts until the
  user approves them.**

### "How do you know one user can't see another user's tasks?"
- Every account is a row in the database; passwords are stored **BCrypt-hashed**, never in
  plain text. Login returns a **JWT** session token.
- Every task query is scoped to the authenticated user (queries are `findByUser…` /
  `WHERE user = :user` style) — the API never returns unscoped task lists, and requests
  without a token get **401 "Authentication required"**, as do wrong passwords.
- This was **proven on the live deployment**, not just read in code: a second test account
  was registered and its task list was empty while the first account's tasks existed.

### "What broke in production, and how did you fix it?" (STAR)
- **Situation:** The app worked locally against MySQL. After deploying the backend to
  Railway with PostgreSQL (Supabase), creating tasks and logging in worked — but the task
  list, the core screen, failed.
- **Task:** Find the root cause from the live error without breaking the running service.
- **Action:** The API error showed PostgreSQL rejecting the generated SQL:
  `function lower(bytea) does not exist`. The task-search JPQL used a nullable `:search`
  parameter inside `LOWER(...)`; when no search text is given, Hibernate binds that null
  String parameter as **bytea** on PostgreSQL (MySQL had silently tolerated it), so
  `lower(bytea)` doesn't exist and the whole query failed. The fix was one line: cast the
  parameter in the query — `CAST(:search AS string)` — in `TaskRepository.searchTasks`.
- **Result:** Pushed as commit **6373d20**; Railway auto-redeployed, and the full
  end-to-end test was re-run on the live site: register → login → create task → task
  appears in the list → dashboard counts correct. Lesson worth saying out loud: "works on
  MySQL" is not "works on PostgreSQL" — null parameter typing differs between databases,
  and only testing against the real production database caught it.

### "Are the dashboard numbers real?"
Yes — totals, overdue, due-today and weekly completions are computed from the account's
tasks at request time (`DashboardService`), and Today's Focus uses a scoring utility
(`ScoringUtil`). Deeper insights (multi-week trends, per-category breakdown, streaks) are
planned (roadmap Phase 6), not yet built.

---

## What's still honest (don't claim these as done)

- **Reminders:** notification *generation* from due dates exists and is deduplicated, and
  the reminders on/off preference is stored — but there is **no scheduler** in the backend
  (no `@Scheduled` anywhere), so notifications are generated when the app refreshes them,
  not fired while the user is away. Scheduled reminders = **planned, Phase 1**.
- **AI engine in production:** the Python NLP service exists in the repo but is **not
  deployed**; production extraction currently runs on the Java fallback. Deploying it
  (`AI_SERVICE_URL`) = **planned, Phase 2 step 1**. The LLM path = **planned, Phase 2
  step 2**, pending a decision.
- **Security hardening:** BCrypt, JWT and per-account isolation are live and tested.
  Tightening CORS (currently permissive) and adding login rate-limiting = **planned,
  Phase 3**.
- **Mobile:** the layout has some responsive CSS but is desktop-first and has not passed a
  real-phone journey test = **planned, Phase 4**.
- **Animations:** only basic CSS transitions today; the motion pass = **planned, Phase 5**.
- **Insights:** real dashboard counts and Today's Focus scoring are live; trends,
  per-category breakdown and streaks = **planned, Phase 6**.
- **CI:** a GitHub Actions workflow (`.github/workflows/ci.yml` — backend `mvn -B verify`
  on Java 21, frontend `npm ci` + `npm run build` on Node 20) and a README status badge
  have been prepared. The badge only becomes meaningful once the workflow is pushed and
  its first run passes — check it is green before the interview.
- **Hosting:** the backend runs on Railway's $5 free trial started 18 Nov 2026; a funding
  or migration decision is due before the trial ends (~18 Dec 2026) — **Phase 0**.
