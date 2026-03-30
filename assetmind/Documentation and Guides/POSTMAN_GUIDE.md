# AssetMind — Postman Testing Guide

## 1. Import the Collection & Environment

1. Open **Postman**
2. Click **Import** (top left)
3. Drag-and-drop **both** files from `assetmind/`:
   - `AssetMind-Local.postman_collection.json`
   - `AssetMind-Local.postman_environment.json`
4. In the top-right environment dropdown, select **"AssetMind Local"**

> The collection uses `{{baseUrl}}` (defaults to `http://localhost:8080`) and
> `{{token}}` (auto-filled by the Login request).  All protected requests send
> `Authorization: Bearer {{token}}` automatically via collection-level auth.

---

## 2. Start the Application

```bash
cd /c/Users/nicholas.mathias/IdeaProjects/assetmind/assetmind
mvn -pl assetmind-application -am spring-boot:run
```

Wait until you see `Started AssetmindApplication` in the console, then proceed.

---

## 3. Checklist — Run Requests in This Order

### ✅ Health & Metrics (no auth needed — run first to confirm app is up)

| # | Request | Expected |
|---|---------|----------|
| 1 | `Health & Metrics / 1 - Health Check` | `{"status":"UP"}` |
| 2 | `Health & Metrics / 2 - Metrics` | JSON list of metric names |

---

### ✅ Auth

| # | Request | Expected | Notes |
|---|---------|----------|-------|
| 3 | `Auth / 1 - Register` | `201 Created` or `200 OK` | Only needed once per fresh DB |
| 4 | `Auth / 2 - Login (auto-saves token)` | `200 OK` + JSON with `accessToken` | **Token is saved automatically** — every subsequent request uses it |
| *(optional)* | `Auth / 3 - Refresh Token` | `200 OK` + new `accessToken` | Run only when your token expires (default 1 hour) |

**After Login you should see** in the Tests tab:
```
✓  Status is 200
```
And in Collection Variables: `token` = a long `eyJ...` string.

---

### ✅ Assets

| # | Request | Expected |
|---|---------|----------|
| 5 | `Assets / 1 - Create Asset (laptop-001)` | `201 Created` |
| 6 | `Assets / 2 - Create Asset (furniture-001)` | `201 Created` |
| 7 | `Assets / 3 - Create Asset (van-001)` | `201 Created` |
| 8 | `Assets / 4 - Get Asset by Id` | `200 OK` + laptop-001 JSON |
| 9 | `Assets / 5 - List All Assets` | `200 OK` + array of all 3 assets |
| 10 | `Assets / 6 - List Assets (Paginated)` | `200 OK` + paged response |
| 11 | `Assets / 7 - List Assets (Filter by Class)` | `200 OK` + only COMPUTER_EQUIPMENT assets |
| 12 | `Assets / 8 - Update Asset` | `200 OK` + updated description |
| *(cleanup)* | `Assets / 9 - Delete Asset (soft)` | `204 No Content` |

> Re-create `laptop-001` after the delete if you want to use it in Depreciation requests.

---

### ✅ Depreciation

| # | Request | What AI does | Expected `aiSource` |
|---|---------|-------------|---------------------|
| 13 | `Depreciation / 1 - Run Depreciation (manual / straight-line)` | No AI — you chose `STRAIGHT_LINE` | n/a |
| 14 | `Depreciation / 2 - AI Recommend (method + useful life only)` | Groq picks method + useful life | `AI_GROQ` |
| 15 | `Depreciation / 3 - AI Run — laptop CA` | Groq picks method, engine runs the schedule | `AI_GROQ` |
| 16 | `Depreciation / 4 - AI Run (furniture NY)` | Same, 7-year furniture | `AI_GROQ` |
| 17 | `Depreciation / 5 - AI Run (delivery van TX)` | Same, vehicle | `AI_GROQ` |
| 18 | `Depreciation / 6 - AI Run (leasehold improvement IL)` | Same, long-horizon ADS | `AI_GROQ` |

**What to look for in a successful AI Run response:**
```json
{
  "recommendedMethod": "MACRS_200DB_HY",
  "suggestedUsefulLifeYears": 5,
  "aiConfidence": 0.92,
  "aiRationale": "...",
  "aiSource": "AI_GROQ",
  "schedule": [ ... ]
}
```
If you see `"aiSource": "RULE_FALLBACK"` the Groq call failed — check the app console for the warning message.

---

### ✅ Tax Strategy

| # | Request | Expected `recommendedMethod` |
|---|---------|------------------------------|
| 19 | `Tax Strategy / 1 - Recommend (CA server rack)` | `MACRS_200DB_HY` (accelerated) |
| 20 | `Tax Strategy / 2 - Recommend (TX vehicle)` | `MACRS_200DB_HY` |
| 21 | `Tax Strategy / 3 - Recommend (IL leasehold long-horizon)` | `ADS_STRAIGHT_LINE` |

---

### ✅ Classification

| # | Request | Expected `assetClass` | Expected `glCode` |
|---|---------|----------------------|-------------------|
| 22 | `Classification / 1 - Classify laptop invoice` | `COMPUTER_EQUIPMENT` | `1610` |
| 23 | `Classification / 2 - Classify furniture` | `FURNITURE` | `1620` |
| 24 | `Classification / 3 - Classify vehicle` | `VEHICLE` | `1630` |
| 25 | `Classification / 4 - Classify leasehold improvement` | `LEASEHOLD_IMPROVEMENT` | `1710` |
| 26 | `Classification / 5 - Classify building improvement` | `BUILDING_IMPROVEMENT` | `1720` |

---

## 4. Quick Reference — Request Bodies

### Create Asset (any)
```json
{
  "id": "my-asset-id",
  "description": "Some description",
  "assetClass": "COMPUTER_EQUIPMENT",
  "costBasis": 10000.00,
  "inServiceDate": "2026-01-01",
  "usefulLifeYears": 5
}
```
**Valid `assetClass` values:** `COMPUTER_EQUIPMENT` · `FURNITURE` · `VEHICLE` · `LEASEHOLD_IMPROVEMENT` · `BUILDING_IMPROVEMENT` · `OTHER`

---

### Depreciation — Manual Run
```json
{
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
}
```
**Valid `method`:** `STRAIGHT_LINE` · `MACRS_200DB_HY` · `ADS_STRAIGHT_LINE`  
**Valid `bookType`:** `BOOK` · `TAX` · `STATE`

---

### Depreciation — AI Recommend Only
```json
{
  "stateCode": "CA",
  "equipmentType": "Dell XPS 15 laptop",
  "assetClass": "COMPUTER_EQUIPMENT",
  "immediateDeductionPreferred": true,
  "longHorizonAsset": false
}
```

---

### Depreciation — AI Run (recommend + schedule)
```json
{
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
}
```

---

### Tax Strategy
```json
{
  "stateCode": "CA",
  "equipmentType": "server rack",
  "immediateDeductionPreferred": true,
  "longHorizonAsset": false
}
```

---

### Classification
```json
{
  "documentText": "Invoice: Dell XPS 15 laptop, 32GB RAM, 1TB SSD — purchased for the accounting team"
}
```

---

## 5. Troubleshooting

| Symptom | Fix |
|---------|-----|
| `401 Unauthorized` on any endpoint | Re-run `Auth / 2 - Login` — token may have expired (1 hr default) |
| `403 Forbidden` | Make sure **"AssetMind Local"** environment is selected in top-right dropdown |
| `404 Not Found` on asset requests | Re-run `Assets / 1 - Create Asset (laptop-001)` first |
| `"aiSource": "RULE_FALLBACK"` | Check app console for Groq error; key is already in `application.yml` |
| Connection refused | Confirm the app is running: `curl http://localhost:8080/actuator/health` |
| Register returns `409 Conflict` | User already exists — skip Register, go straight to Login |

