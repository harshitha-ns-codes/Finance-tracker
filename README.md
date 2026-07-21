# Finbrain

**Finbrain** is a full-stack personal finance platform that helps users track spending, plan budgets, forecast cash flow, and make smarter purchase decisions — with AI-powered advisory insights.

Built with Spring Boot and React, designed for production deployment on **Render** (backend) and **Vercel** (frontend).

---

## Features

- JWT authentication (register / login)
- Transaction tracking with CSV export
- Monthly budgets with category-level spent tracking
- Dashboard analytics, trends, and anomaly detection
- Cash flow forecasting
- Financial health score and purchase advisor
- Bill splits, regret tracker, streaks, net worth, and savings goals

---

## Architecture

```
┌─────────────────┐         HTTPS          ┌──────────────────────┐
│  Vercel (SPA)   │ ─────────────────────► │  Render (Spring Boot) │
│  React + Vite   │    VITE_API_BASE_URL   │  REST API + JWT       │
└─────────────────┘                        └──────────┬───────────┘
                                                      │
                                                      ▼
                                           ┌──────────────────────┐
                                           │  PostgreSQL (Render)  │
                                           └──────────────────────┘
```

| Layer | Technology | Deploy target |
|-------|-----------|---------------|
| Frontend | React 18, TypeScript, Vite, Axios, Recharts | Vercel |
| Backend | Spring Boot 3.3, Spring Security, JPA | Render (Docker) |
| Database | H2 (dev), PostgreSQL (prod) | Render PostgreSQL |
| Auth | JWT (HS256), BCrypt passwords | — |

---

## Tech Stack

**Backend:** Java 17 · Spring Boot 3 · Spring Security · Spring Data JPA · PostgreSQL · H2 · JJWT · Bean Validation · Spring Actuator

**Frontend:** React 18 · TypeScript · Vite · React Router 6 · Axios · Recharts

**DevOps:** Docker · GitHub Actions · Render Blueprint · Vercel

---

## Folder Structure

```
Finance-tracker/
├── backend/                  # Spring Boot API
│   ├── src/main/java/        # Application source
│   ├── src/main/resources/   # application.properties + profiles
│   ├── Dockerfile            # Production multi-stage build
│   ├── .dockerignore
│   └── .env.example
├── frontend/                 # React SPA
│   ├── src/                  # Components, pages, API client
│   ├── vercel.json           # Vercel SPA + cache headers
│   └── .env.example
├── .github/workflows/        # CI pipelines
├── render.yaml               # Render Blueprint (API + PostgreSQL)
├── DEPLOYMENT.md             # Step-by-step deploy guide
├── PROJECT_REVIEW.md         # Deployment audit findings
└── README.md
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 17+ |
| Node.js | 18+ (20 recommended) |
| npm | 9+ |
| Docker | 24+ (optional, for container builds) |
| Git | 2+ |

---

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-org/finance-tracker.git
cd finance-tracker
```

### 2. Backend

```bash
cd backend
cp .env.example .env          # optional — dev defaults work out of the box
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

- API: http://localhost:8081
- Health: http://localhost:8081/actuator/health
- H2 Console (dev only): http://localhost:8081/h2-console

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

- App: http://localhost:5174
- API calls proxy to `localhost:8081` via Vite

---

## Environment Variables

See [`backend/.env.example`](backend/.env.example) and [`frontend/.env.example`](frontend/.env.example).

**Backend (production):**

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` |
| `JWT_SECRET` | Yes | 32+ char random secret |
| `DATABASE_HOST/PORT/NAME/USER/PASSWORD` | Yes | PostgreSQL (auto-set by Render) |
| `CORS_ALLOWED_ORIGINS` | Yes | Your Vercel URL |
| `PORT` | Auto | Injected by Render |

**Frontend (production):**

| Variable | Required | Description |
|----------|----------|-------------|
| `VITE_API_BASE_URL` | Yes | e.g. `https://finbrain-api.onrender.com/api` |
| `VITE_APP_NAME` | No | Display name (default: Finbrain) |

---

## Building

### Backend JAR

```bash
cd backend
./mvnw clean package -DskipTests
# Output: backend/target/finance-tracker-0.0.1-SNAPSHOT.jar
```

### Frontend static bundle

```bash
cd frontend
npm run build
# Output: frontend/dist/
```

---

## Docker

Build and run the backend container locally:

```bash
cd backend
docker build -t finbrain-api .

docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e JWT_SECRET=local-dev-secret-at-least-32-characters \
  -e PORT=8081 \
  finbrain-api
```

Health check: http://localhost:8081/actuator/health

---

## Deployment

Full guides:

- **[DEPLOYMENT.md](DEPLOYMENT.md)** — step-by-step Render + Vercel instructions
- **[PROJECT_REVIEW.md](PROJECT_REVIEW.md)** — audit findings and known limitations
- **[render.yaml](render.yaml)** — one-click Render Blueprint

**Quick summary:**

1. Push to GitHub
2. Render → New Blueprint → select `render.yaml`
3. Set `CORS_ALLOWED_ORIGINS` and `FRONTEND_URL` to your Vercel URL
4. Vercel → Import repo → root directory `frontend`
5. Set `VITE_API_BASE_URL=https://your-api.onrender.com/api`

---

## CI/CD

GitHub Actions run on every push and pull request to `main` / `develop`:

| Workflow | What it does |
|----------|-------------|
| `backend-ci.yml` | `./mvnw clean test package` + Docker build |
| `frontend-ci.yml` | `npm ci` → `tsc --noEmit` → `vite build` |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| CORS errors in browser | Set `CORS_ALLOWED_ORIGINS` on Render to exact Vercel URL |
| 401 on all requests | Verify `JWT_SECRET` is set; log in again after deploy |
| DB connection failed | Check PostgreSQL is linked; verify `DATABASE_*` env vars |
| Blank page on refresh (Vercel) | Ensure `vercel.json` is in `frontend/` root |
| API calls wrong URL | Set `VITE_API_BASE_URL` on Vercel and redeploy |

See [DEPLOYMENT.md](DEPLOYMENT.md) for the full troubleshooting section.

---

## Security

- Never commit secrets — use Render/Vercel env vars or GitHub Secrets
- JWT secret: `openssl rand -base64 64`
- H2 console disabled in production
- Non-root Docker user
- CORS restricted to known frontend origins (no wildcards)
- Frontend exposes only `VITE_*` public variables

See [DEPLOYMENT.md — Security](DEPLOYMENT.md#security) for GitHub Secrets setup.

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

Ensure CI passes before requesting review.

---

## License

This project is provided as-is for educational and portfolio purposes. Add an open-source license (e.g. MIT) before public distribution.

---

## Author

Built as a portfolio-grade full-stack finance application demonstrating production deployment practices, JWT security, and modern React architecture.
