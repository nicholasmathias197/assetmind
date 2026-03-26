# Quick Start Guide: AssetMind Backend

## Build & Run

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8.0+ (or use defaults; tests use H2 in-memory)

### Local Development
```bash
# Build with tests
mvn clean test

# Start application
mvn -pl assetmind-application -am spring-boot:run
```

Server listens on `http://localhost:8080`

## Authentication (JWT)

**All asset endpoints require JWT authentication. Follow these steps first:**

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
# Extract and export token for subsequent requests
export TOKEN="<accessToken from login response>"
```

### Create Your First Asset
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
# First 20 assets
curl "http://localhost:8080/api/v1/assets/page" \
  -H "Authorization: Bearer $TOKEN"

# Custom: page 1, 10 per page, sorted by description (descending)
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

### Delete an Asset (Soft-Delete)
```bash
curl -X DELETE "http://localhost:8080/api/v1/assets/laptop-001" \
  -H "Authorization: Bearer $TOKEN"
```

### Calculate Depreciation
```bash
curl -X POST "http://localhost:8080/api/v1/depreciation/run" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "assetId": "laptop-001",
    "bookType": "BOOK",
    "method": "STRAIGHT_LINE",
    "assetClass": "COMPUTER_EQUIPMENT",
    "inServiceDate": "2026-03-26",
    "costBasis": 1500,
    "salvageValue": 200,
    "usefulLifeYears": 5,
    "section179Enabled": false,
    "section179Amount": 0,
    "bonusDepreciationRate": 0
  }'
```

## Key Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/assets` | Create asset |
| GET | `/api/v1/assets/{id}` | Get single asset |
| PUT | `/api/v1/assets/{id}` | Update asset |
| GET | `/api/v1/assets` | List all assets (non-paginated) |
| GET | `/api/v1/assets/page` | List assets (paginated + sortable) |
| DELETE | `/api/v1/assets/{id}` | Soft-delete asset |
| POST | `/api/v1/depreciation/run` | Calculate depreciation schedule |
| POST | `/api/v1/tax-strategy/recommend` | Get tax optimization recommendation |
| POST | `/api/v1/classification/suggest` | Classify asset from invoice text |

## Database Setup (MySQL)

```sql
CREATE DATABASE assetmind;
GRANT ALL PRIVILEGES ON assetmind.* TO 'root'@'localhost' IDENTIFIED BY 'Postgres1';
FLUSH PRIVILEGES;
```

Flyway will auto-migrate on startup.

## Project Structure
```
assetmind/
├── assetmind-application/     # Executable app + config
├── assetmind-api/             # REST controllers
├── assetmind-core/            # Domain + business logic
├── assetmind-infrastructure/  # JPA/database adapters
├── assetmind-ai/              # Classification placeholder
├── assetmind-batch/           # Scheduled jobs placeholder
├── assetmind-integration/     # External integrations placeholder
├── README.md                  # Full documentation
├── ASSET_API_EXAMPLES.md      # API usage examples
└── IMPLEMENTATION_NOTES.md    # Architecture & design notes
```

## Troubleshooting

### Port already in use
```bash
mvn -pl assetmind-application -am spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### MySQL connection refused
```bash
# Check if MySQL is running
# Or use default H2 in-memory by not setting ASSETMIND_DB_* env vars
```

### Tests fail
```bash
# Ensure Java 21 is set
java -version

# Clean build
mvn clean test
```

### Token Expired
If you get a 401 error, the access token may have expired. Get a new one:

```bash
curl -X POST "http://localhost:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken>"}'
```

## Documentation

- See `JWT_AUTH_GUIDE.md` for comprehensive JWT authentication details
- See `ASSET_API_EXAMPLES.md` for more API usage examples
- See `IMPLEMENTATION_NOTES.md` for architecture & design notes
