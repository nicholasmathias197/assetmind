# AssetMind Backend Skeleton

This repository now contains a runnable multi-module Spring Boot baseline for the AI fixed asset classification and depreciation engine.

## Modules

- `assetmind-application`: executable Spring Boot application
- `assetmind-api`: REST controllers and API DTOs
- `assetmind-core`: depreciation domain model and calculation engine
- `assetmind-ai`: heuristic classification service (placeholder for OCR/LLM pipeline)
- `assetmind-batch`: scheduled compliance sweep job (placeholder)
- `assetmind-infrastructure`: infrastructure placeholder module
- `assetmind-integration`: external integration placeholder module

## Implemented Endpoints

- `POST /api/v1/auth/login` (public)
- `POST /api/v1/auth/register` (public)
- `POST /api/v1/auth/refresh` (public)
- `POST /api/v1/assets`
- `PUT /api/v1/assets/{id}`
- `GET /api/v1/assets/{id}`
- `GET /api/v1/assets?assetClass=COMPUTER_EQUIPMENT`
- `GET /api/v1/assets/page?page=0&size=20&sort=id,asc&assetClass=COMPUTER_EQUIPMENT`
- `DELETE /api/v1/assets/{id}` (soft-delete)
- `POST /api/v1/classification/suggest`
- `POST /api/v1/depreciation/run`
- `POST /api/v1/tax-strategy/recommend`

## Quick Start

```bash
mvn clean test
mvn -pl assetmind-application -am spring-boot:run
```

## Example Request: Depreciation Run

```bash
curl -X POST "http://localhost:8080/api/v1/depreciation/run" \
  -H "Content-Type: application/json" \
  -d '{
    "assetId": "asset-123",
    "bookType": "BOOK",
    "method": "STRAIGHT_LINE",
    "assetClass": "COMPUTER_EQUIPMENT",
    "inServiceDate": "2026-01-01",
    "costBasis": 12000,
    "salvageValue": 2000,
    "usefulLifeYears": 5,
    "section179Enabled": false,
    "section179Amount": 0,
    "bonusDepreciationRate": 0
  }'
```

## Notes

- Default local DB config now points to MySQL via `ASSETMIND_DB_URL=jdbc:mysql://localhost:3306/assetmind`, `ASSETMIND_DB_USERNAME=root`, and `ASSETMIND_DB_PASSWORD=Postgres1`.
- **JWT Authentication**: All asset endpoints are now secured. See `JWT_AUTH_GUIDE.md` for details on login, token generation, and token refresh.
- Flyway migration `V1__create_assets.sql` creates the `assets` table on startup.
- Flyway migration `V2__add_soft_delete.sql` adds soft-delete support.
- Flyway migration `V3__create_users.sql` creates the `users` table for authentication.
- Soft-delete support via migration `V2__add_soft_delete.sql` (assets with `deleted=true` are excluded from all queries).
- Paginated list endpoint supports `page`, `size`, and `sort` parameters; defaults to page size 20.
- Depreciation rules are intentionally simplified for MVP bootstrap.
- Classification currently uses keyword heuristics to keep the platform runnable.
- Next implementation steps: richer persistence model (books/schedules/disposals), OCR pipeline, full MACRS/state rule packs, and role-based access control.
