# StudyFocus – Implementation Plan

## Summary
StudyFocus is an all-in-one Android digital wellbeing and student productivity suite designed to help students reduce social media distractions, manage study time, write structured revision notes, perform scientific calculations, and leverage on-demand AI study assistance (summarization and key concept extraction).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following recommended architectural choices have been set as the defaults for implementation:
> - **AI Integration**: On-demand AI Study Assistant powered by Firebase AI Logic / Gemini for note summarization, key concept flashcards, and study habit recommendations with graceful offline fallback.
> - **Distraction Management**: Foreground UsageStats monitoring with interactive overlay lock screen ("Focus Shield") that offers students an instant study challenge or note-review session when an app limit is reached.
> - **Note Organization**: Hierarchical subject & module categorization with instant search, topic tags, and quick-access revision points.
> - **Scientific Calculator**: Full scientific calculation engine with trigonometry (deg/rad), powers, roots, logs, brackets, and calculation history.

---

## 1. Overview & Core Concept

- **What It Does**: StudyFocus brings together three fragmented workflows students face every day into a cohesive, distraction-free environment:
  1. **Focus Dashboard & App Time Limiter**: Real-time app usage tracking, custom daily quotas for distracting apps (Instagram, YouTube, etc.), predictive analytics on peak distraction hours, and a smart lock screen when quotas expire.
  2. **Smart Study Notes & AI Assistant**: Offline-first rich study notes categorized by subject and module, with one-tap AI summarization, bullet-point revision highlights, and quiz generator.
  3. **Embedded Scientific Calculator**: Seamless mathematical engine (basic arithmetic, trig, ln/log, exponent, square root, parentheses) so students never leave their study environment to solve problems.
- **Target Persona**: High school and college students (e.g., engineering, science, business) juggling social media temptations while studying for exams.
- **Core Value Proposition**: Students stay in their academic flow state without task-switching between disconnected apps.

---

## 2. User Experience & Visual Design

### Aesthetic Direction
- **Vibe**: Modern Editorial & Nordic Academic — intentional, clean, distraction-free, and calming.
- **Color Palette**:
  - `Background`: Deep Midnight Navy (`#0C1017`) for Dark Theme / Soft Warm Slate (`#F8F9FC`) for Light Theme.
  - `Surface`: Elevated Card Slate (`#161D2A`) with subtle border strokes (`#253045`).
  - `Primary / Accent`: Focused Indigo-Violet (`#6366F1`) & Electric Sage (`#10B981`) for positive study streaks.
  - `Alert / Quota`: Warm Amber (`#F59E0B`) and Rose Coral (`#F43F5E`) for reaching daily limits.
- **Typography & Hierarchy**:
  - Clean, high-legibility sans-serif with bold headline numbers for usage counters, crisp monospaced display for the scientific calculator, and comfortable line-height for study notes.
- **Layout & Structure**:
  - Material 3 Scaffold with top status & streak bar, bottom navigation bar (Dashboard, Notes, Calculator, Limits/Analytics), and floating quick-actions.
  - Fluid padding with `WindowInsets.safeDrawing` and `WindowInsets.navigationBars` support.

### Key User Flows
1. **Focus Dashboard Flow**:
   - Displays today's total study time, streak counter, installed app quotas, and circular progress rings.
   - Quick launch buttons for "Start 25m Focus Session", "Open Calculator", and "New Study Note".
   - Distraction prediction banner warning: *"High distraction period typically starts around 7:00 PM. Schedule your DSA revision now!"*
2. **App Limit & Shield Flow**:
   - Displays installed apps fetched via PackageManager, with current day's usage gathered from `UsageStatsManager`.
   - Sliders/pickers to set daily time quotas (e.g., 30m Instagram, 45m YouTube).
   - "Focus Shield" lock screen preview showing remaining minutes and motivational study prompt when limits are reached.
3. **Smart Study Notes & AI Assistant Flow**:
   - Filterable notes list by subject (e.g., "Operating Systems", "Data Structures", "Calculus").
   - Rich note editor supporting markdown-style headers, bullet lists, and code/formula snippets.
   - AI Study Toolbar: Tap **"Summarize with AI"** or **"Generate Revision Points"** to get structured key takeaways and study flashcards powered by Gemini.
4. **Scientific Calculator Flow**:
   - Two-row display (formula input and live evaluated result).
   - Ergonomic keypad with toggle for Degree/Radian, `sin`, `cos`, `tan`, `log`, `ln`, `^`, `√`, `π`, `e`, parentheses, and memory recall.
   - Quick "Insert into Note" button to copy formula or result directly into active study notes.

---

## 3. Key Product Decisions & Trade-Offs

- **Android App Blocking & Usage Tracking**:
  - *Chosen Approach*: Use Android `UsageStatsManager` with clear permission onboarding (`PACKAGE_USAGE_STATS`) to read accurate real-time foreground time. When limits are exceeded, display the StudyFocus Shield screen or reminder overlay prompting the student to return to study mode.
  - *Why*: Conforms strictly to Android security policies without requiring root or fragile accessibility services, while providing genuine device telemetry.
- **Local Persistence via Room**:
  - *Chosen Approach*: Room Database for study notes, subjects, app limit settings, and daily study logs.
  - *Why*: Instant offline access, zero cloud latency, high privacy (students' private notes stay on-device).
- **AI Study Assistant**:
  - *Chosen Approach*: Firebase AI Logic / Gemini integration for summarization and study advice, with an immediate rule-based fallback if offline or no network connection.
  - *Why*: Ensures the application is 100% usable without network or API keys, while giving state-of-the-art LLM summarization whenever online.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├─────────────────┬────────────────────┬─────────────────┬───────────────┤
│ Focus Dashboard │   App Time Limit   │   Study Notes   │  Scientific   │
│ & Streak Tracker│    & App Shield    │  & AI Assistant │  Calculator   │
└────────┬────────┴─────────┬──────────┴────────┬────────┴───────┬───────┘
         │                  │                   │                │
┌────────┴──────────────────┴───────────────────┴────────────────┴───────┐
│                    ViewModel Layer & StateFlow                         │
│  - DashboardViewModel     - AppLimitsViewModel                         │
│  - NotesViewModel         - CalculatorViewModel                        │
├────────────────────────────────────────────────────────────────────────┤
│                       Repository & Service Layer                       │
│  - AppUsageRepository (UsageStatsManager & Quota Engine)               │
│  - NotesRepository (Room DAO queries & search)                         │
│  - AiStudyAssistantService (Firebase AI Logic / Gemini 3.8 Flash)      │
│  - CalculatorEngine (Expression Parser & Evaluator)                    │
├────────────────────────────────────────────────────────────────────────┤
│                         Data Persistence Layer                         │
│  - Room Database (StudyNoteEntity, SubjectEntity, AppLimitEntity)      │
│  - Encrypted SharedPreferences / DataStore for Settings                │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Entities
1. `StudyNote`: `id`, `title`, `content`, `subject`, `tags`, `summary`, `revisionPoints`, `updatedAt`, `isPinned`.
2. `AppLimit`: `packageName`, `appName`, `dailyLimitMinutes`, `isBlocked`, `category`, `customAlertThreshold`.
3. `StudySession`: `id`, `subject`, `durationMinutes`, `timestamp`, `notesCount`, `calculatorUses`.
4. `DailyUsageSummary`: `date`, `totalScreenTimeMinutes`, `studyTimeMinutes`, `limitsRespectedCount`.
