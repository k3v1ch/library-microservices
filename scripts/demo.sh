#!/usr/bin/env bash
# Сквозной сценарий через API Gateway: регистрация, книги, выдача, уведомление, возврат.
set -uo pipefail
GW="${GATEWAY:-http://localhost:8080}"
STAMP=$(date +%s)

field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | sed "s/.*\":\"//;s/\"$//"; }
num_field() { grep -o "\"$1\":[0-9]*" | head -1 | sed "s/.*://"; }
bool_field() { grep -o "\"$1\":\(true\|false\)" | head -1 | sed "s/.*://"; }
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
split "$(call POST /auth/login '' "$body")"
LIB_TOKEN=$(printf '%s' "$BODY" | field access_token)
show "HTTP $CODE, токен получен: ${LIB_TOKEN:0:32}..."

step "2. Регистрация читателя и вход"
LOGIN="reader-$STAMP"
body=$(printf '{"username":"%s","password":"reader-password"}' "$LOGIN")
split "$(call POST /auth/register '' "$body")"
show "HTTP $CODE, выданные роли: $(printf '%s' "$BODY" | grep -o '"roles":\[[^]]*\]')"
split "$(call POST /auth/login '' "$body")"
USER_TOKEN=$(printf '%s' "$BODY" | field access_token)
show "HTTP $CODE, токен читателя получен"

step "3. Читатель создаёт профиль (user-service)"
body=$(printf '{"fullName":"Иван Петров","email":"ivan.%s@example.com","phone":"+7 999 123-45-67"}' "$STAMP")
split "$(call POST /users "$USER_TOKEN" "$body")"
USER_ID=$(printf '%s' "$BODY" | field id)
show "HTTP $CODE, userId=$USER_ID, карта $(printf '%s' "$BODY" | field cardNumber)"

step "4. Библиотекарь добавляет книгу (book-service)"
body=$(printf '{"title":"Чистый код","author":"Роберт Мартин"}')
split "$(call POST /books "$LIB_TOKEN" "$body")"
BOOK_ID=$(printf '%s' "$BODY" | field id)
show "HTTP $CODE, bookId=$BOOK_ID, available=$(printf '%s' "$BODY" | bool_field available)"

step "5. Поиск книги без токена (список книг открыт всем)"
# q=код в percent-encoding: кириллицу в URL нужно кодировать
split "$(call GET '/books?q=%D0%BA%D0%BE%D0%B4' '')"
show "HTTP $CODE, найдено книг: $(printf '%s' "$BODY" | num_field totalElements)"

step "6. Попытка читателя добавить книгу — должна быть 403"
body=$(printf '{"title":"Пробная","author":"Никто"}')
split "$(call POST /books "$USER_TOKEN" "$body")"
show "HTTP $CODE (ожидаем 403 Forbidden)"

step "7. Выдача книги: сага «читатель → бронь книги → выдача»"
IDEMPOTENCY_KEY="demo-$STAMP"
body=$(printf '{"userId":"%s","bookId":"%s"}' "$USER_ID" "$BOOK_ID")
split "$(call POST /borrow "$LIB_TOKEN" "$body" "Idempotency-Key: $IDEMPOTENCY_KEY")"
BORROW_ID=$(printf '%s' "$BODY" | field id)
show "HTTP $CODE, borrowId=$BORROW_ID, статус $(printf '%s' "$BODY" | field status), вернуть до $(printf '%s' "$BODY" | field dueDate)"

step "8. Повтор того же запроса с тем же Idempotency-Key"
split "$(call POST /borrow "$LIB_TOKEN" "$body" "Idempotency-Key: $IDEMPOTENCY_KEY")"
BORROW_ID_2=$(printf '%s' "$BODY" | field id)
if [ "$BORROW_ID" = "$BORROW_ID_2" ]; then
  show "HTTP $CODE, вернулась та же выдача — вторая не создана"
else
  show "ВНИМАНИЕ: создана вторая выдача $BORROW_ID_2"
fi

step "9. Книга больше не доступна"
split "$(call GET "/books/$BOOK_ID" '')"
show "HTTP $CODE, available=$(printf '%s' "$BODY" | bool_field available)"

step "10. Читатель смотрит свои выдачи"
split "$(call GET /borrow/my "$USER_TOKEN")"
show "HTTP $CODE, выдач у читателя: $(printf '%s' "$BODY" | num_field totalElements)"

step "11. Событие borrow.issued доходит до notification-service (outbox → relay)"
sleep 4
split "$(call GET "/notifications?userId=$USER_ID" "$LIB_TOKEN")"
show "HTTP $CODE"
printf '%s' "$BODY" | grep -o '"subject":"[^"]*","body":"[^"]*"' \
  | sed 's/"subject":"/   тема: /;s/","body":"/ | текст: /;s/"$//'

step "12. Журнал событий выдачи (event sourcing, аудит)"
split "$(call GET "/borrow/$BORROW_ID/history" "$LIB_TOKEN")"
show "HTTP $CODE, события: $(printf '%s' "$BODY" | grep -o '"type":"[^"]*"' | sed 's/"type":"//;s/"//' | tr '\n' ' ')"

step "13. Возврат книги"
split "$(call POST "/borrow/$BORROW_ID/return" "$LIB_TOKEN")"
show "HTTP $CODE, статус $(printf '%s' "$BODY" | field status)"
split "$(call GET "/books/$BOOK_ID" '')"
show "книга снова available=$(printf '%s' "$BODY" | bool_field available)"

step "14. Уведомление о возврате"
sleep 4
split "$(call GET "/notifications?userId=$USER_ID" "$LIB_TOKEN")"
show "HTTP $CODE, всего уведомлений читателю: $(printf '%s' "$BODY" | grep -o '"eventId"' | wc -l | tr -d ' ')"
printf '%s' "$BODY" | grep -o '"subject":"[^"]*"' | sed 's/"subject":"/   тема: /;s/"$//'

step "15. Запрос без токена к защищённому ресурсу — 401"
split "$(call GET /borrow '')"
show "HTTP $CODE (ожидаем 401 Unauthorized)"

step "16. Внутренний API снаружи недоступен"
split "$(call GET "/internal/users/$USER_ID" "$LIB_TOKEN")"
show "HTTP $CODE (ожидаем 403 — шлюз не публикует /internal/**)"

printf "\n\033[1mСценарий пройден.\033[0m Логи сервисов: logs/*.log\n"
