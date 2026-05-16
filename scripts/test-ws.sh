#!/usr/bin/env bash
# ----------------------------------------------------------
# test-ws.sh -- WebSocket smoke-test (SPEC S6.1)
#
# Uses an ephemeral Docker container with websocat
# so no host dependencies beyond Docker are required.
#
# Usage: ./scripts/test-ws.sh [host] [port]
# Defaults: localhost 8080
# ----------------------------------------------------------
set -euo pipefail

HOST="${1:-localhost}"
PORT="${2:-8080}"
API_URL="http://${HOST}:${PORT}"
WS_URL="ws://host-gateway:${PORT}/ws/state"

WEBSOCAT_IMAGE="ghcr.io/vi/websocat:latest"
CONTAINER="clapper-ws-test-$$"
WS_OUTPUT=$(mktemp)

DIVIDER="$(printf '~%.0s' {1..32})"

cleanup() {
    docker stop "${CONTAINER}" > /dev/null 2>&1 || true
    docker rm -f "${CONTAINER}" > /dev/null 2>&1 || true
    rm -f "${WS_OUTPUT}"
}
trap cleanup EXIT

echo "${DIVIDER}"
echo "## WebSocket Smoke Test"
echo "${DIVIDER}"
echo ""

# Reset hero so IDLE -> ATTACK is a valid transition
curl --silent --output /dev/null \
    -X DELETE "${API_URL}/api/entities/hero" || true

echo "### Connect"
echo "- [v] target ws://${HOST}:${PORT}/ws/state"

# 1. Start websocat detached. --no-close keeps the
#    connection alive after server messages so we can
#    observe both the snapshot and later broadcasts.
docker run -d \
    --name "${CONTAINER}" \
    --add-host=host-gateway:host-gateway \
    "${WEBSOCAT_IMAGE}" \
    --no-close \
    "${WS_URL}" \
    > /dev/null

sleep 2

# 2. Capture initial snapshot
docker logs "${CONTAINER}" > "${WS_OUTPUT}" 2>&1
echo "- [v] received initial snapshot"
echo ""

echo "### Initial snapshot"
head -1 "${WS_OUTPUT}" \
    | python3 -m json.tool 2>/dev/null \
    || head -1 "${WS_OUTPUT}"
echo ""

# 3. Dispatch ATTACK_INPUT to hero
echo "### Dispatch"
echo "- [v] POST /api/events/hero <- ATTACK_INPUT"
curl --silent --output /dev/null \
    -X POST "${API_URL}/api/events/hero" \
    -H 'Content-Type: application/json' \
    -d '{"event":"ATTACK_INPUT"}'

sleep 2

# 4. Collect full output
docker logs "${CONTAINER}" > "${WS_OUTPUT}" 2>&1
echo ""

echo "### WebSocket frames received"
nl -ba -w2 -s'. ' "${WS_OUTPUT}"
echo ""

# 5. Verify ATTACK transition received
echo "${DIVIDER}"
echo "## Result"
if grep -q '"to":"ATTACK"' "${WS_OUTPUT}"; then
    echo "- [v] received ATTACK transition via WebSocket"
    exit 0
else
    echo "- [x] ATTACK transition not found" >&2
    exit 1
fi
