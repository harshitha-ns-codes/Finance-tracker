# Finbrain — Project Deployment Review

Audit of deployment readiness conducted as part of production hardening.

**Date:** 2026-07-21  
**Scope:** Backend, frontend, Docker, CI/CD, Render, Vercel

---

## Summary

| Area | Status | Notes |
|------|--------|-------|
| Hardcoded localhost | ✅ Resolved | Only in dev defaults and docs |
| Hardcoded credentials | ✅ Resolved | All via env vars / profiles |
| Environment variables | ✅ Complete | `.env.example` for both apps |
| Docker | ✅ Complete | Multi-stage, non-root user |
| Render Blueprint | ✅ Complete | `render.yaml` with PostgreSQL |
| Vercel SPA routing | ✅ Complete | `vercel.json` with headers |
| CI/CD | ✅ Complete | GitHub Actions for both apps |
| Health checks | ✅ Complete | `/actuator/health` |
| CORS | ✅ Complete | Env-driven, no wildcards |
| JWT security | ✅ Complete | Required in prod, no default |

---

## Findings

### ✅ Resolved issues

1. **Hardcoded CORS origins** — moved to `app.cors.allowed-origins` env var
2. **Hardcoded JWT secret default in prod** — prod profile requires `JWT_SECRET`
3. **Fixed server port** — uses `${PORT}` from Render
4. **Frontend API URL** — uses `VITE_API_BASE_URL` via Axios client
5. **No Docker artifacts** — Dockerfile + .dockerignore created
6. **No CI pipeline** — GitHub Actions added
7. **No SPA routing on Vercel** — vercel.json added
8. **H2 in production** — disabled via `application-prod.properties`

### ⚠️ Known limitations (not blockers)

| Item | Impact | Recommendation |
|------|--------|----------------|
| No backend unit tests | CI runs `mvn test` with 0 tests | Add integration tests over time |
| `ddl-auto=update` in prod | Schema managed by Hibernate, not migrations | Add Flyway/Liquibase before scaling |
| No email integration | MAIL_* env vars unused | Wire up when notification feature added |
| No file uploads | CLOUDINARY_* env vars unused | Wire up when upload feature added |
| Render free tier cold starts | 30–60s delay after idle | Upgrade to paid plan for production traffic |
| Single JAR monolith | No horizontal scaling config yet | Add session-less JWT already supports scaling |

### 🔍 Remaining localhost references (acceptable)

| Location | Context | Action |
|----------|---------|--------|
| `application-dev.properties` | Dev CORS defaults | Keep — dev only |
| `vite.config.mts` | Dev proxy target | Keep — dev only |
| `README.md`, `DEPLOYMENT.md` | Documentation examples | Keep |
| `backend/README.md` | Local dev instructions | Keep |

### 🔍 No issues found

- No absolute OS-specific paths in application code
- No secrets in frontend source
- No wildcard CORS origins
- No `.env` files committed (gitignored)
- Docker image uses non-root user
- Actuator health exposed without sensitive details in prod

---

## Docker image analysis

| Metric | Value |
|--------|-------|
| Base image | `eclipse-temurin:17-jre-alpine` (~80 MB base) |
| Build stage | `maven:3.9-eclipse-temurin-17-alpine` (discarded) |
| Layers cached | pom.xml + dependencies separated from source |
| Runtime user | `finbrain` (non-root) |
| Artifacts copied | JAR only |

**Estimated final image size:** ~200–250 MB (JRE + JAR + curl)

---

## Render deployment considerations

1. **DATABASE_* vars** — `render.yaml` links PostgreSQL automatically
2. **JWT_SECRET** — auto-generated on first Blueprint deploy
3. **Health check** — `/actuator/health` with 90s start period for cold starts
4. **PORT** — Render injects automatically; Spring reads `${PORT}`
5. **Manual step** — set `CORS_ALLOWED_ORIGINS` after Vercel deploy

---

## Vercel deployment considerations

1. **Root directory** must be `frontend`
2. **VITE_API_BASE_URL** must include `/api` suffix
3. **vercel.json** handles SPA rewrites for React Router
4. **Static assets** cached with immutable headers
5. **No server-side secrets** — all API auth via JWT in browser

---

## Recommended next steps (post-deploy)

1. Add Flyway database migrations
2. Add backend integration tests
3. Set up Sentry or similar error monitoring
4. Configure custom domain + SSL (optional — included by default)
5. Upgrade Render from free tier for production traffic
6. Add rate limiting on auth endpoints

---

## Sign-off checklist

- [x] Backend builds: `./mvnw clean package`
- [x] Frontend builds: `npm run build`
- [x] Docker builds: `docker build ./backend`
- [x] Health endpoint configured
- [x] Environment variables documented
- [x] Secrets not in source control
- [x] CORS production-ready
- [x] CI workflows created
- [x] Deployment guide written
