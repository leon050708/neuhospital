#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/logs/microservices"
PID_DIR="${ROOT_DIR}/logs/pids"
NACOS_URL="${NACOS_URL:-http://127.0.0.1:8848}"
CURL_MAX_TIME="${CURL_MAX_TIME:-10}"
START_RETRIES="${START_RETRIES:-2}"

mkdir -p "${LOG_DIR}" "${PID_DIR}"

info() {
  printf '[INFO] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1" >&2
}

is_port_listening() {
  local port="$1"
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

check_nacos() {
  if curl -fsS --max-time "${CURL_MAX_TIME}" "${NACOS_URL}/nacos/v1/ns/service/list?pageNo=1&pageSize=1" >/dev/null 2>&1; then
    info "Nacos is reachable: ${NACOS_URL}"
    return
  fi
  warn "Nacos is not reachable at ${NACOS_URL}"
  warn "Please start your local Nacos first, then rerun this script."
  exit 1
}

start_infra() {
  info "Starting Redis, Kafka and MinIO with Docker Compose"
  docker compose -f "${ROOT_DIR}/infra/compose.yml" up -d redis kafka minio
}

start_service() {
  local module="$1"
  local port="$2"
  local log_file="${LOG_DIR}/${module}.log"
  local pid_file="${PID_DIR}/${module}.pid"
  local jar_file

  if is_port_listening "${port}"; then
    warn "${module} port ${port} is already listening; skip starting it."
    return
  fi

  if [[ -f "${pid_file}" ]] && kill -0 "$(cat "${pid_file}")" >/dev/null 2>&1; then
    warn "${module} already has a live PID $(cat "${pid_file}"); skip starting it."
    return
  fi
  rm -f "${pid_file}"

  info "Packaging ${module}; log: ${log_file}"
  : > "${log_file}"
  (
    cd "${ROOT_DIR}/${module}"
    ../mvnw -q -DskipTests package >>"${log_file}" 2>&1
  )

  jar_file="$(find "${ROOT_DIR}/${module}/target" -maxdepth 1 -name "${module}-*.jar" ! -name "*.original" | head -n 1)"
  if [[ -z "${jar_file}" ]]; then
    warn "Cannot find packaged jar for ${module}."
    return 1
  fi

  info "Starting ${module} on port ${port}; log: ${log_file}"
  (
    cd "${ROOT_DIR}/${module}"
    nohup env \
      SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE="${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:-2}" \
      SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE="${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE:-1}" \
      FILE_SERVICE_URI="${FILE_SERVICE_URI:-lb://file-service}" \
      PATIENT_SERVICE_URI="${PATIENT_SERVICE_URI:-lb://patient-service}" \
      DOCTOR_SERVICE_URI="${DOCTOR_SERVICE_URI:-lb://doctor-service}" \
      REGISTRATION_SERVICE_URI="${REGISTRATION_SERVICE_URI:-lb://registration-service}" \
      PAYMENT_SERVICE_URI="${PAYMENT_SERVICE_URI:-lb://backend-service}" \
      PHARMACY_SERVICE_URI="${PHARMACY_SERVICE_URI:-lb://pharmacy-service}" \
      INSPECTION_SERVICE_URI="${INSPECTION_SERVICE_URI:-lb://inspection-service}" \
      OUTPATIENT_SERVICE_URI="${OUTPATIENT_SERVICE_URI:-lb://outpatient-service}" \
      java -jar "${jar_file}" >>"${log_file}" 2>&1 &
    echo $! > "${pid_file}"
  )
}

stop_started_service() {
  local module="$1"
  local pid_file="${PID_DIR}/${module}.pid"

  if [[ ! -f "${pid_file}" ]]; then
    return
  fi

  local pid
  pid="$(cat "${pid_file}")"
  if kill -0 "${pid}" >/dev/null 2>&1; then
    warn "Stopping ${module} PID ${pid} before retry."
    kill "${pid}" >/dev/null 2>&1 || true
    sleep 3
  fi
  rm -f "${pid_file}"
}

wait_for_health() {
  local module="$1"
  local port="$2"
  local health_url="http://127.0.0.1:${port}/actuator/health"
  local deadline=$((SECONDS + 90))

  while (( SECONDS < deadline )); do
    if curl -fsS --max-time "${CURL_MAX_TIME}" "${health_url}" >/dev/null 2>&1; then
      info "${module} is UP: ${health_url}"
      return
    fi
    sleep 2
  done

  warn "${module} did not become healthy in 90 seconds. Check ${LOG_DIR}/${module}.log"
  return 1
}

start_and_wait() {
  local module="$1"
  local port="$2"
  local attempt=1
  local max_attempts=$((START_RETRIES + 1))

  while (( attempt <= max_attempts )); do
    if (( attempt > 1 )); then
      info "Retrying ${module} startup (${attempt}/${max_attempts})"
    fi

    start_service "${module}" "${port}"
    if wait_for_health "${module}" "${port}"; then
      return
    fi

    stop_started_service "${module}"
    attempt=$((attempt + 1))
  done

  warn "${module} failed to become healthy after ${max_attempts} attempt(s)."
  return 1
}

main() {
  start_infra
  check_nacos

  start_and_wait "backend-service" "8081"
  start_service "file-service" "10020"
  start_service "patient-service" "10021"
  start_service "doctor-service" "10022"
  start_service "registration-service" "10023"
  if [[ "${START_PAYMENT_SERVICE:-false}" == "true" ]]; then
    start_service "payment-service" "10024"
  else
    info "Skipping payment-service by default; /api/payment/** stays on backend-service."
  fi
  start_service "pharmacy-service" "10025"
  start_service "inspection-service" "10026"
  start_service "outpatient-service" "10027"
  start_service "gateway-service" "10010"

  wait_for_health "file-service" "10020"
  wait_for_health "patient-service" "10021"
  wait_for_health "doctor-service" "10022"
  wait_for_health "registration-service" "10023"
  if [[ "${START_PAYMENT_SERVICE:-false}" == "true" ]]; then
    wait_for_health "payment-service" "10024"
  fi
  wait_for_health "pharmacy-service" "10025"
  wait_for_health "inspection-service" "10026"
  wait_for_health "outpatient-service" "10027"
  wait_for_health "gateway-service" "10010"

  info "Done. Gateway: http://127.0.0.1:10010"
  info "Logs: ${LOG_DIR}"
  info "Stop command: ${ROOT_DIR}/scripts/stop-microservices.sh"
}

main "$@"
