# Adonnis

An AI-powered life planner for Android that learns your timetable, goals, and habits — then plans your days, reminds you of deadlines, journals with you, and makes sure you actually wake up.

## Features

- **AI chat companion** — a friendly agent that knows your name, timetable, goals, sleep schedule, and your current 3-day plan. Powered by [OpenRouter](https://openrouter.ai), so one API key gives access to hundreds of models (with automatic provider failover).
- **3-day rolling planner** — the AI generates a precise plan for today (30-min blocks), tomorrow (1-hour blocks), and the day after (high-level 2-hour blocks). Plans respect your timetable, sleep schedule, and goals — and get refined as the days approach.
- **Auto-reminders** — mention a deadline, exam, or appointment in chat and the agent silently appends a reminder tag to its reply. No manual setup needed.
- **Math-challenge alarm** — wake-up alarm that can only be dismissed by solving BODMAS equations, with difficulty levels and full-screen lock-screen takeover. The agent generates the equations on the fly.
- **End-of-day diary** — guided reflection sessions that extract goals, future events, and insights to make tomorrow's plan smarter.
- **Context injection** — before every AI request, the system prompt is built from your real data: timetable, current plan, latest diary entry, goals, and the actual calendar date.
- **Markdown rendering** — rich chat and diary output via compose-richtext.
- **Encrypted storage** — your OpenRouter API key is stored with `EncryptedSharedPreferences` (AES-256).

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material 3 (BOM 2024.02.00) |
| Architecture | MVVM — ViewModels + Repositories |
| Local DB | Room 2.6.1 (KSP) |
| Preferences | DataStore + EncryptedSharedPreferences |
| Background | WorkManager 2.9, AlarmManager, foreground service |
| AI | OpenRouter REST API (`HttpURLConnection` + `org.json`, no SDK) |
| Build | AGP 8.2.2, Gradle 8.5, `minSdk 26`, `targetSdk 35` |

## Getting started

### Prerequisites

- Android Studio (or a JDK 17 toolchain)
- Android SDK 35
- An [OpenRouter API key](https://openrouter.ai/keys) (`sk-or-v1-...`) — free-tier models available

### Build

```bash
git clone https://github.com/realEdwinJ/Agent-Adonnis.git
cd Agent-Adonnis
./gradlew assembleDebug
```

Open the project in Android Studio, run it on a device or emulator, and complete the onboarding flow to enter your API key, timetable, goals, and sleep schedule.

### Configuration

The app is fully configured from the settings screen:

- **API key** — validated against OpenRouter's `/auth/key` endpoint, no credits consumed
- **Model** — default `openrouter/auto` (automatic routing + failover); any OpenRouter model ID works, e.g. `deepseek/deepseek-chat-v3-0324:free` or `anthropic/claude-3.5-sonnet`
- **Agent name, user name, timetable, goals** — editable at any time

## Project structure

```
app/src/main/java/com/adonnis/app/
├── ai/                  # OpenRouter client, prompts, response parsing
├── alarm/               # Alarm scheduling, receiver, math challenge activity
├── reminder/            # Reminder scheduling + notifications
├── data/
│   ├── local/           # Room DB — entities, DAOs, database
│   ├── preferences/     # Encrypted preferences manager
│   └── repository/      # Repository layer
├── ui/
│   ├── onboarding/      # Setup flow
│   ├── chat/            # Main chat screen + ViewModel
│   ├── planner/         # 3-day plan display
│   ├── diary/           # Journal UI
│   ├── alarm/           # Alarm setup screen
│   ├── settings/        # Settings screen
│   ├── navigation/      # Nav graph + bottom bar
│   ├── components/      # Shared UI components
│   └── theme/           # Material 3 theme
└── util/                # Date parsing, network monitoring
```

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | OpenRouter API calls |
| `POST_NOTIFICATIONS` | Reminder and diary notifications |
| `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` | Precise alarm firing (Android 14+) |
| `WAKE_LOCK` / `FOREGROUND_SERVICE` | Alarm keeps screen on while math challenge is active |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarms/reminders after reboot |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE) © realEdwinJ
