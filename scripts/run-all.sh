#!/usr/bin/env bash
# Запуск всех сервисов локально: H2 в памяти, без Docker и без Kafka.
# Порядок важен: сначала аутентификация и данные, потом выдача, последним шлюз.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
ROOT="$PWD"
LOGS="$ROOT/logs"
mkdir -p "$LOGS"

find_java() {
  local candidates=()
  [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME/bin/java")
  command -v java >/dev/null 2>&1 && candidates+=("$(command -v java)")
  # JDK, который идёт вместе с IntelliJ IDEA / Android Studio
  candidates+=(
    "/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home/bin/java"
    "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home/bin/java"
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java"
  )
  for candidate in "${candidates[@]}"; do
    # На macOS /usr/bin/java существует всегда, но без JDK только ругается — проверяем запуск
    if [ -x "$candidate" ] && "$candidate" -version >/dev/null 2>&1; then
      echo "$candidate"; return
    fi
  done
  echo ""
}

JAVA="$(find_java)"
if [ -z "$JAVA" ]; then
  echo "Не найден Java 21. Установите JDK 21 или задайте JAVA_HOME." >&2
  exit 1
fi
echo "Java: $("$JAVA" -version 2>&1 | head -1)"

# name:port — уведомления поднимаем раньше выдачи, чтобы первое же событие дошло сразу
SERVICES=(
  "auth-service:8081"
  "catalog-service:8082"
  "readers-service:8083"
  "notification-service:8085"
  "circulation-service:8084"
  "api-gateway:8080"
)

wait_up() {
  local name="$1" port="$2"
  for _ in $(seq 1 90); do
    if curl -fsS "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
      echo "   готов: $name (порт $port)"
      return 0
    fi
    sleep 1
  done
  echo "   НЕ поднялся: $name — смотрите logs/$name.log" >&2
  return 1
}

: > "$LOGS/pids"
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  jar=$(ls "$ROOT/$name/target/$name-"*.jar 2>/dev/null | head -1)
  if [ -z "$jar" ]; then
    echo "Нет собранного jar для $name. Сначала: ./mvnw -DskipTests package" >&2
    exit 1
  fi
  echo "-> запускаю $name"
  nohup "$JAVA" -jar "$jar" > "$LOGS/$name.log" 2>&1 &
  echo "$! $name" >> "$LOGS/pids"
  wait_up "$name" "$port" || exit 1
done

cat <<'INFO'

Все сервисы работают.
  Шлюз (единая точка входа):  http://localhost:8080
  Swagger каталога:           http://localhost:8082/swagger-ui.html
  Swagger выдачи:             http://localhost:8084/swagger-ui.html
  Метрики выдачи:             http://localhost:8084/actuator/prometheus

Сценарий целиком:  ./scripts/demo.sh
Остановить:        ./scripts/stop-all.sh
Логи:              logs/<имя-сервиса>.log
INFO
