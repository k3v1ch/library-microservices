-- База данных на каждый сервис: в одном инстансе Postgres для простоты запуска,
-- но без общих таблиц — сервис видит только свою схему.
CREATE DATABASE auth;
CREATE DATABASE books;
CREATE DATABASE users;
CREATE DATABASE borrow;
CREATE DATABASE notifications;
