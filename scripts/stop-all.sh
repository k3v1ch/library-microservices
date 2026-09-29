#!/usr/bin/env bash
# Остановка всех сервисов, запущенных через run-all.sh.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

if [ -f logs/pids ]; then
  while read -r pid name; do
    if kill "$pid" 2>/dev/null; then
      echo "остановлен $name (pid $pid)"
    fi
  done < logs/pids
  rm -f logs/pids
else
  echo "Файл logs/pids не найден — пробую найти процессы по имени jar"
  pkill -f "library-microservices/.*-service-1.0.0.jar" 2>/dev/null
  pkill -f "library-microservices/api-gateway" 2>/dev/null
fi
echo "Готово."
