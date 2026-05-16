#!/usr/bin/env bash
# ----------------------------------------------------------
# seed-events.sh -- FSM demo sequence (SPEC S6.2)
#
# Usage: ./scripts/seed-events.sh [host] [port]
# Defaults: localhost 8080
# ----------------------------------------------------------
set -euo pipefail

HOST="${1:-localhost}"
PORT="${2:-8080}"
BASE_URL="http://${HOST}:${PORT}/api"

DIVIDER="$(printf '~%.0s' {1..32})"

dispatch() {
    local entity="$1"
    local event="$2"
    local http_code
    http_code=$(curl --silent --output /dev/null \
        --write-out "%{http_code}" \
        -X POST "${BASE_URL}/events/${entity}" \
        -H 'Content-Type: application/json' \
        -d "{\"event\":\"${event}\"}")
    if [ "${http_code}" -ne 200 ]; then
        echo "- [x] ${entity} <- ${event} (HTTP ${http_code})" \
             >&2
        return 1
    fi
    echo "- [v] ${entity} <- ${event}"
}

reset_entity() {
    local entity="$1"
    curl --silent --output /dev/null \
        -X DELETE "${BASE_URL}/entities/${entity}" || true
    echo "- [v] reset ${entity}"
}

echo "${DIVIDER}"
echo "## Seed Events: Full FSM Demo Cycle"
echo "${DIVIDER}"
echo ""

echo "### Reset seeded entities"
reset_entity hero
reset_entity enemy_1
reset_entity enemy_2
echo ""

echo "### hero: full cycle"
dispatch hero MOVE
dispatch hero SPRINT
dispatch hero STOP
dispatch hero ATTACK_INPUT
dispatch hero MOVE
dispatch hero HIT
dispatch hero RESPAWN
echo ""

echo "### enemy_1: attack and die"
dispatch enemy_1 ATTACK_INPUT
dispatch enemy_1 HIT
echo ""

echo "${DIVIDER}"
echo "## Done"
