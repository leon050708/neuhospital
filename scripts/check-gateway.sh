#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:10010}"
NACOS_URL="${NACOS_URL:-http://127.0.0.1:8848}"

echo "== Gateway health =="
curl -fsS "${GATEWAY_URL}/actuator/health"
echo

echo "== Nacos service list =="
curl -fsS "${NACOS_URL}/nacos/v1/ns/service/list?pageNo=1&pageSize=20"
echo

echo "== OpenAPI through gateway =="
curl -fsSI "${GATEWAY_URL}/v3/api-docs" | sed -n '1,8p'

echo "== Auth pre-check =="
auth_status="$(curl -sS -o /dev/null -w '%{http_code}' -I "${GATEWAY_URL}/api/auth/me")"
if [[ "${auth_status}" != "401" ]]; then
  echo "Expected 401 for unauthenticated /api/auth/me, got ${auth_status}" >&2
  exit 1
fi
echo "HTTP ${auth_status} Unauthorized (expected)"
