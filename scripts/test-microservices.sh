#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:10010}"
NACOS_URL="${NACOS_URL:-http://127.0.0.1:8848}"

suffix="$(date +%H%M%S)"
phone="${TEST_PHONE:-139${suffix}001}"
password="${TEST_PASSWORD:-Test123456}"
real_name="${TEST_REAL_NAME:-MicroTest${suffix}}"

extract_json() {
  local expr="$1"
  python3 -c "import json,sys; data=json.load(sys.stdin); print(${expr})"
}

echo "== 1. Gateway health =="
curl -fsS "${GATEWAY_URL}/actuator/health"
echo

echo "== 2. Nacos service list =="
curl -fsS "${NACOS_URL}/nacos/v1/ns/service/list?pageNo=1&pageSize=20"
echo

echo "== 3. Register a temporary patient account through gateway -> backend-service =="
register_payload="$(cat <<JSON
{"phone":"${phone}","password":"${password}","realName":"${real_name}","gender":"UNKNOWN","idCard":"ID${suffix}"}
JSON
)"
curl -fsS -X POST "${GATEWAY_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "${register_payload}"
echo

echo "== 4. Login through gateway -> backend-service =="
login_response="$(curl -fsS -X POST "${GATEWAY_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${phone}\",\"password\":\"${password}\"}")"
echo "${login_response}"
echo

access_token="$(printf '%s' "${login_response}" | extract_json "data['accessToken']")"
biz_id="$(printf '%s' "${login_response}" | extract_json "data['bizId']")"

echo "== 5. Current user through gateway -> backend-service =="
curl -fsS "${GATEWAY_URL}/api/auth/me" \
  -H "Authorization: Bearer ${access_token}"
echo

echo "== 6. Query own patient detail through gateway -> patient-service =="
curl -fsS "${GATEWAY_URL}/api/patients/${biz_id}" \
  -H "Authorization: Bearer ${access_token}"
echo

echo "== 7. Upload a small file through gateway -> file-service =="
tmp_file="$(mktemp /tmp/neuhospital-file-test.XXXXXX.txt)"
printf 'microservice gateway file upload test %s\n' "${suffix}" > "${tmp_file}"
upload_response="$(curl -fsS -X POST "${GATEWAY_URL}/api/files/upload" \
  -H "Authorization: Bearer ${access_token}" \
  -F "file=@${tmp_file}" \
  -F "bizType=TEST" \
  -F "bizId=${biz_id}" \
  -F "objectKeyPrefix=microservice-test" \
  -F "uploaderId=${biz_id}")"
echo "${upload_response}"
echo

file_id="$(printf '%s' "${upload_response}" | extract_json "data['data']['id']")"

echo "== 8. Query uploaded file detail through gateway -> file-service =="
curl -fsS "${GATEWAY_URL}/api/files/${file_id}" \
  -H "Authorization: Bearer ${access_token}"
echo

echo "== 9. Check backend fallback route through gateway -> backend-service docs =="
curl -fsSI "${GATEWAY_URL}/v3/api-docs" | sed -n '1,8p'
echo

echo "== 10. Query departments through gateway -> doctor-service =="
curl -fsS "${GATEWAY_URL}/api/departments" \
  -H "Authorization: Bearer ${access_token}"
echo

echo "== 11. Query doctors through gateway -> doctor-service =="
curl -fsS "${GATEWAY_URL}/api/doctors/page?pageNo=1&pageSize=2" \
  -H "Authorization: Bearer ${access_token}"
echo

echo "== Done =="
echo "Temporary account: ${phone} / ${password}"
echo "Patient bizId: ${biz_id}"
echo "Uploaded fileId: ${file_id}"
