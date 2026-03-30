#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLLECTION="$ROOT_DIR/assetmind/AssetMind-Local.postman_collection.json"
ENV_FILE="$ROOT_DIR/assetmind/AssetMind-Local.postman_environment.json"

usage() {
  cat <<'EOF'
Usage:
  ./run-postman-checklist.sh [--base-url URL] [--newman-cmd CMD] [--reporter-junit] [--junit-dir DIR]

Options:
  --base-url URL     Override the environment baseUrl value at runtime.
  --newman-cmd CMD   Use a specific Newman launcher (e.g. "newman" or "npx newman").
  --reporter-junit   Enable JUnit XML output (CI-friendly).
  --junit-dir DIR    Directory for JUnit XML files (default: ./newman-reports).
  -h, --help         Show this help.

Notes:
  - Start the Spring Boot app first.
  - This runs folders in checklist order and fails on first broken step.
EOF
}

BASE_URL_OVERRIDE=""
NEWMAN_CMD_OVERRIDE=""
CI_JUNIT_MODE="false"
JUNIT_DIR="$ROOT_DIR/newman-reports"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL_OVERRIDE="${2:-}"
      shift 2
      ;;
    --newman-cmd)
      NEWMAN_CMD_OVERRIDE="${2:-}"
      shift 2
      ;;
    --reporter-junit)
      CI_JUNIT_MODE="true"
      shift
      ;;
    --junit-dir)
      JUNIT_DIR="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ ! -f "$COLLECTION" ]]; then
  echo "ERROR: Collection file not found: $COLLECTION"
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: Environment file not found: $ENV_FILE"
  exit 1
fi

resolve_newman_cmd() {
  if [[ -n "$NEWMAN_CMD_OVERRIDE" ]]; then
    printf '%s' "$NEWMAN_CMD_OVERRIDE"
    return
  fi

  if command -v newman >/dev/null 2>&1; then
    printf '%s' "newman"
    return
  fi

  if command -v npx >/dev/null 2>&1; then
    printf '%s' "npx newman"
    return
  fi

  echo "ERROR: Newman is not available. Install with: npm install -g newman"
  exit 1
}

run_folder() {
  local folder="$1"
  local base_cmd="$2"

  echo ""
  echo "==> Running folder: $folder"

  local cmd=( $base_cmd run "$COLLECTION" --environment "$ENV_FILE" --folder "$folder" )

  if [[ -n "$BASE_URL_OVERRIDE" ]]; then
    cmd+=( --env-var "baseUrl=$BASE_URL_OVERRIDE" )
  fi

  if [[ "$CI_JUNIT_MODE" == "true" ]]; then
    local safe_folder
    safe_folder="$(printf '%s' "$folder" | tr ' ' '_' | tr -cd '[:alnum:]_-')"
    cmd+=( --reporters "cli,junit" --reporter-junit-export "$JUNIT_DIR/${safe_folder}.xml" )
  fi

  "${cmd[@]}"
}

NEWMAN_CMD="$(resolve_newman_cmd)"

if [[ "$CI_JUNIT_MODE" == "true" ]]; then
  mkdir -p "$JUNIT_DIR"
fi

echo "Using Newman command: $NEWMAN_CMD"
[[ -n "$BASE_URL_OVERRIDE" ]] && echo "Overriding baseUrl with: $BASE_URL_OVERRIDE"
[[ "$CI_JUNIT_MODE" == "true" ]] && echo "JUnit XML output directory: $JUNIT_DIR"

echo "==> Preflight: quick health check"
HEALTH_URL="${BASE_URL_OVERRIDE:-http://localhost:8080}/actuator/health"
if ! curl -fsS "$HEALTH_URL" >/dev/null; then
  echo "WARNING: Health check failed at $HEALTH_URL"
  echo "The checklist may fail until the app is running."
fi

run_folder "Auth" "$NEWMAN_CMD"
run_folder "Assets" "$NEWMAN_CMD"
run_folder "Depreciation" "$NEWMAN_CMD"
run_folder "Tax Strategy" "$NEWMAN_CMD"
run_folder "Classification" "$NEWMAN_CMD"
run_folder "Health" "$NEWMAN_CMD"

echo ""
echo "Checklist complete."

if [[ "$CI_JUNIT_MODE" == "true" ]]; then
  echo "JUnit reports generated under: $JUNIT_DIR"
fi

