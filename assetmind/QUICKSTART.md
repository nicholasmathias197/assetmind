# Quick Start Guide: AssetMind Backend

## Build & Run

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8.0+ (or use defaults; tests use H2 in-memory)

### Local Development
```bash
# From the assetmind/ project directory
cd /c/Users/nicholas.mathias/IdeaProjects/assetmind/assetmind

# Build all modules (skip tests for speed)
mvn clean install -DskipTests

# Start the application (spring-boot:run now targets only the application module)
mvn -pl assetmind-application -am spring-boot:run
```

Server listens on `http://localhost:8080`

---

## AI Features Setup (Groq — Active by Default)

AssetMind uses **Groq** (free tier, OpenAI-compatible) to power the
`/depreciation/ai-run`, `/depreciation/recommend`, `/tax-strategy/recommend`,
and `/classification/suggest` endpoints.

**The Groq API key is already embedded in `application.yml`** — no environment
variable or extra setup required.  Just start the app and AI is live.

Responses from the AI endpoints will show `"aiSource": "AI_GROQ"` (model: `llama-3.3-70b-versatile`).  
If the Groq API is unreachable for any reason the service automatically falls back to the built-in
rule engine and the response will show `"aiSource": "RULE_FALLBACK"`.

### Override the Key (optional)
If you want to rotate to a different key without touching the source:
```bash
export GROQ_API_KEY=gsk_your_new_key_here
mvn -pl assetmind-application -am spring-boot:run
```

### Verify the Key Works
```bash
bash /c/Users/nicholas.mathias/IdeaProjects/assetmind/run-groq-ai-check.sh
```

---

## Authentication (JWT)

All `/api/v1/**` endpoints (except `/api/v1/auth/**`) require a JWT Bearer token.

### 1. Register a New User
```bash
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123",
    "email": "john@example.com"
  }'
```

### 2. Login to Get Tokens
```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 3. Save Token for Use
```bash
export TOKEN="<accessToken from login response>"
```

---

## Asset Management

### Create an Asset
```bash
curl -X POST "http://localhost:8080/api/v1/assets" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "id": "laptop-001",
    "description": "Dell XPS 15",
    "assetClass": "COMPUTER_EQUIPMENT",
    "costBasis": 1500.00,
    "inServiceDate": "2026-03-26",
    "usefulLifeYears": 5
  }'
```

### List Assets with Pagination
```bash
# First page (default 20 per page)
curl "http://localhost:8080/api/v1/assets/page" \
  -H "Authorization: Bearer $TOKEN"

# Page 1, 10 per page, sorted by description descending
curl "http://localhost:8080/api/v1/assets/page?page=1&size=10&sortBy=description&sortDirection=desc" \
  -H "Authorization: Bearer $TOKEN"

# Filter by asset class
curl "http://localhost:8080/api/v1/assets/page?assetClass=COMPUTER_EQUIPMENT" \
  -H "Authorization: Bearer $TOKEN"
```

### Update an Asset
```bash
curl -X PUT "http://localhost:8080/api/v1/assets/laptop-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "id": "laptop-001",
    "description": "Dell XPS 15 (upgraded SSD)",
    "assetClass": "COMPUTER_EQUIPMENT",
    "costBasis": 1600.00,
    "inServiceDate": "2026-03-26",
    "usefulLifeYears": 5
  }'
```

### Soft-Delete an Asset
```bash
curl -X DELETE "http://localhost:8080/api/v1/assets/laptop-001" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Depreciation

### Option 1 — Manual: supply every parameter yourself
`POST /api/v1/depreciation/run`

You choose the depreciation method and useful life explicitly.

```bash
curl -X POST "http://localhost:8080/api/v1/depreciation/run" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "assetId": "laptop-001",
    "bookType": "TAX",
    "method": "STRAIGHT_LINE",
    "assetClass": "COMPUTER_EQUIPMENT",
    "inServiceDate": "2026-01-01",
    "costBasis": 10000.00,
    "salvageValue": 1000.00,
    "usefulLifeYears": 5,
    "section179Enabled": false,
    "section179Amount": 0,
    "bonusDepreciationRate": 0
  }'
```

**Valid `method` values:** `STRAIGHT_LINE` · `MACRS_200DB_HY` · `ADS_STRAIGHT_LINE`  
**Valid `bookType` values:** `BOOK` · `TAX` · `STATE`  
**Valid `assetClass` values:** `COMPUTER_EQUIPMENT` · `FURNITURE` · `LEASEHOLD_IMPROVEMENT` · `BUILDING_IMPROVEMENT` · `VEHICLE` · `OTHER`

**Example response (5 schedule lines):**
```json
{
  "schedules": [
    {
      "yearNumber": 1,
      "beginningBookValue": 10000.00,
      "depreciationExpense": 1800.00,
      "endingBookValue": 8200.00,
      "explanation": "Year 1 straight-line depreciation"
    },
    {
      "yearNumber": 2,
      "beginningBookValue": 8200.00,
      "depreciationExpense": 1640.00,
      "endingBookValue": 6560.00,
      "explanation": "Year 2 straight-line depreciation"
    },
    {
      "yearNumber": 3,
      "beginningBookValue": 6560.00,
      "depreciationExpense": 1312.00,
      "endingBookValue": 5248.00,
      "explanation": "Year 3 straight-line depreciation"
    },
    {
      "yearNumber": 4,
      "beginningBookValue": 5248.00,
      "depreciationExpense": 1049.60,
      "endingBookValue": 4198.40,
      "explanation": "Year 4 straight-line depreciation"
    },
    {
      "yearNumber": 5,
      "beginningBookValue": 4198.40,
      "depreciationExpense": 839.68,
      "endingBookValue": 3358.72,
      "explanation": "Year 5 straight-line depreciation"
    }
  ]
}
```

---

### Option 2 — AI Recommend only: get method + useful life, no schedule
`POST /api/v1/depreciation/recommend`

Ask the AI what method and useful life to use — useful when you want to inspect
the recommendation before running a schedule.

```bash
curl -X POST "http://localhost:8080/api/v1/depreciation/recommend" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "stateCode": "CA",
    "equipmentType": "Dell XPS 15 laptop",
    "assetClass": "COMPUTER_EQUIPMENT",
    "immediateDeductionPreferred": true,
    "longHorizonAsset": false
  }'
```

**Example response:**
```json
{
  "recommendedMethod": "MACRS_200DB_HY",
  "suggestedUsefulLifeYears": 5,
  "confidence": 0.92,
  "rationale": "MACRS 200DB is optimal for 5-year IT property placed in service in California",
  "source": "AI_GROQ"
}
```

> **`source`** will be `"RULE_FALLBACK"` when no `GROQ_API_KEY` is set.

---

### Option 3 — AI Run: AI picks the method, engine runs the numbers ✨
`POST /api/v1/depreciation/ai-run`

**This is the recommended endpoint for most use-cases.**  
You supply the asset cost and context; the AI selects the depreciation method
and useful-life years, then the engine immediately computes the full dollar
schedule — all in one call.

```bash
curl -X POST "http://localhost:8080/api/v1/depreciation/ai-run" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "assetId": "laptop-001",
    "stateCode": "CA",
    "equipmentType": "Dell XPS 15 laptop",
    "assetClass": "COMPUTER_EQUIPMENT",
    "bookType": "TAX",
    "inServiceDate": "2026-01-01",
    "costBasis": 10000.00,
    "salvageValue": 0,
    "section179Enabled": false,
    "section179Amount": 0,
    "bonusDepreciationRate": 0,
    "immediateDeductionPreferred": true,
    "longHorizonAsset": false
  }'
```

**Example response:**
```json
{
  "recommendedMethod": "MACRS_200DB_HY",
  "suggestedUsefulLifeYears": 5,
  "aiConfidence": 0.92,
  "aiRationale": "MACRS 200DB is optimal for 5-year IT property placed in service in California",
  "aiSource": "AI_GROQ",
  "schedule": [
    {
      "yearNumber": 1,
      "beginningBookValue": 10000.00,
      "depreciationExpense": 4000.00,
      "endingBookValue": 6000.00,
      "explanation": "Year 1 MACRS 200DB half-year convention"
    },
    {
      "yearNumber": 2,
      "beginningBookValue": 6000.00,
      "depreciationExpense": 2400.00,
      "endingBookValue": 3600.00,
      "explanation": "Year 2 MACRS 200DB"
    },
    {
      "yearNumber": 3,
      "beginningBookValue": 3600.00,
      "depreciationExpense": 1440.00,
      "endingBookValue": 2160.00,
      "explanation": "Year 3 MACRS 200DB"
    },
    {
      "yearNumber": 4,
      "beginningBookValue": 2160.00,
      "depreciationExpense": 864.00,
      "endingBookValue": 1296.00,
      "explanation": "Year 4 MACRS 200DB"
    },
    {
      "yearNumber": 5,
      "beginningBookValue": 1296.00,
      "depreciationExpense": 648.00,
      "endingBookValue": 648.00,
      "explanation": "Year 5 MACRS 200DB half-year convention"
    }
  ]
}
```

#### Postman Body — other asset class examples

**Office furniture (straight-line, 7 years):**
```json
{
  "assetId": "furniture-001",
  "stateCode": "NY",
  "equipmentType": "Herman Miller Aeron chair",
  "assetClass": "FURNITURE",
  "bookType": "BOOK",
  "inServiceDate": "2026-01-01",
  "costBasis": 2000.00,
  "salvageValue": 200.00,
  "section179Enabled": false,
  "section179Amount": 0,
  "bonusDepreciationRate": 0,
  "immediateDeductionPreferred": false,
  "longHorizonAsset": false
}
```

**Delivery van (MACRS, 5 years):**
```json
{
  "assetId": "van-001",
  "stateCode": "TX",
  "equipmentType": "Ford Transit cargo van",
  "assetClass": "VEHICLE",
  "bookType": "TAX",
  "inServiceDate": "2026-01-01",
  "costBasis": 45000.00,
  "salvageValue": 5000.00,
  "section179Enabled": false,
  "section179Amount": 0,
  "bonusDepreciationRate": 0,
  "immediateDeductionPreferred": true,
  "longHorizonAsset": false
}
```

**Leasehold improvement (ADS, 15 years, long horizon):**
```json
{
  "assetId": "leasehold-001",
  "stateCode": "IL",
  "equipmentType": "Office floor interior renovation",
  "assetClass": "LEASEHOLD_IMPROVEMENT",
  "bookType": "TAX",
  "inServiceDate": "2026-01-01",
  "costBasis": 120000.00,
  "salvageValue": 0,
  "section179Enabled": false,
  "section179Amount": 0,
  "bonusDepreciationRate": 0,
  "immediateDeductionPreferred": false,
  "longHorizonAsset": true
}
```

---

## AI Asset Classification
`POST /api/v1/classification/suggest`

Paste invoice or purchase-order text; the AI classifies the asset and returns
the GL code, useful life, and confidence score.

```bash
curl -X POST "http://localhost:8080/api/v1/classification/suggest" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "documentText": "Invoice: Dell XPS 15 laptop, 32GB RAM, 1TB SSD — purchased for the accounting team"
  }'
```

**Example response:**
```json
{
  "assetClass": "COMPUTER_EQUIPMENT",
  "glCode": "1610",
  "usefulLifeYears": 5,
  "confidence": 0.97,
  "rationale": "Detected laptop/computer equipment from invoice description"
}
```

**Postman body — other classification examples:**

| documentText snippet | Expected `assetClass` | `glCode` |
|---|---|---|
| `"Standing desk and ergonomic chair"` | `FURNITURE` | `1620` |
| `"Ford Transit cargo van for field team"` | `VEHICLE` | `1630` |
| `"Tenant improvement — floor 4 interior fit-out"` | `LEASEHOLD_IMPROVEMENT` | `1710` |
| `"Roof replacement and HVAC system upgrade"` | `BUILDING_IMPROVEMENT` | `1720` |
| `"Miscellaneous office supplies"` | `OTHER` | `1699` |

---

## AI Tax Strategy Recommendation
`POST /api/v1/tax-strategy/recommend`

Given the state and equipment context, returns the optimal depreciation strategy.

```bash
curl -X POST "http://localhost:8080/api/v1/tax-strategy/recommend" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "stateCode": "CA",
    "equipmentType": "server rack",
    "immediateDeductionPreferred": true,
    "longHorizonAsset": false
  }'
```

**Example response:**
```json
{
  "recommendedMethod": "MACRS_200DB_HY",
  "confidence": 0.91,
  "rationale": "MACRS accelerated depreciation maximises early deductions for IT equipment in California",
  "source": "AI_GROQ"
}
```

---

## Key Endpoints

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST | `/api/v1/auth/register` | ❌ | Register new user |
| POST | `/api/v1/auth/login` | ❌ | Login, returns JWT |
| POST | `/api/v1/auth/refresh` | ❌ | Refresh expired access token |
| POST | `/api/v1/assets` | ✅ | Create asset |
| GET | `/api/v1/assets/{id}` | ✅ | Get single asset |
| PUT | `/api/v1/assets/{id}` | ✅ | Update asset |
| DELETE | `/api/v1/assets/{id}` | ✅ | Soft-delete asset |
| GET | `/api/v1/assets` | ✅ | List all assets |
| GET | `/api/v1/assets/page` | ✅ | List assets (paginated + sortable) |
| POST | `/api/v1/depreciation/run` | ✅ | Run depreciation schedule (manual params) |
| POST | `/api/v1/depreciation/recommend` | ✅ | AI: recommend method + useful life only |
| POST | `/api/v1/depreciation/ai-run` | ✅ | **AI: recommend + compute schedule in one call** |
| POST | `/api/v1/tax-strategy/recommend` | ✅ | AI: recommend optimal tax depreciation strategy |
| POST | `/api/v1/classification/suggest` | ✅ | AI: classify asset from invoice text |
| GET | `/actuator/health` | ❌ | Health check |
| GET | `/actuator/metrics` | ❌ | Runtime metrics |

---

## Postman Checklist Automation (Newman)

Run this from the outer workspace root after the app is started.

```bash
# One-time install
npm install -g newman

# Run the endpoint checklist in order
bash /c/Users/nicholas.mathias/IdeaProjects/assetmind/run-postman-checklist.sh
```

Optional overrides:
```bash
# Custom base URL
bash /c/Users/nicholas.mathias/IdeaProjects/assetmind/run-postman-checklist.sh --base-url http://localhost:8081

# CI-friendly JUnit XML output
bash /c/Users/nicholas.mathias/IdeaProjects/assetmind/run-postman-checklist.sh --reporter-junit

# Custom JUnit output directory
bash /c/Users/nicholas.mathias/IdeaProjects/assetmind/run-postman-checklist.sh --reporter-junit --junit-dir ./newman-reports
```

---

## Database Setup (MySQL)

```sql
CREATE DATABASE assetmind;
GRANT ALL PRIVILEGES ON assetmind.* TO 'root'@'localhost' IDENTIFIED BY 'Postgres1';
FLUSH PRIVILEGES;
```

Flyway runs all migrations automatically on startup (`V1` → assets, `V2` → soft-delete, `V3` → users).

---

## Project Structure
```
assetmind/
├── assetmind-application/     # Executable Spring Boot app + config
├── assetmind-api/             # REST controllers + request/response DTOs
├── assetmind-core/            # Domain models + business logic (no Spring deps)
├── assetmind-infrastructure/  # JPA adapters + Spring Security
├── assetmind-ai/              # Groq/Spring-AI classification, depreciation & tax services
├── assetmind-batch/           # Scheduled jobs (placeholder)
├── assetmind-integration/     # External integrations (placeholder)
├── README.md                  # Full documentation
├── QUICKSTART.md              # This file
├── ASSET_API_EXAMPLES.md      # API usage examples
├── JWT_AUTH_GUIDE.md          # JWT authentication deep-dive
└── IMPLEMENTATION_NOTES.md    # Architecture & design notes
```

---

## Troubleshooting

### Port already in use
```bash
mvn -pl assetmind-application -am spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### MySQL connection refused
```bash
# Tests always use H2 in-memory — no MySQL needed for mvn test.
# For the running app, ensure MySQL is up or override the DB URL:
export ASSETMIND_DB_URL=jdbc:h2:mem:assetmind
```

### AI returns RULE_FALLBACK unexpectedly
```bash
# Confirm the key is visible to the process
echo $GROQ_API_KEY

# Re-run the connectivity check
bash /c/Users/nicholas.mathias/IdeaProjects/assetmind/run-groq-ai-check.sh
```

### Tests fail
```bash
java -version   # Must be Java 21
mvn clean test  # Full rebuild + test
```

### Token expired (HTTP 401)
```bash
curl -X POST "http://localhost:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<your refreshToken>"}'
```

---

## Documentation

| File | Contents |
|------|----------|
| `JWT_AUTH_GUIDE.md` | Full JWT authentication reference |
| `ASSET_API_EXAMPLES.md` | Extended API usage examples |
| `IMPLEMENTATION_NOTES.md` | Architecture & design decisions |
| `JWT_IMPLEMENTATION_SUMMARY.md` | Security architecture overview |
| `COMPLETE_IMPLEMENTATION_SUMMARY.md` | Full feature checklist |
