# SWRewards

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

Плагин для автоматической выдачи наград игрокам с настраиваемым периодом.

## ✨ Преимущества
- Настройка любого количества наград
- Гибкая система периодов (секунды, минуты, часы, дни)
- Поддержка Folia
- Автосохранение данных

## 📥 Установка
1. Скачайте `SWRewards.jar` из [Releases](https://github.com/kapusta2039/SWRewards/releases)
2. Поместите в папку `plugins/` вашего сервера
3. Перезапустите сервер
4. Настройте `plugins/SWRewards/config.yml`

## ⚙️ Конфигурация
```yaml
rewards:
  daily:
    period: "1d"
    message: "&aВы получили ежедневную награду!"
    commands:
      - "give %player_name% diamond 1"
