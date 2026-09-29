-- База данных на каждый сервис: в одном инстансе Postgres для простоты запуска,
-- но без общих таблиц — сервис видит только свою схему.
CREATE DATABASE auth;
CREATE DATABASE catalog;
CREATE DATABASE readers;
CREATE DATABASE circulation;
CREATE DATABASE notifications;
