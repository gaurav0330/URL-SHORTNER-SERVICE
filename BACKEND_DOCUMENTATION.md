# ShortTo Backend Documentation

Comprehensive backend guide for the URL shortener service built with Spring Boot, MySQL, Redis, and JWT auth.

## 1) Project Overview

The backend provides:
- User registration and login with JWT authentication.
- URL shortening with optional custom alias, expiry, category, and password protection.
- Redirect handling with secure unlock flow for protected links.
- Analytics tracking (country, city, browser, device, click trend).
- Rate limiting and Redis-assisted performance optimizations.

## 2) Tech Stack

- Java 17
- Spring Boot 3.5.x
- Spring Security (JWT)
- Spring Data JPA (MySQL)
- Spring Data Redis
- Spring Scheduler (`@Scheduled`) for click flush batching
- OpenAPI/Swagger (`springdoc`)
- Docker + Docker Compose

## 3) Core Architecture

- **Controllers** expose REST endpoints.
- **Services** contain business logic.
- **Repositories** handle persistence (MySQL).
- **Redis** is used for:
  - URL destination cache
  - Click counters before periodic DB flush
  - Rate limiting counters by IP

Data flow on redirect:
1. Resolve short code.
2. Validate active/expiry/password.
3. Redirect to original URL.
4. Increment click count in Redis.
5. Persist click logs (analytics service, async).
6. Scheduled job flushes click aggregates to MySQL.

## 4) Feature Matrix

- JWT auth (register/login)
- URL create/list/update/delete (soft delete)
- Custom alias support
- Expiration support
- Password-protected links
- Redirect with unlock flow
- Dashboard analytics endpoint
- Rate limiting on URL creation endpoint
- Swagger API docs

## 5) API Summary

Base URL (local): `http://localhost:8080`

Public endpoints:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /{shortCode}` (redirect/unlock redirect path)
- Swagger docs endpoints

Authenticated endpoints:
- `POST /api/v1/urls`
- `GET /api/v1/urls`
- `GET /api/v1/urls/{shortCode}/stats`
- `PUT /api/v1/urls/{id}`
- `DELETE /api/v1/urls/{id}`
- `GET /api/v1/analytics/{shortCode}`

## 6) Environment and Config

Main config: `src/main/resources/application.properties`

Important properties:
- `server.port` (default `8080`)
- `spring.datasource.*` (MySQL)
- `spring.data.redis.*` (Redis)
- `app.base-url` (short URL host)
- `app.jwt.secret`
- `app.jwt.expiration-ms`
- `app.rate-limit.max-requests`
- `app.rate-limit.window-minutes`
- `app.cache.ttl-minutes`
- `app.click-flush.interval-ms`
- `app.frontend-url` (used by protected redirect to unlock page)

Profiles:
- `dev` for local development
- `prod` for production tuning
- `docker` for Docker Compose environment

## 7) How to Run

### Option A: Docker Compose (recommended)

From backend root:

```bash
docker-compose up --build
```

Services started:
- MySQL (`3307 -> 3306` container mapping)
- Redis (`6379`)
- Backend app (`8080`)

Stop:

```bash
docker-compose down
```

Remove volumes too:

```bash
docker-compose down -v
```

### Option B: Local runtime

Prerequisites:
- Java 17
- MySQL running and DB `urlshortener` created
- Redis running on `localhost:6379`

Run:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## 8) Production-Readiness Checklist

Before production deployment:

- Replace default JWT secret with a strong secret manager-backed value.
- Configure CORS to explicit frontend domains (avoid broad localhost-only assumptions).
- Ensure `APP_BASE_URL` and `app.frontend-url` are correct public URLs.
- Use managed MySQL + Redis with backups and failover.
- Enable HTTPS and place app behind reverse proxy/load balancer.
- Set proper logging level and centralized log aggregation.
- Add health probes and resource limits.
- Add monitoring/alerts (latency, 5xx, Redis availability, DB connections).
- Verify scheduler interval and batch size for click flush under expected load.
- Run DB migrations strategy (Flyway/Liquibase recommended for controlled schema evolution).

## 9) Important Edge Cases and Behavior

- **Expired URLs**: return `410 Gone`; URL marked inactive.
- **Inactive/unknown URLs**: return `404`.
- **Password-protected links**:
  - Without password or wrong password, redirect endpoint routes to frontend unlock page.
  - Correct password query param (`?p=...`) unlocks and redirects.
- **Alias collisions**: rejected with `400`.
- **Invalid URL format**: rejected by validation.
- **Rate limit exceeded**: `429 Too Many Requests`.
- **Token expiry/invalid token**: protected endpoints denied (`401/403` depending on context).
- **Soft delete**: does not remove historical records; marks URL inactive.
- **Redis down**: can impact cache/rate-limit/click aggregation paths; production should include fallback strategy and observability.

## 10) Performance and Scaling Strategy

### Horizontal API scaling
- Run multiple stateless backend instances behind a load balancer.
- Keep session state out of app memory (already JWT-based stateless auth).
- Externalize Redis and MySQL.

### Database scaling
- Add indexes for high-cardinality query paths (`shortCode`, user scoped lookups, analytics query fields).
- Use read replicas for analytics-heavy workloads.
- Partition/retention strategy for click logs as volume grows.

### Redis scaling
- Move to Redis cluster/sentinel for HA.
- Separate keyspaces or instances for cache vs rate-limit vs counters if needed.
- Track memory and key expiry patterns.

### Background jobs
- Tune `app.click-flush.interval-ms` for write amplification vs freshness tradeoff.
- Consider queue/stream-based ingestion for very high click throughput.

### Analytics scaling
- Current synchronous geolocation call per click may become bottleneck.
- Move geo enrichment to async pipeline and/or cached geo lookup by IP prefix.

## 11) Security Notes

- Enforce strong password policy for users.
- Consider hashing + salting for per-link passwords (already encoded via `PasswordEncoder`).
- Add API abuse protection beyond create endpoint if needed.
- Add audit logging for sensitive operations.
- Add optional email verification / MFA for accounts.

## 12) Testing Recommendations

- Unit tests for service layer logic (URL creation/update/expiry/password checks).
- Integration tests for auth + protected APIs.
- End-to-end tests for redirect/unlock flow.
- Load tests:
  - Redirect throughput
  - Rate limiting behavior under burst traffic
  - Click flush behavior under sustained load

## 13) Known Improvement Opportunities

- Standardize all auth failures to a single status strategy where appropriate.
- Add migration framework (Flyway/Liquibase).
- Add fallback behavior for analytics geolocation dependency failures.
- Add idempotency and request tracing for production observability.

