# 🗓️ Adonnis — AI Life Planner App — Master Checklist

**50 items. Complete them in order. Each item builds on the last. No skipping.**

---

## PHASE 1: PROJECT SCAFFOLD (Items 1–5)

### ☐ 1 — Create Android project structure
- Android project root with `settings.gradle.kts`, `build.gradle.kts` (project level), `local.properties`
- `app/build.gradle.kts` with:
  - `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`
  - Kotlin enabled, Jetpack Compose enabled
  - `buildFeatures { compose = true }`
- `gradle.properties`, `gradle-wrapper.properties` (use latest stable Gradle)
- `app/src/main/AndroidManifest.xml` with base permissions
- Proper package namespace (e.g., `com.adonnis.app`)

### ☐ 2 — Add all dependencies
- Jetpack Compose (BOM)
- Material 3 Compose
- Navigation Compose
- Room (runtime, ktx, compiler via KSP)
- Gemini API (`com.google.ai.client.generativeai:generativeai`)
- DataStore / EncryptedSharedPreferences
- AlarmManager, notifications compat
- WorkManager (for background scheduling)
- CameraX (for timetable photo capture) — optional v0, manual entry first
- ViewModel + Lifecycle
- KSP plugin for Room
- Kotlinx Serialization or Gson

### ☐ 3 — Create package structure
```
com.adonnis.app/
├── di/                  # Dependency injection (manual or Hilt)
├── data/
│   ├── local/           # Room DB, DAOs, entities
│   ├── preferences/     # DataStore / SharedPrefs
│   └── repository/      # Repository implementations
├── domain/
│   ├── model/           # Domain models
│   └── repository/      # Repository interfaces
├── ui/
│   ├── navigation/      # Nav graph setup
│   ├── onboarding/      # Setup screens
│   ├── chat/            # Main chat screen
│   ├── diary/           # End-of-day diary
│   ├── planner/         # 3-day plan display
│   ├── alarm/           # Alarm + math challenge
│   ├── settings/        # Settings screen
│   └── theme/           # Material 3 theme
├── ai/
│   ├── GeminiClient.kt
│   └── Prompts.kt       # All prompt templates
├── alarm/
│   ├── AlarmScheduler.kt
│   ├── AlarmReceiver.kt
│   └── MathChallengeActivity.kt
└── MainActivity.kt
```

### ☐ 4 — Custom Material 3 theme
- `Color.kt` — define primary, secondary, tertiary, error, surface, backgrounds
- `Theme.kt` — light/dark color schemes, typography, shapes
- `Type.kt` — custom typography (headline, body, label, etc.)
- Dynamic color support (Material You) on Android 12+

### ☐ 5 — Navigation graph skeleton
- NavHost with all routes defined (but screens are stubs):
  - `onboarding` → `chat` → `planner` → `diary` → `settings`
- Pass arguments between screens (API key, agent name, etc.)
- Deep link support for alarm

---

## PHASE 2: DATA LAYER (Items 6–10)

### ☐ 6 — Room database entities
- `UserEntity` — id, name, agentName, apiKey (encrypted), timetableRaw, createdAt
- `PlanEntity` — id, date, dayRelative (0=today,1=tomorrow,2=day3), planJson, createdAt, updatedAt
- `DiaryEntryEntity` — id, date, content, goalsList, futureEventsList, createdAt
- `ReminderEntity` — id, title, description, dateTime, isCompleted
- `AlarmEntity` — id, label, time, isEnabled, mathDifficulty

### ☐ 7 — Room DAOs
- `UserDao` — insert, get, update, delete
- `PlanDao` — insert, getByDate, getRange(start,end), update, delete
- `DiaryEntryDao` — insert, getByDate, getLatest, getAllOrdered
- `ReminderDao` — insert, getAll, getUpcoming, markCompleted, delete
- `AlarmDao` — insert, getEnabled, update, delete

### ☐ 8 — Room database class
- `AppDatabase` — abstract class extending RoomDatabase
- All entities and DAOs registered
- Migration strategy (fallbackToDestructiveMigration for dev)
- Type converters for LocalDateTime, List<String> etc.

### ☐ 9 — Preferences/local storage
- `PreferencesManager` — wrapper around EncryptedSharedPreferences or DataStore
- Save/load: API key (encrypted), user name, agent name, onboarding complete flag
- Base64 encode/decode for timetable text

### ☐ 10 — Repository layer
- `UserRepository` — combines UserDao + PreferencesManager
- `PlanRepository` — PlanDao with date range queries
- `DiaryRepository` — DiaryEntryDao
- `ReminderRepository` — ReminderDao, scheduling
- `AlarmRepository` — AlarmDao + AlarmScheduler

---

## PHASE 3: ONBOARDING (Items 11–17)

### ☐ 11 — Onboarding screen: API key entry
- Text input field for Google Gemini API key
- "Test connection" button that pings Gemini
- Loading spinner during test, success/error feedback
- Save key to encrypted storage on success
- Animated transition to next screen

### ☐ 12 — Onboarding screen: User name & Agent name
- "What's your name?" — text input
- "What would you like to call your assistant?" — text input
- Agent name defaults to "Adonnis" but user can change
- Both fields validated (non-empty, sanitized)
- Preview text: "Nice to meet you, [Name]! I am [AgentName], your AI life planner."

### ☐ 13 — Onboarding screen: Timetable entry
- Two options: (a) Type it manually, (b) Take a photo (CameraX)
- Manual entry: Multi-line text field with hints (format guide)
- Photo: Camera preview → capture → Gemini vision API → structured timetable
- Parse timetable into structured text via Gemini
- Preview & edit parsed timetable before saving
- Store final timetable in UserEntity

### ☐ 14 — Onboarding screen: Initial goals & priorities
- "What are your top 3 biggest goals right now?" — 3 text fields
- "What classes/subjects are you taking?"
- "Any ongoing commitments (part-time job, sports, family duties)?"
- All saved to UserEntity for future prompt context

### ☐ 15 — Onboarding screen: Sleep & wake preferences
- "What time do you usually wake up?"
- "What time do you usually go to bed?"
- "How many hours of sleep do you need?"
- "Do you have a morning routine? (gym, reading, etc.)"
- Save to UserEntity

### ☐ 16 — Onboarding screen: Summary & confirmation
- Display all collected info as cards
- "Is everything correct?" — Yes / No (goes back to edit)
- "Ready to start!" button transitions to main chat
- Set `onboardingComplete = true` in preferences

### ☐ 17 — Onboarding flow complete
- Navigation guard: if onboarding not complete → redirect to onboarding
- No back button from chat to onboarding
- User can reset onboarding from settings later

---

## PHASE 4: CHAT INTERFACE (Items 18–22)

### ☐ 18 — Chat screen UI
- Material 3 Compose chat layout
- Scrollable message list (LazyColumn, newest at bottom)
- Message bubbles: user = right-aligned, agent = left-aligned
- Avatar icon for agent (customizable? or default bot icon)
- Timestamps on each message (optional, toggle)
- Typing indicator ("[AgentName] is thinking...")

### ☐ 19 — Message input bar
- Text field with send button
- Attachment button (for photos, files)
- Send on Enter keyboard action
- Character limit display (4096 chars)
- Text field grows with content (max 4 lines)
- Disable send while AI is responding

### ☐ 20 — Message model & history
- `ChatMessage` data class: id, role(user/agent/system), content, timestamp, type(text/image/system)
- Room entity for message persistence
- DAO: insert, getHistory(limit, offset), deleteAll
- Load last N messages on app start (pagination)
- "Clear chat" option in overflow menu (with confirmation)

### ☐ 21 — Chat view model
- `ChatViewModel` — holds message list as StateFlow
- `sendMessage(text)` — adds user msg, calls Gemini, adds AI response
- Loading state during API call
- Error handling (show error bubble with retry button)
- Auto-scroll to bottom on new messages

### ☐ 22 — Context injection system
- Before each Gemini call, inject context into system prompt:
  - User name, agent name
  - Timetable for today/tomorrow/day after
  - Current plan for next 3 days
  - Latest diary entry
  - Top goals
- Update context automatically when data changes

---

## PHASE 5: GEMINI AI INTEGRATION (Items 23–27)

### ☐ 23 — Gemini API client setup
- `GeminiClient` class using `Google AI Gemini SDK`
- Initialize with user's API key
- `generateResponse(history, systemPrompt, userMessage)` → returns text
- Safety settings: set to lowest blocking for personal use
- Generation config: temperature 0.7, max output tokens 2048

### ☐ 24 — Chat history formatting
- Convert stored ChatMessage list to Gemini's `Content` format
- Alternate user/model roles correctly
- Trim history to stay within context window (last 50 messages)
- System instruction prepended to every request

### ☐ 25 — System prompt templates
- `Prompts.kt` — all prompt templates as constants/functions
- **Greeting prompt**: "You are [AgentName], a friendly AI life planner..."
- **Planner prompt**: "Given the timetable..., create a 3-day plan..."
- **Diary prompt**: "Ask the user about their day..."
- **Alarm math prompt**: "Generate 10 basic BODMAS equations..."
- All prompts include user's name, goals, and context

### ☐ 26 — Structured output parsing
- Gemini returns JSON for plans → parse into PlanEntity
- Gemini returns JSON for diary → extract diary content, goals, future events
- Gemini returns math equations → parse into question/answer pairs
- Fallback: if JSON fails, attempt regex extraction
- Store raw response as backup

### ☐ 27 — Retry & fallback logic
- If Gemini API call fails (network, rate limit, invalid key):
  - Retry 2 times with exponential backoff
  - Show error message to user: "I'm having trouble connecting..."
  - Option to check API key / retry
- Graceful degradation: if no internet, use cached plans

---

## PHASE 6: 3-DAY PLANNER (Items 28–32)

### ☐ 28 — Daily plan generation
- Each morning (or on first launch), Gemini generates a 3-day rolling plan
- Plan structure: Time blocks with activity, duration, notes
- **Tomorrow**: 30-minute blocks, detailed
- **Day after**: 1-hour blocks, moderate detail
- **Day 3**: 2-hour blocks, high-level
- Plan stored in Room with date + dayRelative index

### ☐ 29 — Plan refinement as days approach
- When a new diary entry is added, regenerate plan for remaining days
- When user mentions a future event, update the plan
- When today ends, shift: old tomorrow → new today, old day3 → new tomorrow, generate new day3
- On each refinement, keep what's already committed, add more details

### ☐ 30 — Plan display screen
- Day selector tabs (Today / Tomorrow / Day 3)
- Timeline view: time blocks with color coding (study=blue, rest=green, meals=orange, social=purple)
- Tap a block to see notes / edit
- "Regenerate plan" button at bottom
- Summary stats at top: total study hours, breaks, sleep

### ☐ 31 — Plan integration with chat
- When user asks "What's my plan for today?" → show plan inline in chat
- Agent automatically references plan: "According to your plan, you have study from 2-4pm"
- User can say "Move my 2pm study to 4pm" → Gemini updates plan → saves
- Agent confirms changes: "Done! I've moved your study session."

### ☐ 32 — Plan persistence & versioning
- Each plan version stored with timestamp
- Only latest version shown, but previous versions available in history
- Auto-save plan every time it's generated or modified
- Export plan as text (for sharing/printing)

---

## PHASE 7: DIARY SYSTEM (Items 33–37)

### ☐ 33 — End-of-day diary prompt
- At user-set time (default 8pm), notification: "Time for your daily diary, [Name]!"
- Opens chat with automated diary session start
- Gemini asks structured questions:
  - "How was your day? Tell me everything."
  - "What went well? What didn't?"
  - "Did you follow today's plan? What changed?"
  - "Any thoughts or feelings you want to note?"

### ☐ 34 — Future events & goals capture
- Diary session continues:
  - "Any upcoming events I should know about?"
  - "Any tests, exams, or deadlines coming up?"
  - "How are you progressing on your top goals?"
  - "Any new goals or priorities?"
- User responds naturally in chat
- Gemini extracts structured data: events (date + description), goals (text + priority)

### ☐ 35 — Diary entry storage & formatting
- Save complete diary conversation as DiaryEntryEntity
- Extract structured fields: content summary, goals list, future events list, mood
- Display diary as scrollable "journal entry" — formatted markdown-like
- Calendar view: dots on days with diary entries, tap to read
- Edit/append to today's diary entry

### ☐ 36 — Goal tracking
- Goals extracted from diary sessions stored in dedicated table
- Priority ranking (user can reorder in settings/chat)
- Agent references goals: "Your top goal is [X]. How did you work toward it today?"
- Weekly goal progress summary generated every Sunday
- Celebrate milestones: "You've been consistent with [goal] for 7 days!"

### ☐ 37 — Diary review & insights
- "Weekly recap" generated by Gemini every 7 days
- Insights: "You've been most productive on Tuesdays", "You tend to skip breakfast on school days"
- Mood tracking over time (extract sentiment from diary entries)
- Patterns identified by AI, presented in chat

---

## PHASE 8: REMINDERS & NOTIFICATIONS (Items 38–42)

### ☐ 38 — Reminder creation
- User can type: "Remind me to submit homework at 5pm tomorrow"
- Gemini detects intent → calls reminder creation function
- Or use dedicated "Add Reminder" button in chat and settings
- Reminder fields: title, description, dateTime, repeat (none/daily/weekly)
- Reminder list screen with toggle on/off

### ☐ 39 — Reminder scheduling engine
- `AlarmScheduler` — wraps Android's `AlarmManager`
- Schedule using `setExactAndAllowWhileIdle` for precise timing
- Use `BroadcastReceiver` to trigger notification
- Show notification with snooze (5 min) and "Done" action
- Permission check for exact alarms (Android 14+)

### ☐ 40 — Agent auto-reminders
- Based on plan: if plan says "Study at 2pm", auto-create reminder 5min before
- Based on timetable: "You have Math class at 9am tomorrow" → reminder
- Based on goals: "You haven't worked on [goal] in 3 days" → gentle nudge
- All auto-reminders marked with a flag so user knows they're AI-generated

### ☐ 41 — Notification channels
- `adonnis_reminders` — for regular reminders
- `adonnis_diary` — for end-of-day diary prompt
- `adonnis_alarm` — for wake-up alarm
- `adonnis_insights` — for weekly recaps and insights
- Each with proper importance, description, sound settings

### ☐ 42 — Notification deep linking
- Tap reminder notification → opens chat with context
- Tap diary notification → opens diary chat session
- Tap alarm notification → opens MathChallengeActivity (full screen, no dismiss)
- Tap insight notification → opens weekly recap

---

## PHASE 9: ALARM & MATH CHALLENGE (Items 43–47)

### ☐ 43 — Alarm setup screen
- Set alarm time (TimePicker)
- Select days (Sun–Sat checkboxes)
- Alarm label (e.g., "Wake up!")
- Sound selection (system alarm tones) + volume
- "Math challenge" difficulty slider (Easy / Medium / Hard)
- Toggle: "Strict mode" — alarm won't stop until equations solved

### ☐ 44 — Alarm service & receiver
- `AlarmReceiver` extends `BroadcastReceiver`
- On receive: acquire wake lock, start `AlarmForegroundService`
- Service: show persistent notification, start `MathChallengeActivity`
- Activity launches as full-screen intent, turns screen on, bypasses lock screen
- Use `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`
- Use `PowerManager.WAKE_LOCK` (partial) to keep CPU awake

### ☐ 45 — Math challenge screen
- Full-screen, bright colors, pulsating background
- Large label: "WAKE UP, [NAME]!"
- Shows agent's name/mascot
- 10 BODMAS equations displayed one at a time
- Text input for answer, "Submit" button
- Correct → green flash, next question
- Wrong → red shake, try again (same question)
- Progress indicator: "3/10 completed"

### ☐ 46 — Equation generation
- On alarm trigger: call Gemini with prompt: "Generate 10 BODMAS math equations with answers"
- Gemini returns JSON array: `[{question: "12 + 5 × 3", answer: 27}]`
- Difficulty levels: Easy (2 operators), Medium (3 operators), Hard (3+ operators with brackets)
- BODMAS enforced: brackets, orders, division, multiplication, addition, subtraction
- Cache equations in AlarmEntity for the alarm's lifecycle
- Fallback: hardcoded equation bank if no internet

### ☐ 47 — Alarm dismissal & snooze
- After 10 correct answers: celebration animation (confetti, "You're awake!")
- Dismiss button appears → alarm service stops
- Snooze: shake phone or tap snooze → repeats in 5 minutes
- Max snoozes: 3 (after that, math difficulty increases or alarm goes nuclear)
- Log: "Math challenge completed in X minutes, X attempts"
- After dismiss: Gemini morning greeting in chat: "Good morning, [Name]! Ready for today?"

---

## PHASE 10: POLISH & EDGE CASES (Items 48–50)

### ☐ 48 — Error handling & edge cases
- **No internet**: show offline indicator, use cached data, queue Gemini calls
- **Invalid API key**: notification + prompt to update in settings
- **App killed**: restore state on relaunch (alarms still scheduled via AlarmManager)
- **Permission denied**: rational dialog, graceful degradation
- **Storage full**: Room handles gracefully, show warning
- **Date/timezone handling**: use UTC internally, display in local time, handle DST changes
- **Rapid taps**: debounce send button to prevent duplicate messages
- **Very long messages**: truncate with "Continue" expand option

### ☐ 49 — Settings & configuration
- Settings screen with all configurable options:
  - Update API key
  - Change user name / agent name
  - Redo timetable
  - Update goals
  - Change sleep schedule
  - Notification toggles per channel
  - Alarm settings
  - Diary time setting
  - Clear all data (with confirmation + onboarding reset)
- About screen: app version, credits, open source licenses

### ☐ 50 — Final integration test & ship-ready
- Full flow test: onboarding → chat → plan generation → reminders → diary → alarm
- All navigation paths tested (back stack, no double screens)
- Dark mode toggle works (follow system or manual)
- Screen rotation: state preserved (no crashes)
- App icon created (adaptive icon, foreground + background layers)
- App name "Adonnis" or user-chosen name in launcher
- Keyboard handling: dismiss on scroll, proper insets
- Minimal memory footprint: no leaks (profiled)
- `proguard-rules.pro` configured for release build
- Signed APK ready: `./gradlew assembleRelease`

---

## ✅ Complete! Ready to build.

```
Total items: 50
Phases:     10
```
