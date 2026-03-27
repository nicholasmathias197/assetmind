#!/usr/bin/env bash
set -euo pipefail

APP_PORT="${APP_PORT:-8091}"
PROJECT_ROOT="/c/Users/nicholas.mathias/IdeaProjects/assetmind/assetmind"
APP_POM="$PROJECT_ROOT/assetmind-application/pom.xml"
APP_LOG="/tmp/assetmind-ai-check.log"

if [[ -z "${GROQ_API_KEY:-}" ]]; then
  read -rsp "Enter GROQ_API_KEY: " GROQ_API_KEY
  echo
  export GROQ_API_KEY
fi

echo "[1/5] Testing Groq API key..."
GROQ_RESP=$(curl -sS https://api.groq.com/openai/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $GROQ_API_KEY" \
  -d '{
    "model": "openai/gpt-oss-120b",
    "messages": [{"role": "user", "content": "Reply with OK"}],
    "max_tokens": 16,
    "temperature": 0
  }')

echo "$GROQ_RESP" | python - <<'PY'
import json,sys
raw=sys.stdin.read()
try:
    data=json.loads(raw)
except Exception:
    print("Groq response is not valid JSON")
    print(raw)
    raise SystemExit(1)
if "error" in data:
    print("Groq API error:", data["error"])
    raise SystemExit(1)
msg=data.get("choices", [{}])[0].get("message", {}).get("content", "")
print("Groq test OK:", msg.strip()[:120])
PY

echo "[2/5] Starting AssetMind on port $APP_PORT..."
mvn -f "$APP_POM" spring-boot:run -Dspring-boot.run.arguments="--server.port=$APP_PORT" > "$APP_LOG" 2>&1 &
APP_PID=$!
trap 'kill "$APP_PID" >/dev/null 2>&1 || true' EXIT

echo "[3/5] Waiting for health endpoint..."
for _ in $(seq 1 45); do
  if curl -sS "http://localhost:$APP_PORT/actuator/health" >/tmp/assetmind-health.json 2>/dev/null; then
    break
  fi
  sleep 2
done

if ! curl -sS "http://localhost:$APP_PORT/actuator/health" >/tmp/assetmind-health.json 2>/dev/null; then
  echo "App did not become healthy. Last logs:"
  tail -n 80 "$APP_LOG"
  exit 1
fi

echo "Health: $(cat /tmp/assetmind-health.json)"

echo "[4/5] Creating test user and obtaining JWT..."
TS=$(date +%s)
USERNAME="ai_check_$TS"
EMAIL="$USERNAME@example.com"

curl -sS -X POST "http://localhost:$APP_PORT/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"SecurePassword123\",\"email\":\"$EMAIL\"}" >/tmp/assetmind-register.json

LOGIN_JSON=$(curl -sS -X POST "http://localhost:$APP_PORT/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"SecurePassword123\"}")

TOKEN=$(python - <<'PY' "$LOGIN_JSON"
import json,sys
print(json.loads(sys.argv[1])["accessToken"])
PY
)

echo "[5/5] Calling classification endpoint..."
CLASSIFY=$(curl -sS -X POST "http://localhost:$APP_PORT/api/v1/classification/suggest" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"documentText":"Invoice for Dell XPS 15 laptop purchase for accounting team"}')

echo "Classification response:"
echo "$CLASSIFY"

echo "Done. App logs at $APP_LOG"

