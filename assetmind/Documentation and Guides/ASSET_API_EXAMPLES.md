# Asset API Examples

## Create Asset
```bash
curl -X POST "http://localhost:8080/api/v1/assets" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "asset-1",
    "description": "MacBook Pro 14-inch",
    "assetClass": "COMPUTER_EQUIPMENT",
    "costBasis": 2500,
    "inServiceDate": "2026-03-01",
    "usefulLifeYears": 5
  }'
```

## Get Single Asset
```bash
curl "http://localhost:8080/api/v1/assets/asset-1"
```

## Update Asset
```bash
curl -X PUT "http://localhost:8080/api/v1/assets/asset-1" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "asset-1",
    "description": "MacBook Pro 14-inch M4",
    "assetClass": "COMPUTER_EQUIPMENT",
    "costBasis": 2800,
    "inServiceDate": "2026-03-01",
    "usefulLifeYears": 5
  }'
```

## List All Assets (non-paginated)
```bash
curl "http://localhost:8080/api/v1/assets"
```

## List Assets by Class (non-paginated)
```bash
curl "http://localhost:8080/api/v1/assets?assetClass=COMPUTER_EQUIPMENT"
```

## List Assets with Pagination (default 20 per page, sorted by id ascending)
```bash
curl "http://localhost:8080/api/v1/assets/page"
```

## List Assets with Custom Pagination (page 0, size 10, sorted by description descending)
```bash
curl "http://localhost:8080/api/v1/assets/page?page=0&size=10&sortBy=description&sortDirection=desc"
```

## List Assets with Pagination and Class Filter
```bash
curl "http://localhost:8080/api/v1/assets/page?page=0&size=20&sortBy=id&sortDirection=asc&assetClass=COMPUTER_EQUIPMENT"
```

## Delete Asset (soft-delete)
```bash
curl -X DELETE "http://localhost:8080/api/v1/assets/asset-1"
```

After deletion, the asset will not appear in any list or get queries (soft-delete: `deleted=true`).

## Pagination Response Format
```json
{
  "content": [
    {
      "id": "asset-1",
      "description": "MacBook Pro 14-inch M4",
      "assetClass": "COMPUTER_EQUIPMENT",
      "costBasis": 2800,
      "inServiceDate": "2026-03-01",
      "usefulLifeYears": 5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

