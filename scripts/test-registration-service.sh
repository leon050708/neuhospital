#!/usr/bin/env bash
set -euo pipefail

REGISTRATION_URL="${REGISTRATION_URL:-http://127.0.0.1:10023}"
GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:10010}"
NACOS_URL="${NACOS_URL:-http://127.0.0.1:8848}"
LOGIN_USERNAME="${LOGIN_USERNAME:-139210956001}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-Test123456}"
DIRECT_PATIENT_USER_ID="${DIRECT_PATIENT_USER_ID:-9301}"
DIRECT_PATIENT_BIZ_ID="${DIRECT_PATIENT_BIZ_ID:-9301}"
CURL_MAX_TIME="${CURL_MAX_TIME:-25}"

extract_token() {
  sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

echo "== 1. registration-service health =="
curl -fsS --max-time "${CURL_MAX_TIME}" "${REGISTRATION_URL}/actuator/health"
echo

echo "== 2. Nacos registration-service instance =="
curl -fsS --max-time "${CURL_MAX_TIME}" "${NACOS_URL}/nacos/v1/ns/instance/list?serviceName=registration-service"
echo

echo "== 3. Direct schedules query =="
curl -fsS --max-time "${CURL_MAX_TIME}" "${REGISTRATION_URL}/api/schedules?pageNo=1&pageSize=2" \
  -H "X-User-Id: 1" \
  -H "X-Username: admin" \
  -H "X-User-Roles: ADMIN" \
  -H "X-User-Type: ADMIN"
echo

echo "== 4. Direct my registrations query =="
curl -fsS --max-time "${CURL_MAX_TIME}" "${REGISTRATION_URL}/api/registrations/my?pageNo=1&pageSize=2" \
  -H "X-User-Id: ${DIRECT_PATIENT_USER_ID}" \
  -H "X-Username: patient-demo" \
  -H "X-User-Roles: PATIENT" \
  -H "X-User-Type: PATIENT" \
  -H "X-Biz-Id: ${DIRECT_PATIENT_BIZ_ID}"
echo

echo "== 5. Login through gateway =="
login_response="$(curl -fsS --max-time "${CURL_MAX_TIME}" -X POST "${GATEWAY_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${LOGIN_USERNAME}\",\"password\":\"${LOGIN_PASSWORD}\"}")"
access_token="$(printf '%s' "${login_response}" | extract_token)"
if [ -z "${access_token}" ]; then
  echo "Cannot extract accessToken from login response" >&2
  exit 1
fi
echo "login ok"

echo "== 6. Gateway schedules query =="
curl -fsS --max-time "${CURL_MAX_TIME}" "${GATEWAY_URL}/api/schedules?pageNo=1&pageSize=2" \
  -H "Authorization: Bearer ${access_token}"
echo

echo "== 7. Gateway my registrations query =="
curl -fsS --max-time "${CURL_MAX_TIME}" "${GATEWAY_URL}/api/registrations/my?pageNo=1&pageSize=2" \
  -H "Authorization: Bearer ${access_token}"
echo

echo "== Done =="
echo "Note: /api/registrations/quick is intentionally excluded from this smoke test."
