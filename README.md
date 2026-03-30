# AssetMind

AI-powered fixed asset management platform for classification, depreciation scheduling, and tax strategy optimization.

## What It Does

AssetMind helps finance and accounting teams manage the full lifecycle of fixed assets — from purchase through depreciation to disposal. Instead of manually classifying invoices and choosing depreciation methods, the platform uses AI to automate the heavy lifting.

### Core Capabilities

- **Asset Management** — Create, update, list, and soft-delete fixed assets with full pagination and filtering by asset class.
- **AI Classification** — Paste invoice or purchase description text and get an automatic asset class suggestion (e.g. Computer Equipment, Furniture, Vehicle) with a GL code, useful life estimate, and confidence score.
- **Depreciation Engine** — Calculate year-by-year depreciation schedules using Straight Line, Declining Balance, Double Declining, Sum of Years, or MACRS methods. Supports Section 179 and bonus depreciation.
- **AI Depreciation** — Let the AI recommend the optimal depreciation method and useful life, then generate the full schedule in one step.
- **Tax Strategy** — Get state-aware tax strategy recommendations based on equipment type, jurisdiction, and whether you prefer immediate deductions vs. long-horizon spread.
- **Asset Breakout** — Split a large purchase (e.g. a commercial property) into component assets with different classes and useful lives. Includes templates for common scenarios like property acquisitions and office build-outs.
- **AI Breakout Suggestion** — Paste an invoice description and get an AI-suggested component breakdown with asset classes, cost percentages, and useful lives. Supports keyword fallback for property, office, data center, and vehicle purchases.

## Architecture

```
assetmind/                  ← Spring Boot backend (multi-module Maven)
├── assetmind-application   ← Executable Spring Boot app, config, Flyway migrations
├── assetmind-api           ← REST controllers and DTOs
├── assetmind-core          ← Depreciation domain model and calculation engine
├── assetmind-ai            ← AI classification and recommendation service (Groq LLM + rule fallback)
├── assetmind-batch         ← Scheduled compliance sweep (placeholder)
├── assetmind-infrastructure← Infrastructure layer (placeholder)
└── assetmind-integration   ← External integrations (placeholder)

frontend/                   ← React SPA (Vite + React 19 + React Router 7)
├── src/
│   ├── api.js              ← API client with JWT auth, auto-refresh
│   ├── pages/              ← Assets, Depreciation, TaxStrategy, Classification, Breakout
│   └── components/         ← Navbar
```

## API Endpoints

### Authentication (public)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Create a new user account |
| POST | `/api/v1/auth/login` | Authenticate and receive JWT tokens |
| POST | `/api/v1/auth/refresh` | Refresh an expired access token |

### Assets (JWT required)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/assets` | Create a new asset |
| GET | `/api/v1/assets` | List all assets (optional `?assetClass=` filter) |
| GET | `/api/v1/assets/page` | Paginated list (`page`, `size`, `sortBy`, `sortDirection`, `assetClass`) |
| GET | `/api/v1/assets/{id}` | Get a single asset |
| PUT | `/api/v1/assets/{id}` | Update an asset |
| DELETE | `/api/v1/assets/{id}` | Soft-delete an asset |

### Depreciation (JWT required)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/depreciation/run` | Calculate a depreciation schedule |
| POST | `/api/v1/depreciation/recommend` | AI-recommended method and useful life |
| POST | `/api/v1/depreciation/ai-run` | AI recommendation + full schedule in one call |

### Tax Strategy & Classification (JWT required)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/tax-strategy/recommend` | State-aware tax strategy recommendation |
| POST | `/api/v1/classification/suggest` | Classify document text into an asset category |

### Breakout (JWT required)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/breakout/suggest` | AI-suggested component breakdown from invoice text |

### Health & Docs (public)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Application health check |
| GET | `/actuator/metrics` | Application metrics |
| GET | `/swagger-ui.html` | Swagger UI (interactive API docs) |
| GET | `/v3/api-docs` | OpenAPI 3.0 JSON spec |

## Quick Start

### Backend

```bash
cd assetmind
mvn clean test
mvn -pl assetmind-application -am spring-boot:run
```

Starts on `http://localhost:8080`. Requires MySQL — configure via environment variables:
- `ASSETMIND_DB_URL` (default: `jdbc:mysql://localhost:3306/assetmind`)
- `ASSETMIND_DB_USERNAME` (default: `root`)
- `ASSETMIND_DB_PASSWORD`

Flyway migrations run automatically on startup to create the `assets` and `users` tables.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Starts on `http://localhost:3000` with API requests proxied to the backend on port 8080.

### API Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` (no authentication required).
OpenAPI spec at `http://localhost:8080/v3/api-docs`.

## Quality & Testing

### Tests

```bash
cd assetmind
mvn clean test                          # run all tests
mvn test -pl assetmind-ai              # unit tests only (classification, breakout, etc.)
mvn test -pl assetmind-application     # integration tests (controllers, auth, Swagger)
```

**Unit tests** — AI service keyword-fallback and mocked ChatClient tests for Classification, Depreciation, Tax Strategy, and Breakout services.

**Integration tests** — Full Spring Boot context with H2 in-memory DB and JWT auth covering all controller endpoints, validation, auth flows, Swagger accessibility, and correlation ID propagation.

### Code Coverage

JaCoCo coverage reports are generated automatically during `mvn test`:

```bash
mvn clean test
# HTML report at: assetmind-ai/target/site/jacoco/index.html
# HTML report at: assetmind-application/target/site/jacoco/index.html
```

## Production-Readiness Features

### Structured Logging & Correlation IDs

Every HTTP request is tagged with a unique `X-Correlation-ID` (auto-generated or echoed from the incoming header). The ID appears in all log entries via SLF4J MDC and is returned in the response header for end-to-end tracing.

Default output includes the correlation ID in human-readable format. Activate the `json-logging` Spring profile for structured JSON output suitable for log aggregators:

```bash
SPRING_PROFILES_ACTIVE=json-logging mvn -pl assetmind-application spring-boot:run
```

### Rate Limiting

AI endpoints are protected by an in-memory rate limiter (20 requests per minute per client). Affected endpoints:
- `/api/v1/classification/suggest`
- `/api/v1/depreciation/recommend`
- `/api/v1/depreciation/ai-run`
- `/api/v1/tax-strategy/recommend`
- `/api/v1/breakout/suggest`

Rate limit headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After`) are included in responses.

### Caching

Classification and tax strategy recommendations are cached using Caffeine (in-memory, 5 minute TTL, max 500 entries). Identical requests hit the cache instead of re-invoking the AI, reducing latency and API costs.

### Data Visualization

The Depreciation page includes interactive charts (via Recharts) showing:
- **Bar chart** — Depreciation expense per year
- **Line overlay** — Book value declining over time

Charts render automatically below the schedule table for both Manual Run and AI Full Schedule modes.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot, Spring Security (JWT), Flyway, MySQL |
| AI | Groq LLM API with rule-based fallback |
| Frontend | React 19, Vite, React Router 7, Recharts, custom CSS |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Caching | Caffeine (in-memory, 5 min TTL) |
| Testing | JUnit 5, Mockito, MockMvc, JaCoCo |
| Build | Maven (multi-module), npm |

