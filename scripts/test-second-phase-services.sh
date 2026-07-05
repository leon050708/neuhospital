#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:10010}"
NACOS_URL="${NACOS_URL:-http://127.0.0.1:8848}"
LOGIN_USERNAME="${LOGIN_USERNAME:-doctor_demo_01}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-password123}"
CURL_MAX_TIME="${CURL_MAX_TIME:-25}"

DEMO_PATIENT_ID="${DEMO_PATIENT_ID:-9301}"
DEMO_DOCTOR_ID="${DEMO_DOCTOR_ID:-9201}"

extract_token() {
  sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

require_service() {
  local service_name="$1"
  local response
  response="$(curl -fsS --max-time "${CURL_MAX_TIME}" "${NACOS_URL}/nacos/v1/ns/instance/list?serviceName=${service_name}")"
  printf '%s\n' "${response}"
  if ! printf '%s' "${response}" | grep -q '"healthy":true'; then
    echo "${service_name} has no healthy Nacos instance" >&2
    exit 1
  fi
}

gateway_get() {
  local path="$1"
  curl -fsS --max-time "${CURL_MAX_TIME}" "${GATEWAY_URL}${path}" \
    -H "Authorization: Bearer ${access_token}"
  echo
}

echo "== 1. Gateway health =="
curl -fsS --max-time "${CURL_MAX_TIME}" "${GATEWAY_URL}/actuator/health"
echo

echo "== 2. Nacos service instances =="
require_service "pharmacy-service"
require_service "inspection-service"
require_service "outpatient-service"

echo "== 3. Login through gateway =="
login_response="$(curl -fsS --max-time "${CURL_MAX_TIME}" -X POST "${GATEWAY_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${LOGIN_USERNAME}\",\"password\":\"${LOGIN_PASSWORD}\"}")"
access_token="$(printf '%s' "${login_response}" | extract_token)"
if [ -z "${access_token}" ]; then
  echo "Cannot extract accessToken from login response" >&2
  exit 1
fi
echo "login ok"

echo "== 4. Pharmacy route: drugs page =="
gateway_get "/api/drugs?pageNo=1&pageSize=2"

echo "== 5. Pharmacy route: prescriptions page =="
gateway_get "/api/prescriptions?pageNo=1&pageSize=2&patientId=${DEMO_PATIENT_ID}"

echo "== 6. Inspection route: check requests page =="
gateway_get "/api/check-requests?pageNo=1&pageSize=2&patientId=${DEMO_PATIENT_ID}&doctorId=${DEMO_DOCTOR_ID}"

echo "== 7. Inspection route: inspection requests page =="
gateway_get "/api/inspection-requests?pageNo=1&pageSize=2&patientId=${DEMO_PATIENT_ID}&doctorId=${DEMO_DOCTOR_ID}"

echo "== 8. Inspection route: disposal requests page =="
gateway_get "/api/disposal-requests?pageNo=1&pageSize=2&patientId=${DEMO_PATIENT_ID}&doctorId=${DEMO_DOCTOR_ID}"

echo "== 9. Outpatient route: medical records page =="
gateway_get "/api/outpatient/records?pageNo=1&pageSize=2&patientId=${DEMO_PATIENT_ID}&doctorId=${DEMO_DOCTOR_ID}"

echo "== Done =="
echo "Verified gateway routing for pharmacy-service, inspection-service and outpatient-service."
echo "Payment remains on backend-service in the default gateway configuration."
