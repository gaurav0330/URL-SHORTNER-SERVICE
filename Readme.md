# ShortTo — URL Shortener Service

A production-grade URL shortener built with Spring Boot, MySQL, Redis and Docker.

---

## Tech Stack

- **Backend** — Java 17 + Spring Boot 3.5
- **Database** — MySQL 8.0
- **Cache** — Redis
- **Containerization** — Docker + Docker Compose
- **API Docs** — Swagger UI

---

## How to Run

### Option 1 — Docker (Recommended, One Command)

Make sure Docker Desktop is running, then:

```bash
# Clone the project
git clone https://github.com/gaurav0330/URL-SHORTNER-SERVICE.git
cd URL-SHORTNER-SERVICE

# Create .env file in project root
DB_USERNAME=root
DB_PASSWORD=root
REDIS_HOST=localhost
REDIS_PORT=6379
APP_BASE_URL=http://localhost:8080

# Start everything
docker-compose up --build
```

That's it. Spring Boot + MySQL + Redis all start automatically.

To stop:
```bash
docker-compose down
```

To stop and delete all data:
```bash
docker-compose down -v
```

---

### Option 2 — Run Locally (Manual Setup)

**Prerequisites:**
- Java 17
- Maven
- MySQL 8.0 running on port 3306
- Redis running on port 6379

**Step 1 — Create MySQL database:**
```sql
CREATE DATABASE urlshortener;
```

**Step 2 — Start Redis:**
```bash
docker run -d --name redis -p 6379:6379 redis:alpine
```

**Step 3 — Configure `application.properties`:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/urlshortener
spring.datasource.username=root
spring.datasource.password=yourpassword
```

**Step 4 — Run the app:**
```bash
mvn spring-boot:run
```

App starts at `http://localhost:8080`

---

## API Endpoints

### Base URL
```
http://localhost:8080
```

---

### 1. Shorten a URL

**POST** `/api/v1/urls`

Shortens a long URL and returns a short code.

**Request Body:**
```json
{
    "originalUrl": "https://www.google.com",
    "customAlias": "my-google",
    "expiresAt": "2027-01-01T00:00:00"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| originalUrl | String | ✅ Yes | Must start with http:// or https:// |
| customAlias | String | ❌ No | 3-30 chars, letters/numbers/hyphens only |
| expiresAt | DateTime | ❌ No | Must be a future date. Null = never expires |

**Success Response — 201 Created:**
```json
{
    "shortCode": "aB3xYz",
    "shortUrl": "http://localhost:8080/aB3xYz",
    "originalUrl": "https://www.google.com",
    "customAlias": "my-google",
    "clickCount": 0,
    "createdAt": "2026-04-05T23:01:22",
    "expiresAt": "2027-01-01T00:00:00"
}
```

**Error Responses:**

| Status | Reason |
|---|---|
| 400 | Empty URL, invalid format, alias too short/long |
| 400 | Custom alias already taken |
| 429 | Too many requests (rate limit exceeded) |

**Examples:**

Shorten without alias:
```json
{
    "originalUrl": "https://www.youtube.com/watch?v=abc123"
}
```

Shorten with custom alias:
```json
{
    "originalUrl": "https://www.youtube.com/watch?v=abc123",
    "customAlias": "my-video"
}
```

Shorten with expiry:
```json
{
    "originalUrl": "https://www.youtube.com/watch?v=abc123",
    "expiresAt": "2026-12-31T23:59:59"
}
```

---

### 2. Redirect to Original URL

**GET** `/{shortCode}`

Redirects the user to the original URL. Also increments click count.

**Example:**
```
GET http://localhost:8080/aB3xYz
→ 302 redirect to https://www.google.com
```

**Responses:**

| Status | Meaning |
|---|---|
| 302 Found | Redirects to original URL |
| 404 Not Found | Short code does not exist |
| 410 Gone | Short URL has expired |

**Try it in browser:**
Just paste `http://localhost:8080/aB3xYz` in your browser — it will redirect automatically.

---

### 3. Get URL Stats

**GET** `/api/v1/urls/{shortCode}/stats`

Returns metadata and click count for a short URL.

**Example:**
```
GET http://localhost:8080/api/v1/urls/aB3xYz/stats
```

**Success Response — 200 OK:**
```json
{
    "shortCode": "aB3xYz",
    "shortUrl": "http://localhost:8080/aB3xYz",
    "originalUrl": "https://www.google.com",
    "customAlias": null,
    "clickCount": 42,
    "createdAt": "2026-04-05T23:01:22",
    "expiresAt": null
}
```

**Error Responses:**

| Status | Reason |
|---|---|
| 404 | Short code not found or inactive |

---

## Features Explained

### ✅ Auto-Generated Short Codes
- 6-character alphanumeric codes (a-z, A-Z, 0-9)
- 62^6 = 56 billion possible combinations
- Cryptographically random — impossible to predict

### ✅ Custom Aliases
- Users can choose their own short code e.g. `/my-video`
- Uniqueness validated before saving
- 3-30 characters, letters/numbers/hyphens/underscores only

### ✅ URL Expiry
- Optional expiry date on any short URL
- Expired URLs return `410 Gone` (not 404)
- Expired URLs automatically deactivated in DB
- Cache cleared on expiry

### ✅ Click Analytics
- Every redirect increments click count
- Counts tracked in Redis for performance
- Flushed to MySQL every 5 minutes in batch
- View stats anytime via `/stats` endpoint

### ✅ Redis Caching
- First redirect → fetches from MySQL, stores in Redis
- All subsequent redirects → served from Redis (no DB hit)
- Cache TTL: 60 minutes
- 100x faster response time vs direct DB lookup

### ✅ Rate Limiting
- Max 10 requests per minute per IP address
- Tracked in Redis with automatic TTL reset
- Returns `429 Too Many Requests` when exceeded

### ✅ Soft Delete
- URLs are never hard deleted from DB
- `is_active = false` marks them as inactive
- Preserves analytics history and audit trail

### ✅ Input Validation
- URL must start with http:// or https://
- Custom alias: only safe characters allowed
- Expiry date must be in the future
- All errors return clean JSON (never HTML)

---

## Error Response Format

All errors return consistent JSON:

```json
{
    "status": 404,
    "error": "Not Found",
    "message": "Short URL not found: fakecode",
    "path": "/fakecode",
    "timestamp": "2026-04-05T23:01:22"
}
```

| Status Code | Meaning |
|---|---|
| 400 | Bad Request — invalid input |
| 404 | Not Found — short code doesn't exist |
| 410 | Gone — URL has expired |
| 429 | Too Many Requests — rate limit hit |
| 500 | Internal Server Error — unexpected error |

---

## Swagger UI — Interactive API Docs

After starting the app, open in browser:
```
http://localhost:8080/swagger-ui.html
```

You can test all endpoints directly from the browser — no Postman needed.

---

## Project Structure

```
src/main/java/com/shortTo/urlshortener/
├── config/
│   ├── RedisConfig.java           → Redis connection setup
│   └── SwaggerConfig.java         → API documentation setup
├── controller/
│   ├── UrlController.java         → POST shorten, GET stats
│   └── RedirectController.java    → GET /{shortCode} redirect
├── dto/
│   ├── UrlRequestDto.java         → input validation
│   ├── UrlResponseDto.java        → success response shape
│   └── ErrorResponseDto.java      → error response shape
├── exception/
│   ├── BadRequestException.java
│   ├── ResourceNotFoundException.java
│   ├── RateLimitException.java
│   ├── UrlExpiredException.java
│   └── GlobalExceptionHandler.java → catches all exceptions
├── model/
│   └── UrlMapping.java            → DB table definition
├── repository/
│   └── UrlRepository.java         → DB operations
├── service/
│   ├── UrlService.java            → core business logic
│   ├── RateLimiterService.java    → rate limiting logic
│   └── ClickFlushService.java     → async click count flusher
└── UrlshortenerApplication.java
```

---

## Testing with Postman

**Import this collection of requests:**

**1. Shorten a URL:**
- Method: POST
- URL: `http://localhost:8080/api/v1/urls`
- Headers: `Content-Type: application/json`
- Body: `{ "originalUrl": "https://www.google.com" }`

**2. Shorten with custom alias:**
- Method: POST
- URL: `http://localhost:8080/api/v1/urls`
- Body: `{ "originalUrl": "https://www.google.com", "customAlias": "google" }`

**3. Redirect:**
- Method: GET
- URL: `http://localhost:8080/{shortCode}`

**4. Get stats:**
- Method: GET
- URL: `http://localhost:8080/api/v1/urls/{shortCode}/stats`

**5. Test rate limiting** — send POST 11 times rapidly → 11th returns 429

**6. Test expiry** — create with past date → visit → returns 410

**7. Test validation** — send empty URL → returns 400 with message

---

## GitHub

```
https://github.com/gaurav0330/URL-SHORTNER-SERVICE
```