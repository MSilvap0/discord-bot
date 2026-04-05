#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${PROJECT_DIR}/.env"
JAR_FILE="${PROJECT_DIR}/target/discord-bot-reminder-1.0-SNAPSHOT.jar"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

if [[ -z "${DISCORD_TOKEN:-}" ]]; then
  echo "Erro: defina DISCORD_TOKEN no ambiente ou no arquivo .env"
  exit 1
fi

if [[ ! -f "${JAR_FILE}" ]]; then
  echo "Jar nao encontrado. Rode: mvn package"
  exit 1
fi

cd "${PROJECT_DIR}"
exec java -jar "${JAR_FILE}"
