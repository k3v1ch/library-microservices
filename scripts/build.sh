#!/usr/bin/env bash
# Сборка всех модулей. JDK ищется сам (в том числе тот, что идёт с IntelliJ IDEA),
# Maven скачивается wrapper'ом — ставить его отдельно не нужно.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

find_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version >/dev/null 2>&1; then
    echo "$JAVA_HOME"; return
  fi
  for candidate in \
    "/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home" \
    "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "$(/usr/libexec/java_home 2>/dev/null)"; do
    if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ] && "$candidate/bin/java" -version >/dev/null 2>&1; then
      echo "$candidate"; return
    fi
  done
  echo ""
}

JH="$(find_java_home)"
if [ -z "$JH" ]; then
  echo "Не найден JDK 21. Установите его или задайте JAVA_HOME." >&2
  exit 1
fi
export JAVA_HOME="$JH"
echo "JAVA_HOME=$JAVA_HOME"

if [ "${1:-}" = "--with-tests" ]; then
  ./mvnw -B clean install
else
  ./mvnw -B -DskipTests clean package
fi
