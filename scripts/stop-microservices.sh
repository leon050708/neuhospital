#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="${ROOT_DIR}/logs/pids"

stop_service() {
  local module="$1"
  local pid_file="${PID_DIR}/${module}.pid"

  if [[ ! -f "${pid_file}" ]]; then
    printf '[INFO] %s has no PID file; skip.\n' "${module}"
    return
  fi

  local pid
  pid="$(cat "${pid_file}")"
  if kill -0 "${pid}" >/dev/null 2>&1; then
    printf '[INFO] Stopping %s PID %s\n' "${module}" "${pid}"
    kill "${pid}"
  else
    printf '[INFO] %s PID %s is not running.\n' "${module}" "${pid}"
  fi
  rm -f "${pid_file}"
}

stop_service "gateway-service"
stop_service "outpatient-service"
stop_service "inspection-service"
stop_service "pharmacy-service"
stop_service "payment-service"
stop_service "registration-service"
stop_service "doctor-service"
stop_service "patient-service"
stop_service "file-service"
stop_service "backend-service"

printf '[INFO] Java services stopped. Docker infra is left running.\n'
printf '[INFO] To stop Redis/Kafka/MinIO: docker compose -f %s/infra/compose.yml stop redis kafka minio\n' "${ROOT_DIR}"
