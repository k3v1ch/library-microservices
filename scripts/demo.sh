#!/usr/bin/env bash
# Сквозной сценарий через API Gateway: регистрация, каталог, выдача, уведомление, возврат.
# Каждый шаг печатает код ответа — видно, что работает авторизация, сага и события.
set -uo pipefail
GW="${GATEWAY:-http://localhost:8080}"
STAMP=$(date +%s)

field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | sed "s/.*\":\"//;s/\"$//"; }
num_field() { grep -o "\"$1\":[0-9]*" | head -1 | sed "s/.*://"; }
step() { printf "\n\033[1m== %s\033[0m\n" "$1"; }
show() { printf "   %s\n" "$1"; }

call() { # call METHOD PATH [TOKEN] [BODY] [EXTRA_HEADER]
  local method="$1" path="$2" token="${3:-}" body="${4:-}" extra="${5:-}"
  local args=(-s -w '\n%{http_code}' -X "$method" "$GW$path" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$extra" ] && args+=(-H "$extra")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}"
}

split() { # разделяет ответ на тело (BODY) и код (CODE)
  BODY=$(printf '%s' "$1" | sed '$d')
  CODE=$(printf '%s' "$1" | tail -1)
}

if ! curl -fsS "$GW/actuator/health" >/dev/null 2>&1; then
  echo "Шлюз $GW не отвечает. Сначала: ./scripts/run-all.sh" >&2
  exit 1
fi

step "1. Вход библиотекаря (учётная запись создаётся при старте auth-service)"
body=$(printf '{"username":"librarian","password":"librarian-pass"}')
split "$(call POST /api/v1/auth/login '' "$body")"
LIB_TOKEN=$(printf '%s' "$BODY" | field access_token)
show "HTTP $CODE, токен получен: ${LIB_TOKEN:0:32}..."

step "2. Регистрация читателя и вход"
READER_LOGIN="reader-$STAMP"
body=$(printf '{"username":"%s","password":"reader-password"}' "$READER_LOGIN")
split "$(call POST /api/v1/auth/register '' "$body")"
show "HTTP $CODE, выданные роли: $(printf '%s' "$BODY" | grep -o '"roles":\[[^]]*\]')"
split "$(call POST /api/v1/auth/login '' "$body")"
READER_TOKEN=$(printf '%s' "$BODY" | field access_token)
show "HTTP $CODE, токен читателя получен"

step "3. Читатель создаёт профиль (сервис читателей)"
body=$(printf '{"fullName":"Иван Петров","email":"ivan.%s@example.com","phone":"+7 999 123-45-67"}' "$STAMP")
split "$(call POST /api/v1/readers "$READER_TOKEN" "$body")"
READER_ID=$(printf '%s' "$BODY" | field id)
show "HTTP $CODE, readerId=$READER_ID, карта $(printf '%s' "$BODY" | field cardNumber)"

step "4. Библиотекарь добавляет книгу в каталог"
body=$(printf '{"isbn":"978-5-4461-%s","title":"Чистый код","author":"Роберт Мартин","genre":"IT","publishedYear":2008,"totalCopies":2}' "$((RANDOM + 1000))")
split "$(call POST /api/v1/books "$LIB_TOKEN" "$body")"
BOOK_ID=$(printf '%s' "$BODY" | field id)
show "HTTP $CODE, bookId=$BOOK_ID, экземпляров $(printf '%s' "$BODY" | num_field availableCopies)"

step "5. Поиск по каталогу без токена (каталог открыт всем)"
# q=код в percent-encoding: кириллицу в URL нужно кодировать
split "$(call GET '/api/v1/books?q=%D0%BA%D0%BE%D0%B4' '')"
show "HTTP $CODE, найдено книг: $(printf '%s' "$BODY" | num_field totalElements)"

step "6. Попытка читателя добавить книгу — должна быть 403"
body=$(printf '{"isbn":"1234567890","title":"Пробная","author":"Никто","totalCopies":1}')
split "$(call POST /api/v1/books "$READER_TOKEN" "$body")"
show "HTTP $CODE (ожидаем 403 Forbidden)"

step "7. Выдача книги: сага «читатель → бронь экземпляра → выдача»"
IDEMPOTENCY_KEY="demo-$STAMP"
body=$(printf '{"readerId":"%s","bookId":"%s"}' "$READER_ID" "$BOOK_ID")
split "$(call POST /api/v1/loans "$LIB_TOKEN" "$body" "Idempotency-Key: $IDEMPOTENCY_KEY")"
LOAN_ID=$(printf '%s' "$BODY" | field id)
show "HTTP $CODE, loanId=$LOAN_ID, статус $(printf '%s' "$BODY" | field status), вернуть до $(printf '%s' "$BODY" | field dueDate)"

step "8. Повтор того же запроса с тем же Idempotency-Key"
split "$(call POST /api/v1/loans "$LIB_TOKEN" "$body" "Idempotency-Key: $IDEMPOTENCY_KEY")"
LOAN_ID_2=$(printf '%s' "$BODY" | field id)
if [ "$LOAN_ID" = "$LOAN_ID_2" ]; then
  show "HTTP $CODE, вернулась та же выдача — второй экземпляр не списан"
else
  show "ВНИМАНИЕ: создана вторая выдача $LOAN_ID_2"
fi

step "9. В каталоге стало меньше свободных экземпляров"
split "$(call GET "/api/v1/books/$BOOK_ID" '')"
show "HTTP $CODE, свободно $(printf '%s' "$BODY" | num_field availableCopies) из $(printf '%s' "$BODY" | num_field totalCopies)"

step "10. Читатель смотрит свои выдачи"
split "$(call GET /api/v1/loans/my "$READER_TOKEN")"
show "HTTP $CODE, выдач у читателя: $(printf '%s' "$BODY" | num_field totalElements)"

step "11. Событие LoanIssued доходит до сервиса уведомлений (outbox → relay)"
sleep 4
split "$(call "GET" "/api/v1/notifications?readerId=$READER_ID" "$LIB_TOKEN")"
show "HTTP $CODE"
printf '%s' "$BODY" | grep -o '"subject":"[^"]*","body":"[^"]*"' \
  | sed 's/"subject":"/   тема: /;s/","body":"/ | текст: /;s/"$//'

step "12. Журнал событий выдачи (event sourcing, аудит)"
split "$(call GET "/api/v1/loans/$LOAN_ID/history" "$LIB_TOKEN")"
show "HTTP $CODE, события: $(printf '%s' "$BODY" | grep -o '"type":"[^"]*"' | sed 's/"type":"//;s/"//' | tr '\n' ' ')"

step "13. Возврат книги"
split "$(call POST "/api/v1/loans/$LOAN_ID/return" "$LIB_TOKEN")"
show "HTTP $CODE, статус $(printf '%s' "$BODY" | field status)"
split "$(call GET "/api/v1/books/$BOOK_ID" '')"
show "в каталоге снова свободно: $(printf '%s' "$BODY" | num_field availableCopies)"

step "14. Уведомление о возврате"
sleep 4
split "$(call GET "/api/v1/notifications?readerId=$READER_ID" "$LIB_TOKEN")"
show "HTTP $CODE, всего уведомлений читателю: $(printf '%s' "$BODY" | grep -o '"eventId"' | wc -l | tr -d ' ')"
printf '%s' "$BODY" | grep -o '"subject":"[^"]*"' | sed 's/"subject":"/   тема: /;s/"$//'

step "15. Запрос без токена к защищённому ресурсу — 401"
split "$(call GET /api/v1/loans '')"
show "HTTP $CODE (ожидаем 401 Unauthorized)"

step "16. Внутренний API снаружи недоступен"
split "$(call GET "/internal/v1/readers/$READER_ID" "$LIB_TOKEN")"
show "HTTP $CODE (ожидаем 403 — шлюз не публикует /internal/**)"

printf "\n\033[1mСценарий пройден.\033[0m Логи сервисов: logs/*.log\n"
