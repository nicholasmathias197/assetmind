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

### Health (public)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Application health check |
| GET | `/actuator/metrics` | Application metrics |

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

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot, Spring Security (JWT), Flyway, MySQL |
| AI | Groq LLM API with rule-based fallback |
| Frontend | React 19, Vite, React Router 7, custom CSS |
| Build | Maven (multi-module), npm |

