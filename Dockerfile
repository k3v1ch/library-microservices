# Один Dockerfile на все сервисы: имя модуля передаётся аргументом сборки.
# Jar должен быть собран заранее: ./mvnw -DskipTests package
FROM eclipse-temurin:21-jre
ARG SERVICE
WORKDIR /app
COPY ${SERVICE}/target/${SERVICE}-1.0.0.jar /app/app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
