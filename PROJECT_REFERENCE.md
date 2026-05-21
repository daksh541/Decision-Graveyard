# DecisionGraveyard - Full Project Reference

## 1) Project Summary

DecisionGraveyard is a Java Android app that helps users make better long-term decisions through delayed review, reflection, behavior tracking, and pattern-aware coaching.

The product combines:
- Decision journaling with delayed judgment
- Activity planning and execution tracking
- Pattern detection (miss streaks, risky decision windows)
- AI-generated insights and actionable recommendations
- Gamified profile progress (XP/levels/streaks)
- Notification systems for reminders and behavior nudges

Architecture style is **MVVM + Repository + Firebase-backed realtime data** with Android Fragments/Activities on top.

---

## 2) Core Features

- **Auth & Identity**
  - Email/password login + registration
  - Google Sign-In support
  - Per-user data isolation via Firebase Auth UID

- **Decision Lifecycle**
  - Create decision with category and future review time
  - Duplicate/similarity checks
  - Risk warnings based on historical regret patterns
  - Evaluate outcome (`good` / `bad` / `neutral`) with notes
  - Track pending/ready/reviewed states

- **Activities & Discipline**
  - Schedule activities by category/time
  - Mark complete or detect missed
  - Pre-miss reminders before deadline
  - Detect repeated misses and generate recovery missions

- **Dashboard / Insights / Timeline**
  - Aggregated metrics and trend views
  - Category regret insights + time-of-day analysis
  - Cause-effect style insights linking activity misses and decision quality
  - Timeline visualization of behavior history

- **AI Layer**
  - Gemini-powered textual insights via `LLMService`
  - Fallback deterministic insights when LLM is unavailable

- **Behavior & Notifications**
  - Decision review reminders
  - Activity pre-miss reminders
  - Daily/weekly smart notifications and behavioral checks via WorkManager

---

## 3) Tech Stack

- **Platform**: Android (minSdk 24, targetSdk 36, compileSdk 36)
- **Language**: Java 11
- **Architecture**: MVVM + Repository pattern + LiveData
- **Backend**: Firebase
  - Firebase Authentication
  - Cloud Firestore
  - Firestore Security Rules
- **AI**: Google Generative AI SDK (`gemini-1.5-flash`) via `LLMService`
- **Background Jobs**: WorkManager
- **Notifications**: AlarmManager + BroadcastReceivers + Notification channels
- **UI**: AndroidX + Material Components + RecyclerView
- **Build**: Gradle Kotlin DSL + Google Services plugin
- **Testing**: JUnit + AndroidX test dependencies

---

## 4) High-Level Component Interaction

1. UI (Fragment/Activity) captures user action.
2. ViewModel validates/transforms state.
3. Repository executes Firestore/logic operations.
4. Repository emits updates via LiveData/callback/snapshot listener.
5. ViewModel maps repository results to UI state.
6. UI re-renders.

Cross-cutting flows:
- **AuthHelper** gates authenticated access.
- **RepositoryProvider** wires repository dependencies.
- **Schedulers/Workers/Receivers** trigger reminders and behavior checks.
- **LLMService** enriches analytics with generated language insights.

---

## 5) Folder Structure

```text
DecisionGraveyard/
├── app/
│   ├── src/main/java/com/sai/decisiongraveyard/
│   │   ├── MainActivity.java
│   │   ├── DecisionDetailActivity.java
│   │   ├── adapter/                  # RecyclerView adapters
│   │   ├── firebase/                 # AuthHelper
│   │   ├── model/                    # Domain models
│   │   ├── notifications/            # Schedulers, workers, receivers
│   │   ├── repository/               # Data/business repositories
│   │   ├── service/                  # Legacy/manual LLM connector
│   │   ├── ui/                       # Feature fragments/activities/widgets
│   │   ├── util/                     # Utilities
│   │   └── viewmodel/                # MVVM view models
│   ├── src/main/res/                 # Layouts, drawables, menus, values
│   └── src/test/                     # Unit tests
├── firestore.rules
├── TEST_CASES.md
├── DOCUMENTATION.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/ (libs.versions.toml, wrapper config)
```

---

## 6) Key Runtime Modules

- **Bootstrap**
  - `MainActivity`: root navigation, permissions, scheduling startup
  - `RepositoryProvider`: singleton dependency wiring

- **Repositories**
  - `DecisionRepository`
  - `ActivityRepository`
  - `RecoveryMissionRepository`
  - `UserProfileRepository`
  - `UserPreferencesRepository`
  - `DailyCheckInRepository`
  - `CauseEffectRepository`
  - `TimelineRepository`
  - `AnalyticsRepository`

- **Main UI Areas**
  - Dashboard: `DashboardContainerFragment`, `DashboardFragment`
  - Decisions: `HomeFragment`, `AddDecisionFragment`, `DecisionDetailActivity`
  - Activities: `ActivityListFragment`, `AddActivityFragment`
  - Insights: `InsightsFragment`, `AIAnalysisFragment`
  - Timeline: `TimelineFragment`
  - Profile/Settings: `ProfileFragment`, `SettingsFragment`

---

## 7) API Endpoints and External Interfaces

This app is primarily Firebase SDK-driven, so there is no internal REST backend. "Endpoints" are external services + Firestore collections.

### A) External HTTP Endpoint (legacy/manual service)

- `https://api.openai.com/v1/chat/completions`
  - Defined in: `service/LLMAnalysisService.java`
  - Status: appears legacy/demo (contains placeholder API key)
  - Not the main production insight path

### B) Primary AI Interface (SDK-based)

- Google Generative AI model:
  - Model name: `gemini-1.5-flash`
  - Config key source: `BuildConfig.GEMINI_API_KEY` (from `gradle.properties`)
  - Used via: `repository/LLMService.java`

### C) Firebase Authentication Interfaces

- Email/password login/register via FirebaseAuth
- Google Sign-In -> Firebase credential sign-in
- Auth orchestration in `firebase/AuthHelper.java`

### D) Firestore Data Interfaces (Collections)

- `users`
- `userProfiles`
- `decisions`
- `activities`
- `recovery_missions`
- `causeEffectInsights`
- `dailyCheckIns`
- `analytics`
- `timeline`
- `checkins`
- `userPreferences`
- `userProgress`

All reads/writes are user-scoped by `userId` and enforced by `firestore.rules`.

---

## 8) Data Schema (Firestore-Oriented)

Below is the practical schema as inferred from model + repository usage.

### `decisions` (doc ID = decisionId string)
- `id: long`
- `title: string`
- `description: string`
- `category: string`
- `decisionTime: long` (epoch ms)
- `evaluationTime: long` (epoch ms)
- `decisionHour: int`
- `userId: string`
- `outcome: string|null` (`good|bad|neutral`)
- `reflectionNotes: string`
- `evaluatedAt: long|null`

### `activities` (doc ID = activityId string)
- `activityId: long`
- `title: string`
- `category: string`
- `scheduledTime: long`
- `completed: boolean`
- `completedTime: long|null`
- `userId: string`
- `createdAt: long`

### `recovery_missions`
- `missionId: string`
- `title: string`
- `description: string`
- `activitiesToComplete: int`
- `activitiesCompleted: int`
- `deadline: long`
- `isCompleted: boolean`
- `userId: string`

### `userProfiles`
- `userId: string`
- progression/streak/XP counters (as updated by `UserProfileRepository`)

### `userPreferences`
- `userId: string`
- notification mode / goals / personalization settings

### `dailyCheckIns`
- `checkInId: string`
- `userId: string`
- daily reflection/check-in fields

### `causeEffectInsights`, `analytics`, `timeline`, `checkins`, `userProgress`
- Additional derived/behavioral/user-state collections used by specialized repositories/features.

---

## 9) Security Model (Firestore Rules)

`firestore.rules` enforces:
- Only authenticated users can read/write protected data.
- Users can only access documents where `userId == request.auth.uid`.
- User-scoped collections (`userPreferences`, `userProgress`) are tied to UID path ownership.

This prevents cross-user data access at the datastore layer.

---

## 10) Setup Instructions

## Prerequisites
- Android Studio (latest stable)
- JDK 11
- Firebase project configured for Android app
- Google Services config file in app module (`google-services.json`)

## Local Setup
1. Open project in Android Studio.
2. Ensure `gradle.properties` contains:
   - `gemini.api.key=<YOUR_GEMINI_API_KEY>`
3. Ensure Firebase is configured:
   - Auth providers enabled (email/password, Google if required)
   - Firestore database created
   - Firestore rules applied from `firestore.rules`
4. Sync Gradle.
5. Build and run app on emulator/device.

## Build/Test Commands

From project root:

```bash
./gradlew assembleDebug
./gradlew test
```

---

## 11) Main User Flows

- **Auth flow**: Login/Register -> user document upsert -> main app
- **Decision flow**: Add decision -> schedule reminder -> review later -> evaluate -> analytics refresh
- **Activity flow**: Add activity -> pre-miss reminder -> complete/miss -> pattern detection
- **Recovery flow**: repeated misses -> mission creation -> completion tracking
- **Insights flow**: aggregate decisions/activities -> generate deterministic + LLM insights

---

## 12) Important Notes and Current Reality

- Project still contains legacy/demo docs and services; current runtime architecture is richer than early docs.
- `LLMAnalysisService` is legacy/manual; `repository/LLMService` is the actively integrated path.
- Firestore indexes are intentionally minimized in some queries by fetching user-scoped docs then filtering in memory.

---

## 13) Recommended Next Improvements

- Add/refresh top-level `README.md` for contributors.
- Add explicit schema/versioning docs for Firestore collections.
- Add instrumentation tests around notification and receiver flows.
- Add CI for `assembleDebug + test` on pull requests.
- Move any remaining placeholder API-key flows out of code.
