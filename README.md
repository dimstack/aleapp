# aleapp

Android-клиент и backend для CallApp.

## Установка backend

Запуск установщика напрямую из GitHub:

```bash
bash <(curl -fsSL https://raw.githubusercontent.com/dimstack/aleapp/main/server/install.sh)
```

Скрипт:

- при необходимости установит Docker
- развернёт backend на сервере
- при повторном запуске предложит обновить контейнеры с сохранением данных или полностью переустановить сервер после явного подтверждения
