
FROM openjdk:21-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app
# Копируем JAR-файл из этапа сборки
COPY build/libs/TechSupBot-0.0.1.jar techsupbot.jar
# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "techsupbot.jar"]