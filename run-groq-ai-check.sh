#!/usr/bin/env bash
set -euo pipefail

APP_PORT="${APP_PORT:-8091}"
PROJECT_ROOT="/c/Users/nicholas.mathias/IdeaProjects/assetmind/assetmind"
APP_POM="$PROJECT_ROOT/assetmind-application/pom.xml"
APP_LOG="/tmp/assetmind-ai-check.log"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_ENV_FILE="${LOCAL_ENV_FILE:-$SCRIPT_DIR/.groq.local.env}"

if [[ -z "${GROQ_API_KEY:-}" && -f "$LOCAL_ENV_FILE" ]]; then
  # shellcheck source=/dev/null
  source "$LOCAL_ENV_FILE"
fi

if [[ -z "${GROQ_API_KEY:-}" ]]; then
  echo "GROQ_API_KEY is not set."
  echo "Set it once in your shell or create $LOCAL_ENV_FILE with:"
  echo "GROQ_API_KEY=your_groq_key_here"
  exit 1
fi

if [[ "$GROQ_API_KEY" == "replace_with_your_groq_api_key" ]]; then
  echo "GROQ_API_KEY is still set to the placeholder value in $LOCAL_ENV_FILE"
  echo "Please replace it with a real key from https://console.groq.com"
  exit 1
fi

echo "[1/5] Testing Groq API key..."
GROQ_HTTP=$(curl -sS -w '\n%{http_code}' https://api.groq.com/openai/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $GROQ_API_KEY" \
  -d '{
    "model": "openai/gpt-oss-120b",
    "messages": [{"role": "user", "content": "Reply with OK"}],
    "max_tokens": 16,
    "temperature": 0
  }')

GROQ_RESP="${GROQ_HTTP%$'\n'*}"
GROQ_STATUS="${GROQ_HTTP##*$'\n'}"

if [[ "$GROQ_STATUS" -lt 200 || "$GROQ_STATUS" -ge 300 ]]; then
  echo "Groq API HTTP error: $GROQ_STATUS"
  echo "$GROQ_RESP"
  exit 1
fi

python - <<'PY' "$GROQ_RESP"
import json,sys
raw=sys.argv[1]
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

