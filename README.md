# SWRewards

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Folia](https://img.shields.io/badge/Folia-supported-blueviolet)](https://papermc.io/software/folia)

**SWRewards** — многофункциональный плагин для Paper 1.21+, который автоматически выдаёт награды игрокам за проведённое на сервере время.  
Поддерживает **Folia**, **AFK-детект**, **базу данных (H2 / SQLite / MySQL)**, **HEX-цвета**, **PlaceholderAPI**, **мультиязычность** и систему для вернувшихся игроков.

---

## ✨ Возможности

| Возможность | Описание |
|-------------|----------|
| 🎁 **Настраиваемые награды** | Любое количество наград в `rewards.yml` с индивидуальным периодом |
| ⏱ **Периоды времени** | `30s`, `5m`, `1h`, `2h`, `7d` — секунды, минуты, часы, дни |
| 🛌 **AFK-детект** | Время AFK не засчитывается (настраивается для каждой награды) |
| ⏰ **Временные интервалы** | Награды доступны только в указанный промежуток времени (например, `18:00-22:00`) |
| 🔐 **Права (permissions)** | Награды могут требовать определённое право |
| 🔁 **Repetitive** | Награды могут выдаваться раз в период или единожды за наигранное время |
| 🏆 **Welcome-back** | Награда для игроков, вернувшихся после долгого отсутствия |
| 💾 **База данных** | H2 (по умолчанию), SQLite или MySQL — всё сохраняется в БД |
| 🌐 **Мультиязычность** | Русский / Английский язык сообщений (переключается в config.yml) |
| 🎨 **HEX-цвета** | Поддержка `&#rrggbb` в сообщениях и конфигах |
| 📊 **PlaceholderAPI** | Плейсхолдеры `%swrewards_next_reward%`, `%swrewards_afk%` и др. |
| 🧩 **Folia** | Полная поддержка Folia через глобальные регион-шедулеры |

---

## 📥 Установка

1. Скачайте `SWRewards.jar` из [Releases](https://github.com/kapusta2039/SWRewards/releases)
2. Поместите JAR в папку `plugins/` вашего сервера
3. Перезапустите сервер
4. Настройте под свой сервер и перезагрузите конфигурацию:  
   `/swrewards reload`

---

## ⚙️ Файлы конфигурации

### `config.yml` — основные настройки

```yaml
options:
  lang: ru # Language -> Русский | en — English
  time_zone: "Europe/Moscow" # Часовой пояс -> Москва
  storage-method: H2 # Поддержка MySQL, H2, SQLite
  # Настройки подключения MySQL (Только для storage-method: MySQL)
  data:
    host: localhost
    port: 3306
    database: swrewards
    username: root
    password: ""
    pool-size: 10

  # Настройки AFK
  time_to_afk: "5m"
  afk:
    afk_actionbar: true # Оповещение об AFK в ActionBar
    afk_bossbar: true # Оповещение об AFK в BossBar
    afk_message: true # Оповещение об AFK в чат
```

### `rewards.yml` — настройки наград

```yaml
rewards:
  diamond:
    period: "1h"
    detect_afk: true
    time_range: "18:00-22:00"
    permission: "swrewards.vip"
    repetitive: true
    message: "&#7dc16a| &fДержи &#7dc16aалмазик &fза час игры здесь! &#7dc16a:>"
    commands:
      - "give %player_name% minecraft:diamond 1"

welcome-back:
  enabled: true # Включить / выключить функцию
  check-period: "7d" # Отсутствовал минимум 7 дней
  reward-mode: "ALL" # ALL — выполнить все команды | RANDOM — случайная команда из списка
  message: "&#7dc16a| &fИгрок &#7dc16a%player_name% &fвернулся спустя долгое время и получил награду!"
  commands:
    - "give %player_name% minecraft:diamond 1"
    - "give %player_name% minecraft:emerald 1"
```

---

## 📋 Команды

| Команда | Описание | Право |
|---------|----------|-------|
| `/swrewards reload` | Безопасная перезагрузка конфигурации | `swrewards.reload` |

Алиасы: `/swreward reload`, `/swr reload`

### Права (permissions)

| Узел | Описание | По умолчанию |
|------|----------|-------------|
| `swrewards.reload` | Перезагрузка конфигурации | op |
| `swrewards.admin` | Все админ-функции (включая reload) | op |

---

## 📊 PlaceholderAPI

При установленном PlaceholderAPI доступны плейсхолдеры:

| Плейсхолдер | Описание |
|-------------|----------|
| `%swrewards_next_reward%` | Время до ближайшей доступной награды |
| `%swrewards_next_reward_<id>%` | Время до конкретной награды (например, `%swrewards_next_reward_diamond%`) |
| `%swrewards_afk%` | `true` / `false` — в AFK ли игрок |

---