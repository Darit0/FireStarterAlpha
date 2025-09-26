# Firestarter — система двухэтапной обработки Excel-файлов

## Цель проекта
Система принимает Excel-файлы, проводит:
1. **Первичную валидацию** (размер ≤5 МБ, расширение `.xls`/`.xlsx`)
2. **Вторичную валидацию** (на первом листе заполнены ячейки A1:C2)

Статусы файлов сохраняются в MongoDB и доступны через REST API.

## Архитектура
- `file-uploader` — принимает файлы по HTTPS, валидирует, отправляет в Kafka
- `file-processor` — читает из `upload-topic`, валидирует содержимое, отправляет статус
- `file-status-processor` — читает из `status-topic`, обновляет MongoDB
- `config-server` — централизованная конфигурация
- **Kafka** — обмен сообщениями
- **MongoDB** — хранение статусов

### Требования
- Docker + Docker Compose
- JDK 17
- Gradle

## Запуск
Запуск инфраструктуры:
```bash
docker-compose up -d
```
Соберите и запустите микросервисы:
```bash
# config-server
./gradlew :config-server:bootRun

# file-uploader
./gradlew :file-uploader:bootRun

# file-processor
./gradlew :file-processor:bootRun

# file-status-processor
./gradlew :file-status-processor:bootRun
```

## Для проверки работы мини-колллекция Postman
https://www.postman.com/daritoss/interntask/collection/w6e3ol3/firestartertests?action=share&creator=34097842

## Альтернативный варинт проверки эндпоинтов
Загрузка файла
```bash
curl -k -X POST https://localhost:8081/upload \
  -F "file=@test.xlsx" \
  -v
```
Проверка статуса файла
```bash
curl -k https://localhost:8081/status/ваш_хеш_файла
```


