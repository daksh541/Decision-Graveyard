# DecisionGraveyard - Project Design Document (PDR)

## Document Information
- **Project Name**: DecisionGraveyard
- **Version**: 1.0
- **Date**: May 21, 2026
- **Author**: Development Team
- **Document Type**: Project Design Document

---

## 1. Executive Summary

### 1.1 Project Overview
DecisionGraveyard is an Android application designed to help users improve their decision-making through delayed evaluation, pattern recognition, and behavioral coaching. The app allows users to log decisions, schedule future reviews, track activities, and receive AI-powered insights about their decision patterns.

### 1.2 Problem Statement
Users often make decisions without understanding their long-term consequences. DecisionGraveyard addresses this by:
- Implementing delayed judgment to reduce emotional bias
- Tracking decision outcomes over time
- Identifying patterns in decision quality
- Providing behavioral coaching through activity tracking
- Offering AI-generated insights for self-improvement

### 1.3 Solution Approach
The solution combines:
- Decision journaling with scheduled review dates
- Activity planning and execution tracking
- Pattern detection for missed activities and risky decision windows
- AI-powered insights using Google Gemini API
- Gamified progress system (XP, levels, streaks)
- Real-time data synchronization via Firebase Firestore

---

## 2. System Architecture

### 2.1 Architecture Pattern
The application follows **MVVM (Model-View-ViewModel)** architecture with Repository pattern for data access.

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer                              │
│  (Activities, Fragments, Adapters, RecyclerViews)        │
└────────────────────┬────────────────────────────────────┘
                     │ LiveData/Callbacks
┌────────────────────▼────────────────────────────────────┐
│                 ViewModel Layer                          │
│  (Business Logic, State Management, Validation)          │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│               Repository Layer                           │
│  (Data Access, Business Rules, External APIs)            │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                 Data Layer                               │
│  (Firebase Firestore, Firebase Auth, AI Services)       │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Architecture Principles
- **Unidirectional Data Flow**: User input → ViewModel → Repository → Data → Repository → ViewModel → UI
- **Reactive Updates**: LiveData and Firestore snapshot listeners for real-time synchronization
- **Separation of Concerns**: Each layer has a single, well-defined responsibility
- **Repository Pattern**: Centralized data access abstraction
- **Dependency Injection**: Manual dependency wiring via RepositoryProvider

### 2.3 Component Interaction Flow
1. User interacts with UI (Activity/Fragment)
2. ViewModel validates input and transforms state
3. Repository executes business logic and data operations
4. Repository emits updates via LiveData/callback/snapshot listener
5. ViewModel maps repository results to UI state
6. UI re-renders based on state changes

---

## 3. Technology Stack

### 3.1 Platform Configuration
- **Platform**: Android
- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36
- **Language**: Java 11
- **Build System**: Gradle with Kotlin DSL

### 3.2 Core Dependencies

#### AndroidX Libraries
- `androidx.core:core-ktx:1.12.0` - Core Android components
- `androidx.appcompat:appcompat:1.6.1` - AppCompat support
- `com.google.android.material:material:1.12.0` - Material Design components
- `androidx.lifecycle:lifecycle-livedata:2.7.0` - LiveData for reactive programming
- `androidx.lifecycle:lifecycle-viewmodel:2.7.0` - ViewModel for UI state management
- `androidx.fragment:fragment:1.6.2` - Fragment support
- `androidx.constraintlayout:constraintlayout:2.1.4` - ConstraintLayout
- `androidx.coordinatorlayout:coordinatorlayout:1.2.0` - CoordinatorLayout
- `androidx.recyclerview:recyclerview:1.3.1` - RecyclerView for lists
- `androidx.cardview:cardview:1.0.0` - CardView
- `androidx.work:work-runtime:2.9.0` - WorkManager for background tasks

#### Firebase Services
- `com.google.firebase:firebase-firestore` - Cloud Firestore database (BOM 34.12.0)
- `com.google.firebase:firebase-auth` - Firebase Authentication (BOM 34.12.0)
- `com.firebaseui:firebase-ui-firestore:8.0.2` - Firebase UI for Firestore

#### Google Services
- `com.google.android.gms:play-services-auth:21.1.0` - Google Sign-In authentication
- `com.google.ai.client.generativeai:generativeai:0.9.0` - Google Gemini API for AI insights

#### Utilities
- `com.google.guava:guava:33.0.0-android` - Guava libraries

#### Testing
- `junit:junit:4.13.2` - Unit testing
- `androidx.test.ext:junit:1.3.0` - Android JUnit extensions
- `androidx.test.espresso:espresso-core:3.7.0` - UI testing

### 3.3 Build Configuration
- **Gradle Plugin**: Android Gradle Plugin 9.1.1
- **Kotlin Version**: 2.2.10
- **Google Services Plugin**: 4.4.4

---

## 4. Data Models

### 4.1 Core Domain Models

#### Decision
Represents a logged decision awaiting or completed evaluation.

**Properties:**
- `id` (long): Unique identifier
- `title` (String): Decision title
- `description` (String): Detailed description
- `category` (String): Category (money, health, study, personal, work, relationship)
- `decisionTime` (long): Timestamp when decision was made
- `evaluationTime` (long): Timestamp when evaluation is scheduled
- `decisionHour` (int): Hour of day (0-23)
- `userId` (String): Firebase user ID
- `outcome` (String): Evaluation result (good/bad/neutral)
- `reflectionNotes` (String): User's reflection notes
- `evaluatedAt` (Long): Timestamp of evaluation

**Key Methods:**
- Multiple constructors for different use cases
- Standard getters/setters for all properties
- `toString()`: String representation for debugging

#### Evaluation
Stores evaluation results for decisions.

**Properties:**
- `id` (long): Unique identifier
- `decisionId` (long): Reference to decision
- `outcome` (String): good/bad/neutral
- `reflectionNotes` (String): User's thoughts
- `evaluatedAt` (long): Timestamp of evaluation

#### DecisionRecord
Combines Decision and Evaluation into a single object for UI consumption.

**Properties:**
- `decision` (Decision): The decision object
- `evaluation` (Evaluation): The evaluation object (nullable if not evaluated)

**Key Methods:**
- `isEvaluated()`: Returns true if evaluation exists
- `isReadyForReview()`: Returns true if current time >= evaluation time
- `isUpcoming()`: Returns true if not evaluated and not ready
- `getOutcomeOrPending()`: Returns outcome or "pending"

#### Activity
Activity tracking entity for scheduled tasks and commitments.

**Properties:**
- `activityId` (long): Unique identifier
- `title` (String): Activity title
- `category` (String): Category classification
- `scheduledTime` (long): When activity is scheduled (timestamp)
- `completed` (boolean): Completion status
- `completedTime` (Long): Timestamp when completed (nullable)
- `userId` (String): Firebase user ID
- `createdAt` (long): Creation timestamp

**Key Methods:**
- `getStatus()`: Returns status based on state
  - "completed": If marked as completed
  - "missed": If past scheduled time and not completed
  - "pending": If future time and not completed

#### UserProfile
Gamified user profile with progression system.

**Properties:**
- `userId` (String): Firebase user ID
- `levelName` (String): Current level name (Chaotic, Aware, Controlled, Disciplined, Optimized)
- `currentLevel` (int): Current level (1-5)
- `currentXP` (int): Current experience points
- `xpToNextLevel` (int): XP required for next level
- `dailyDisciplineStreak` (int): Consecutive days with completed activities
- `missedDayStreak` (int): Consecutive days with missed activities
- `totalGoodDecisions` (int): Total good decisions count
- `totalBadDecisions` (int): Total bad decisions count
- `totalCompletedActivities` (int): Total completed activities
- `totalMissedActivities` (int): Total missed activities
- `activityCompletionRate` (double): Percentage of completed activities
- `decisionQualityRate` (double): Percentage of good decisions
- `createdAt` (Timestamp): Account creation timestamp
- `updatedAt` (Timestamp): Last update timestamp

**Key Methods:**
- `addXP(int)`: Add XP and check for level up
- `removeXP(int)`: Remove XP (minimum 0)
- `checkLevelUp()`: Automatic level progression
- `calculateXPForLevel(int)`: XP requirements per level
- `getLevelName()`: Returns level name based on current level
- `getProgressPercentage()`: Returns XP progress percentage
- `getDecisionQualityRate()`: Calculates decision quality percentage
- `getActivityCompletionRate()`: Calculates activity completion percentage

**Level Progression:**
- Level 1 (Chaotic): 0-99 XP
- Level 2 (Aware): 100-249 XP
- Level 3 (Controlled): 250-499 XP
- Level 4 (Disciplined): 500-999 XP
- Level 5 (Optimized): 1000+ XP

#### PatternTracker
Analyzes activity completion patterns to detect failure trends.

**Properties:**
- `consecutiveMisses` (List<String>): List of consecutively missed activity titles
- `totalMisses` (int): Total missed activities in analysis period
- `mostMissedCategory` (String): Category with most misses
- `isPatternDetected` (boolean): Whether a concerning pattern exists
- `patternMessage` (String): Human-readable pattern description
- `recoveryActivitiesNeeded` (int): Number of activities to complete for recovery

**Key Methods:**
- Builder pattern for flexible construction
- Pattern detection logic (3+ consecutive misses triggers pattern)
- Recovery activity calculation

#### RecoveryMission
Represents a recovery mission to break negative patterns.

**Properties:**
- `missionId` (String): Unique mission identifier
- `title` (String): Mission title
- `description` (String): Mission description
- `activitiesToComplete` (int): Target activity count
- `activitiesCompleted` (int): Completed activity count
- `deadline` (long): Mission deadline timestamp
- `isCompleted` (boolean): Mission completion status
- `userId` (String): Firebase user ID

**Key Methods:**
- `getProgress()`: Returns completion percentage (0.0-1.0)
- `isExpired()`: Returns true if deadline passed and not completed
- Builder pattern for construction

#### AnalyticsSnapshot
Comprehensive analytics snapshot for insights generation.

**Properties:**
- `totalDecisions` (int): Total number of decisions
- `totalEvaluated` (int): Number of evaluated decisions
- `goodCount` (int): Number of good decisions
- `badCount` (int): Number of bad decisions
- `neutralCount` (int): Number of neutral decisions
- `readyForReviewCount` (int): Decisions ready for review
- `upcomingCount` (int): Decisions not yet ready
- `categoryInsights` (List<CategoryInsight>): Category-based analytics
- `generatedInsights` (List<String>): Textual insights
- `intensityLevel` (int): Bad decision rate percentage
- `actionableInsights` (List<String>): Actionable recommendations
- `patternRepetitions` (List<PatternRepetition>): Detected patterns
- `consequencePoints` (int): Calculated consequence score
- `consequenceMessage` (String): Consequence description
- `totalActivities` (int): Total activities count
- `completedActivities` (int): Completed activities count
- `missedActivities` (int): Missed activities count
- `activityInsights` (List<String>): Activity-based insights

**Key Methods:**
- `isEmpty()`: Returns true if no evaluations exist
- `getGoodPercent()`: Percentage of good decisions
- `getBadPercent()`: Percentage of bad decisions
- `getNeutralPercent()`: Percentage of neutral decisions
- `getQualityScore()`: Overall decision quality score (0-100)

#### CategoryInsight
Analytics data for decision categories.

**Properties:**
- `category` (String): Category name
- `evaluatedCount` (int): Total evaluated decisions in category
- `regretCount` (int): Number of bad decisions
- `regretRate` (int): Percentage of regrets (0-100)

#### CauseEffectInsight
Links missed activities to decision quality.

**Properties:**
- `insightId` (String): Unique identifier
- `userId` (String): Firebase user ID
- `causeType` (String): Type of cause (missed activity, pattern, etc.)
- `effectType` (String): Type of effect (bad decision, quality drop)
- `description` (String): Insight description
- `severity` (String): Severity level (low, medium, high)
- `createdAt` (Timestamp): Creation timestamp

#### TimelineEvent
Represents an event in the timeline view.

**Properties:**
- `eventId` (String): Unique identifier
- `eventType` (String): Type (decision, activity)
- `title` (String): Event title
- `timestamp` (long): Event timestamp
- `status` (String): Event status
- `category` (String): Event category
- `userId` (String): Firebase user ID

#### DailyCheckIn
Daily reflection check-in for users.

**Properties:**
- `checkInId` (String): Unique identifier
- `userId` (String): Firebase user ID
- `date` (String): Date in YYYY-MM-DD format
- `mood` (String): User's mood
- `energyLevel` (int): Energy level (1-10)
- `notes` (String): Daily notes
- `completed` (boolean): Check-in completion status
- `createdAt` (Timestamp): Creation timestamp

#### UserPreferences
User customization preferences.

**Properties:**
- `userId` (String): Firebase user ID
- `notificationMode` (String): Normal or Brutal
- `goals` (List<String>): User's selected goals
- `theme` (String): Light or Dark
- `reminderTime` (long): Preferred reminder time

### 4.2 Firestore Schema

#### Collections

**decisions**
```
Document ID: decisionId (string)
Fields:
  - id: long
  - title: string
  - description: string
  - category: string
  - decisionTime: long (epoch ms)
  - evaluationTime: long (epoch ms)
  - decisionHour: int
  - userId: string
  - outcome: string|null (good|bad|neutral)
  - reflectionNotes: string
  - evaluatedAt: long|null
```

**activities**
```
Document ID: activityId (string)
Fields:
  - activityId: long
  - title: string
  - category: string
  - scheduledTime: long
  - completed: boolean
  - completedTime: long|null
  - userId: string
  - createdAt: long
```

**recovery_missions**
```
Document ID: missionId (string)
Fields:
  - missionId: string
  - title: string
  - description: string
  - activitiesToComplete: int
  - activitiesCompleted: int
  - deadline: long
  - isCompleted: boolean
  - userId: string
```

**userProfiles**
```
Document ID: userId (string)
Fields:
  - userId: string
  - levelName: string
  - currentLevel: int
  - currentXP: int
  - xpToNextLevel: int
  - dailyDisciplineStreak: int
  - missedDayStreak: int
  - totalGoodDecisions: int
  - totalBadDecisions: int
  - totalCompletedActivities: int
  - totalMissedActivities: int
  - activityCompletionRate: double
  - decisionQualityRate: double
  - createdAt: Timestamp
  - updatedAt: Timestamp
```

**userPreferences**
```
Document ID: userId (string)
Fields:
  - userId: string
  - notificationMode: string
  - goals: array<string>
  - theme: string
  - reminderTime: long
```

**users**
```
Document ID: userId (string)
Fields:
  - userId: string
  - email: string
  - createdAt: Timestamp
```

**dailyCheckIns**
```
Document ID: checkInId (string)
Fields:
  - checkInId: string
  - userId: string
  - date: string (YYYY-MM-DD)
  - mood: string
  - energyLevel: int
  - notes: string
  - completed: boolean
  - createdAt: Timestamp
```

**causeEffectInsights**
```
Document ID: insightId (string)
Fields:
  - insightId: string
  - userId: string
  - causeType: string
  - effectType: string
  - description: string
  - severity: string
  - createdAt: Timestamp
```

**analytics**
```
Document ID: analyticsId (string)
Fields:
  - userId: string
  - snapshotData: map
  - generatedAt: Timestamp
```

**timeline**
```
Document ID: eventId (string)
Fields:
  - eventId: string
  - userId: string
  - eventType: string
  - title: string
  - timestamp: long
  - status: string
  - category: string
```

---

## 5. Repository Layer

### 5.1 DecisionRepository
Central data access layer for decision management using Firebase Firestore.

**Responsibilities:**
- Decision CRUD operations
- Evaluation management
- Real-time data synchronization
- Analytics calculation
- Pattern detection
- AI insight generation

**Key Methods:**

**Decision Management:**
- `createDecision(long, String, String, String, long, ActionCallback)`: Async decision creation
- `createDecision(String, String, String, long)`: Blocking decision creation
- `hasSimilarDecision(String, String, long)`: Duplicate detection within 24 hours
- `checkDecisionWarning(String, int)`: Risk assessment based on historical patterns
- `deleteDecision(long)`: Remove decision from Firestore
- `getAllDecisionRecords()`: Fetch all user's decisions
- `getDecisionRecord(long)`: Fetch single decision by ID

**Evaluation Management:**
- `saveEvaluation(long, String, String)`: Blocking evaluation save
- `saveEvaluation(long, String, String, ActionCallback)`: Async evaluation save with XP rewards

**Real-time Updates:**
- `listenToDecisions(DecisionRecordsListener)`: Set up Firestore snapshot listener
- `getDataChangedTrigger()`: LiveData for manual refresh triggers

**Analytics:**
- `getAnalyticsSnapshot()`: Calculate comprehensive analytics
- `getAnalyticsSnapshot(boolean brutalMode)`: Analytics with brutal mode support

**Analytics Calculation Logic:**
```java
// Category regret rate calculation
regretRate = (regretCount / evaluatedCount) * 100

// Time bucket analysis
- Late night: 23:00 - 05:00
- Morning: 05:00 - 12:00
- Afternoon: 12:00 - 17:00
- Evening: 17:00 - 23:00

// Consequence points calculation
consequencePoints = (goodCount * 5) - (badCount * 10)

// Intensity level (bad decision rate)
intensityLevel = (badCount / totalEvaluated) * 100
```

### 5.2 ActivityRepository
Central data access layer for activity tracking.

**Responsibilities:**
- Activity CRUD operations
- Activity status management
- Pre-miss reminder scheduling
- Pattern detection
- Activity filtering

**Key Methods:**
- `createActivity(String, String, long, ActionCallback)`: Async activity creation
- `markAsCompleted(long, ActionCallback)`: Mark activity as completed
- `deleteActivity(long, ActionCallback)`: Remove activity
- `getAllActivities()`: Fetch all user's activities
- `getActivitiesForDay(long, long)`: Fetch activities for specific day
- `getMissedActivities()`: Fetch missed activities
- `detectFailurePattern()`: Analyze activity completion patterns
- `listenToActivities(ActivityListListener)`: Set up Firestore snapshot listener

### 5.3 RecoveryMissionRepository
Manages recovery missions for breaking negative patterns.

**Key Methods:**
- `createRecoveryMission(RecoveryMission, ActionCallback)`: Create new recovery mission
- `updateMissionProgress(String, int, ActionCallback)`: Update mission completion count
- `getActiveMissions()`: Fetch user's active recovery missions
- `getMissionById(String)`: Fetch single mission by ID

### 5.4 UserProfileRepository
Manages user profile and progression system.

**Key Methods:**
- `getUserProfile()`: Fetch user profile
- `updateUserProfile(UserProfile, ActionCallback)`: Update profile
- `addGoodDecision(ActionCallback)`: Increment good decision count and award XP
- `addBadDecision(ActionCallback)`: Increment bad decision count and apply XP penalty
- `addCompletedActivity(ActionCallback)`: Increment completed activity count and award XP
- `addMissedActivity(ActionCallback)`: Increment missed activity count
- `updateStreaks(ActionCallback)`: Update discipline and miss streaks

### 5.5 UserPreferencesRepository
Manages user preferences and settings.

**Key Methods:**
- `getUserPreferences()`: Fetch user preferences
- `savePreferences(UserPreferences, ActionCallback)`: Save preferences
- `updateNotificationMode(String, ActionCallback)`: Update notification mode
- `updateGoals(List<String>, ActionCallback)`: Update user goals

### 5.6 LLMService
AI-powered insights generation using Google Gemini API.

**Key Methods:**
- `generateInsights(String, boolean)`: Generate decision pattern insights
  - Takes decision data as string
  - Supports brutal mode (harsh, direct) or normal mode (analytical)
  - Returns CompletableFuture with generated insights
  - Falls back to null on error

- `generateActionableInsights(String)`: Generate actionable recommendations
  - Provides specific, practical recommendations
  - Max 15 words per recommendation
  - Returns as simple list

- `generateActivityInsights(String)`: Generate activity pattern insights
  - Analyzes missed activities and decision quality connection
  - Focuses on cause-effect relationships
  - Max 20 words per insight

**Configuration:**
- Model: gemini-1.5-flash (fast, cost-effective)
- API Key: BuildConfig.GEMINI_API_KEY from gradle.properties
- Execution: Main thread via ContextCompat.getMainExecutor()

### 5.7 CauseEffectRepository
Manages cause-effect insights linking activities to decision quality.

**Key Methods:**
- `generateCauseEffectInsights(List<DecisionRecord>, Map<String, Integer>, int)`: Generate insights
- `saveCauseEffectInsight(CauseEffectInsight, ActionCallback)`: Save insight
- `getRecentCauseEffectInsights()`: Fetch recent insights

### 5.8 TimelineRepository
Manages timeline events for chronological view.

**Key Methods:**
- `generateTimelineEvents()`: Generate timeline from decisions and activities
- `getTimelineEvents()`: Fetch timeline events
- `saveTimelineEvent(TimelineEvent, ActionCallback)`: Save event

### 5.9 DailyCheckInRepository
Manages daily check-in functionality.

**Key Methods:**
- `getTodayCheckIn()`: Fetch today's check-in
- `saveCheckIn(DailyCheckIn, ActionCallback)`: Save check-in
- `hasCheckedInToday()`: Check if user completed today's check-in

### 5.10 AnalyticsRepository
Manages analytics snapshots and historical data.

**Key Methods:**
- `saveAnalyticsSnapshot(AnalyticsSnapshot, ActionCallback)`: Save snapshot
- `getHistoricalAnalytics(int days)`: Fetch historical analytics
- `getTrends()`: Calculate trends over time

---

## 6. ViewModel Layer

### 6.1 AddDecisionViewModel
Handles decision creation workflow with validation and state management.

**State Classes:**
```java
class SaveState {
    boolean success;
    String message;
    long decisionId;
    long evaluationTime;
    String title;
}
```

**Key Methods:**
- `saveDecision(String, String, String, long)`: Main save logic
  - Validates input (title required, category required, future evaluation time)
  - Generates unique decision ID
  - Checks for similar decisions asynchronously
  - Calls repository with callback
  - Updates LiveData with SaveState

- `setCustomDateMillis(Long)`, `clearCustomDate()`: Custom date picker support
- `clearSaveState()`: Reset save state

**Thread Management:** Uses ExecutorService for background operations

### 6.2 DecisionDetailViewModel
Handles decision detail view and evaluation operations.

**State Classes:**
```java
class ActionState {
    boolean saved;
    boolean deleted;
    String message;
}
```

**Key Methods:**
- `loadDecision(long)`: Fetch decision by ID from repository
- `saveEvaluation(long, String, String)`: Save evaluation outcome and notes
- `deleteDecision(long)`: Remove decision from repository
- `clearActionState()`: Reset action state

### 6.3 HomeViewModel
Manages home screen state with filtering and real-time updates.

**State Classes:**
```java
class HomeScreenState {
    List<DecisionRecord> filteredRecords;
    int totalCount;
    int readyCount;
    int upcomingCount;
}
```

**Key Methods:**
- `startListening()`: Set up Firestore real-time listener
- `setStatusFilter(String)`: Filter by status (all/ready/reviewed)
- `setCategoryFilter(String)`: Filter by category
- `refresh()`: Restart listener if needed
- `postFilteredState()`: Apply filters and update screen state

**Filtering Logic:**
```java
boolean matchesStatus(DecisionRecord record) {
    if ("ready".equals(statusFilter)) return record.isReadyForReview();
    if ("reviewed".equals(statusFilter)) return record.isEvaluated();
    return true;
}

boolean matchesCategory(DecisionRecord record) {
    return "All".equals(categoryFilter) || 
           categoryFilter.equalsIgnoreCase(record.getDecision().getCategory());
}
```

### 6.4 InsightsViewModel
Calculates analytics and insights for decision patterns.

**Key Methods:**
- `refresh()`: Calculate analytics snapshot and streak
- `calculateStreak()`: Calculate consecutive good decisions
  - Sorts records by evaluation time (oldest first)
  - Counts consecutive good decisions within 24 hours
  - Breaks on bad/neutral decisions or large gaps
  - Returns streak count

### 6.5 DashboardViewModel
Manages dashboard state and metrics.

**Key Methods:**
- `loadDashboardData()`: Load all dashboard metrics
- `refreshMetrics()`: Refresh dashboard statistics
- `getTodayStats()`: Get today's decision and activity counts

### 6.6 ActivityListViewModel
Manages activity list state and filtering.

**Key Methods:**
- `loadActivities()`: Load activities with real-time updates
- `setStatusFilter(String)`: Filter by status (pending/completed/missed)
- `markAsCompleted(long)`: Mark activity as completed
- `deleteActivity(long)`: Delete activity

### 6.7 ProfileViewModel
Manages user profile state and settings.

**Key Methods:**
- `loadProfile()`: Load user profile data
- `updateSettings(UserPreferences)`: Update user preferences
- `logout()`: Handle user logout

---

## 7. UI Layer

### 7.1 Activities

#### MainActivity
Main container with bottom navigation and theme management.

**Key Methods:**
- `onCreate(Bundle)`: Setup theme, toolbar, bottom nav, notification channel
- `setupBottomNavigation()`: Handle navigation between fragments
- `loadFragment(Fragment)`: Replace fragment in container
- `onToolbarItemSelected(MenuItem)`: Handle toolbar actions (theme toggle, logout)
- `onDecisionClicked(DecisionRecord)`: Navigate to decision detail
- `redirectToLogin()`: Redirect if not authenticated

**Navigation Structure:**
- Home: Decision list with filters
- Add: Decision creation form
- Insights: Analytics dashboard
- Profile: User profile and stats

#### DecisionDetailActivity
Detailed view and evaluation interface for individual decisions.

**Key Methods:**
- `bindViews()`: Initialize all UI components
- `setupOutcomeToggle()`: Handle good/neutral/bad selection
- `renderDecision(DecisionRecord)`: Display decision based on state
  - **Evaluated state**: Shows outcome, reflection, disabled controls
  - **Ready for review state**: Shows evaluation form, enabled controls
  - **Upcoming state**: Shows countdown, disabled controls
- `saveEvaluation()`: Submit evaluation to ViewModel
- `deleteDecision()`: Remove decision
- `getCategoryColor(String)`: Map category to color resource
- `getOutcomeLabel(String)`: Map outcome to label (GOOD/BAD/MEH)

**Visual Features:**
- Category chip with color coding
- Timeline with active/inactive dots
- Status badges (positive/negative/neutral backgrounds)
- Outcome toggle group with MaterialButtons

#### LoginActivity
User authentication interface.

**Key Methods:**
- `loginUser()`: Validate credentials and authenticate
- `loginWithGoogle()`: Handle Google Sign-In
- `setLoading(boolean)`: Toggle loading state
- `navigateToMain()`: Clear back stack and go to MainActivity

#### RegisterActivity
User registration interface.

**Key Methods:**
- `registerUser()`: Validate and create account
- `setLoading(boolean)`: Toggle loading state

### 7.2 Fragments

#### HomeFragment
Decision list with filtering and export functionality.

**Key Methods:**
- `setupCategoryDropdown()`: AutoCompleteTextView for categories
- `setupStatusFilter()`: ChipGroup for status filtering
- `exportToUri(Uri)`: Export decision history to CSV
- `observeScreenState()`: Update UI based on ViewModel state

**Features:**
- Status chips (All, Ready, Reviewed)
- Category dropdown filter
- Export to CSV functionality
- Empty state handling

#### AddDecisionFragment
Complex decision creation with risk assessment and visual feedback.

**Key Methods:**
- `setupCategoryDropdown()`: Category selection
- `setupScheduleToggle()`: Review date selection (3 days, 1 week, custom)
- `setupRiskMeter()`: Risk assessment based on category history
- `updateRiskMeter(String)`: Calculate and display regret rate
- `applyRiskVisuals(boolean, int)`: Visual feedback for high-risk categories
  - **High risk (>=60%)**: Red background tint, card pulse animation, vibration warning
  - **Low/Medium risk**: Normal background, no animation
- `showConsequenceMomentDialog(String)`: Confirmation dialog before save

#### ActivityListFragment
Activity list with filtering and completion tracking.

**Key Methods:**
- `setupStatusFilter()`: Filter by pending/completed/missed
- `observeActivityList()`: Update UI based on ViewModel state
- `onCompleteClicked(Activity)`: Mark activity as completed
- `onDeleteClicked(Activity)`: Delete activity

#### InsightsFragment
Analytics dashboard with insights display.

**Key Methods:**
- `loadInsights()`: Load analytics from ViewModel
- `setupBrutalModeToggle()`: Toggle brutal mode
- `displayInsights(AnalyticsSnapshot)`: Render insights
- `displayCategoryInsights(List<CategoryInsight>)`: Render category analytics

#### TimelineFragment
Chronological view of decisions and activities.

**Key Methods:**
- `loadTimeline()`: Load timeline events
- `renderTimeline(List<TimelineEvent>)`: Render timeline with day grouping
- `getEventColor(String)`: Map event type to color

#### ProfileFragment
User profile and settings display.

**Key Methods:**
- `loadProfile()`: Load user profile
- `displayStats(UserProfile)`: Display user statistics
- `setupSettings()`: Configure settings options

### 7.3 Adapters

#### DecisionAdapter
RecyclerView adapter for decision list.

**Key Methods:**
- `onBindViewHolder()`: Bind decision data to view holder
- `getItemCount()`: Return decision count
- `updateList(List<DecisionRecord>)`: Update decision list

#### ActivityAdapter
RecyclerView adapter for activity list.

**Key Methods:**
- `onBindViewHolder()`: Bind activity data to view holder
- `getItemCount()`: Return activity count
- `updateList(List<Activity>)`: Update activity list

#### InsightAdapter
RecyclerView adapter for insights list.

**Key Methods:**
- `onBindViewHolder()`: Bind insight data to view holder
- `getItemCount()`: Return insight count

#### TimelineDayAdapter
RecyclerView adapter for timeline day grouping.

**Key Methods:**
- `onBindViewHolder()`: Bind day data to view holder
- `getItemCount()`: Return day count

#### TimelineEventAdapter
RecyclerView adapter for timeline events within a day.

**Key Methods:**
- `onBindViewHolder()`: Bind event data to view holder
- `getItemCount()`: Return event count

#### DecisionSwipeCallback
Swipe gesture handler for quick evaluation.

**Key Methods:**
- `onSwiped()`: Handle swipe actions
  - Swipe right: Mark as good
  - Swipe left: Mark as bad

---

## 8. Notification System

### 8.1 Notification Components

#### NotificationScheduler
Schedules decision evaluation reminders using AlarmManager.

**Key Methods:**
- `scheduleDecisionReminder(long decisionId, long evaluationTime)`: Schedule reminder
- `cancelReminder(long decisionId)`: Cancel scheduled reminder

#### EvaluationReminderReceiver
Broadcast receiver for decision evaluation reminders.

**Key Methods:**
- `onReceive()`: Handle reminder broadcast and show notification

#### PreMissReminderScheduler
Schedules pre-miss reminders for activities (10 minutes before deadline).

**Key Methods:**
- `schedulePreMissReminder(long activityId, long scheduledTime)`: Schedule reminder
- `cancelPreMissReminder(long activityId)`: Cancel scheduled reminder

#### PreMissReminderReceiver
Broadcast receiver for pre-miss reminders.

**Key Methods:**
- `onReceive()`: Handle pre-miss broadcast and show notification

#### BootReceiver
Broadcast receiver for device boot to reschedule reminders.

**Key Methods:**
- `onReceive()`: Reschedule all pending reminders on device boot

#### ActivityScheduler
WorkManager scheduler for periodic activity checks.

**Key Methods:**
- `schedulePeriodicCheck()`: Schedule periodic missed activity detection

#### ActivityMissedWorker
WorkManager worker for handling missed activities.

**Key Methods:**
- `doWork()`: Check for missed activities and update status

#### BehavioralTriggerWorker
WorkManager worker for behavioral pattern analysis.

**Key Methods:**
- `doWork()`: Analyze patterns and trigger smart notifications

#### SmartNotificationScheduler
Scheduler for smart, context-aware notifications.

**Key Methods:**
- `scheduleSmartNotification()`: Schedule intelligent notification
- `calculateOptimalTime()`: Calculate optimal notification time based on patterns

#### SmartNotificationReceiver
Broadcast receiver for smart notifications.

**Key Methods:**
- `onReceive()`: Handle smart notification broadcast

### 8.2 Notification Channels

**Decision Review Channel**
- ID: "decision_review_channel"
- Importance: High
- Description: Notifications for decision review reminders

**Activity Reminder Channel**
- ID: "activity_reminder_channel"
- Importance: High
- Description: Notifications for activity reminders

**Pre-Miss Reminder Channel**
- ID: "pre_miss_reminder_channel"
- Importance: Default
- Description: Notifications before activity deadlines

**Smart Notification Channel**
- ID: "smart_notification_channel"
- Importance: Default
- Description: Context-aware behavioral notifications

### 8.3 Permissions

Required permissions in AndroidManifest.xml:
- `INTERNET`: For Firebase connectivity
- `POST_NOTIFICATIONS`: For posting notifications (Android 13+)
- `RECEIVE_BOOT_COMPLETED`: For rescheduling reminders on boot
- `VIBRATE`: For notification vibration
- `SCHEDULE_EXACT_ALARM`: For precise alarm scheduling (Android 12+)

---

## 9. Firebase Integration

### 9.1 Authentication

#### AuthHelper
Authentication wrapper supporting email/password and Google Sign-In.

**Key Methods:**
- `login(String, String, AuthCallback)`: Email/password login
- `register(String, String, AuthCallback)`: Email/password registration
- `signInWithGoogle(GoogleSignInAccount, AuthCallback)`: Google Sign-In
- `logout()`: Sign out user
- `resetPassword(String, AuthCallback)`: Password reset
- `getCurrentUser()`: Get current Firebase user
- `isUserLoggedIn()`: Check authentication status

**Error Handling:**
- Maps Firebase auth errors to user-friendly messages
- Handles network errors
- Detects sign-in method conflicts (Google vs email/password)
- Validates email format and password strength

**User Document Management:**
- Automatically creates/updates user document in Firestore on login
- Stores userId and email
- Tracks account creation timestamp

### 9.2 Firestore

#### Database Structure
- **Real-time synchronization**: Uses snapshot listeners for real-time updates
- **User-scoped data**: All queries filtered by userId
- **Security rules**: Enforced at database level (see Section 10)

#### Data Access Patterns
- **Read operations**: Real-time listeners for lists, direct gets for single documents
- **Write operations**: Async operations with callbacks for UI updates
- **Offline support**: Automatic offline caching by Firebase SDK
- **Conflict resolution**: Last-write-wins with server timestamps

#### Collection Indexing
- Minimizes composite indexes by fetching user-scoped docs then filtering in memory
- Optimizes for read performance over write performance
- Uses simple queries where possible

---

## 10. Security Design

### 10.1 Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null 
                        && request.auth.uid == userId;
    }
    
    // Decisions collection
    match /decisions/{decisionId} {
      allow read, write: if request.auth != null 
                        && resource.data.userId == request.auth.uid
                        || request.resource.data.userId == request.auth.uid;
    }
    
    // Activities collection
    match /activities/{activityId} {
      allow read, write: if request.auth != null 
                        && resource.data.userId == request.auth.uid
                        || request.resource.data.userId == request.auth.uid;
    }
    
    // Recovery missions collection
    match /recovery_missions/{missionId} {
      allow read, write: if request.auth != null 
                        && resource.data.userId == request.auth.uid
                        || request.resource.data.userId == request.auth.uid;
    }
    
    // User profiles collection
    match /userProfiles/{userId} {
      allow read, write: if request.auth != null 
                        && request.auth.uid == userId;
    }
    
    // User preferences collection
    match /userPreferences/{userId} {
      allow read, write: if request.auth != null 
                        && request.auth.uid == userId;
    }
    
    // Daily check-ins collection
    match /dailyCheckIns/{checkInId} {
      allow read, write: if request.auth != null 
                        && resource.data.userId == request.auth.uid
                        || request.resource.data.userId == request.auth.uid;
    }
    
    // Cause-effect insights collection
    match /causeEffectInsights/{insightId} {
      allow read, write: if request.auth != null 
                        && resource.data.userId == request.auth.uid
                        || request.resource.data.userId == request.auth.uid;
    }
    
    // Analytics collection
    match /analytics/{analyticsId} {
      allow read, write: if request.auth != null 
                        && resource.data.userId == request.auth.uid
                        || request.resource.data.userId == request.auth.uid;
    }
    
    // Timeline collection
    match /timeline/{eventId} {
      allow read, write: if request.auth != null 
                        && resource.data.userId == request.auth.uid
                        || request.resource.data.userId == request.auth.uid;
    }
  }
}
```

### 10.2 Security Principles
- **Authentication Required**: All data access requires authenticated user
- **User Isolation**: Users can only access their own data
- **UID Validation**: Server-side validation of userId matching
- **No Public Access**: No collections are publicly readable/writable
- **Resource Validation**: Both resource and request.resource validated for writes

### 10.3 Client-Side Security
- **API Key Management**: Gemini API key stored in gradle.properties (not in code)
- **BuildConfig**: API key accessed via BuildConfig.GEMINI_API_KEY
- **Input Validation**: All user inputs validated before processing
- **SQL Injection Prevention**: Uses parameterized Firestore queries
- **XSS Prevention**: No web views or HTML rendering

### 10.4 Data Privacy
- **Minimal Data Collection**: Only essential user data stored
- **No PII Beyond Email**: Only email address collected from Firebase Auth
- **Local Storage**: No sensitive data stored locally
- **Secure Transmission**: All data transmitted over HTTPS via Firebase SDK

---

## 11. External APIs

### 11.1 Google Gemini API
**Purpose**: AI-powered insights generation

**Endpoint**: SDK-based (no direct HTTP endpoint)

**Configuration:**
- Model: gemini-1.5-flash
- API Key: BuildConfig.GEMINI_API_KEY
- Library: com.google.ai.client.generativeai:generativeai:0.9.0

**Usage:**
- Decision pattern analysis
- Actionable recommendation generation
- Activity pattern insights
- Fallback to deterministic insights if API unavailable

**Error Handling:**
- Graceful fallback to pre-defined insights
- Error logging without crashing
- Null result handling

### 11.2 Firebase Services

#### Firebase Authentication
**Purpose**: User authentication

**Methods:**
- Email/password authentication
- Google Sign-In
- Password reset

#### Cloud Firestore
**Purpose**: NoSQL database for data persistence

**Features:**
- Real-time synchronization
- Offline support
- Automatic scaling

#### Google Sign-In
**Purpose**: OAuth authentication

**Library**: com.google.android.gms:play-services-auth:21.1.0

---

## 12. Utilities

### 12.1 DateUtils
Date/time formatting and manipulation utilities.

**Key Methods:**
- `getHourOfDay(long)`: Extract hour from timestamp
- `getHourLabel(int)`: Get time bucket label (Late night, Morning, etc.)
- `formatDate(long)`: Format timestamp to readable date
- `formatTime(long)`: Format timestamp to readable time
- `getCategoryDisplayName(String)`: Get display name for category

### 12.2 ExportUtils
CSV export functionality.

**Key Methods:**
- `exportDecisionHistory(List<DecisionRecord>)`: Export decisions to CSV
- `exportActivityHistory(List<Activity>)`: Export activities to CSV

### 12.3 ThemeManager
Theme management for light/dark mode.

**Key Methods:**
- `applyTheme(Activity)`: Apply selected theme
- `saveThemePreference(String)`: Save theme preference
- `getThemePreference()`: Retrieve saved theme

### 12.4 RepositoryProvider
Dependency injection for repositories.

**Key Methods:**
- `provideDecisionRepository(Context)`: Provide DecisionRepository instance
- `provideActivityRepository(Context)`: Provide ActivityRepository instance
- `provideUserProfileRepository(Context)`: Provide UserProfileRepository instance
- `provideLLMService(Context)`: Provide LLMService instance

---

## 13. Key User Flows

### 13.1 Authentication Flow
```
1. User opens app
2. LoginActivity displayed
3. User enters credentials or selects Google Sign-In
4. AuthHelper validates credentials
5. Firebase Auth authenticates user
6. User document created/updated in Firestore
7. MainActivity launched
8. Bottom navigation displayed
```

### 13.2 Decision Creation Flow
```
1. User navigates to Add Decision screen
2. User enters title, description, category
3. User selects review schedule (3 days, 1 week, custom)
4. Risk meter calculates regret rate based on history
5. If high risk, warning dialog displayed
6. User confirms or cancels
7. Decision saved to Firestore
8. Reminder scheduled via AlarmManager
9. User returns to decision list
```

### 13.3 Decision Evaluation Flow
```
1. Decision becomes ready for review
2. Notification sent to user
3. User opens decision detail
4. User selects outcome (good/bad/neutral)
5. User adds reflection notes
6. Evaluation saved to Firestore
7. XP awarded/penalty applied
8. User profile updated
9. Analytics recalculated
```

### 13.4 Activity Tracking Flow
```
1. User navigates to Activities screen
2. User adds new activity with title, category, scheduled time
3. Activity saved to Firestore
4. Pre-miss reminder scheduled (10 minutes before deadline)
5. User completes or misses activity
6. If completed: XP awarded, reminder canceled
7. If missed: XP penalty applied, pattern detection triggered
8. Recovery mission created if pattern detected
```

### 13.5 Insights Generation Flow
```
1. User navigates to Insights screen
2. ViewModel requests analytics snapshot
3. Repository fetches decisions and activities
4. Repository calculates metrics (quality score, regret rates, etc.)
5. Repository detects patterns (category, time-of-day, activity misses)
6. LLMService generates AI insights (if available)
7. Fallback insights generated if LLM unavailable
8. Insights displayed to user
9. User can toggle brutal mode for harsher insights
```

### 13.6 Recovery Mission Flow
```
1. Pattern detection identifies 3+ consecutive missed activities
2. RecoveryMissionRepository creates recovery mission
3. Mission displayed in insights/dashboard
4. User completes required activities
5. Mission progress updated
6. When complete: XP reward applied
7. Mission marked as completed
```

---

## 14. Testing Strategy

### 14.1 Test Coverage Areas

#### Unit Tests
- Model validation logic
- Repository data transformation
- ViewModel state management
- Utility functions
- Analytics calculations

#### Integration Tests
- Repository-Firestore integration
- AuthHelper-Firebase Auth integration
- LLMService-Gemini API integration
- Notification scheduling

#### UI Tests (Espresso)
- Authentication flow
- Decision creation flow
- Evaluation flow
- Activity tracking flow
- Navigation between screens

#### Manual Test Cases
Comprehensive manual test cases documented in TEST_CASES.md covering:
- Authentication (10 test cases)
- Dashboard (5 test cases)
- Decisions (12 test cases)
- Activities (10 test cases)
- Timeline (6 test cases)
- Insights (6 test cases)
- Profile (6 test cases)
- Daily Check-In (3 test cases)
- Notifications (5 test cases)
- Recovery System (3 test cases)
- Settings (5 test cases)
- Error Handling (8 test cases)

### 14.2 Test Data
- Test email: qa.decisiongraveyard@example.com
- Test password: Qa@123456
- Decision categories: Money, Health, Study, Personal, Work, Relationship
- Activity categories: Study, Health, Work, Personal

### 14.3 Regression Smoke Suite
Before every release:
1. Register/Login
2. Add decision
3. Add activity
4. Open dashboard overview
5. Open dashboard profile tab
6. Open decisions list
7. Open activities list
8. Open timeline
9. Open insights
10. Save settings
11. Logout/Login again

---

## 15. Deployment Strategy

### 15.1 Build Configuration

#### Debug Build
- Minification disabled
- Debug logging enabled
- ProGuard disabled
- API key from local gradle.properties

#### Release Build
- Minification enabled (ProGuard)
- Debug logging disabled
- ProGuard rules applied
- API key from secure build config

### 15.2 Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### 15.3 Distribution
- **Internal Testing**: Debug APK via direct installation
- **External Testing**: Release APK via Google Play Console Internal Testing
- **Production**: Release via Google Play Console

### 15.4 Version Management
- **Version Code**: Incremented for each release
- **Version Name**: Semantic versioning (major.minor.patch)
- **Current Version**: 1.0 (versionCode: 1)

---

## 16. Performance Considerations

### 16.1 Database Optimization
- **Query Optimization**: User-scoped queries to minimize data transfer
- **Indexing Strategy**: Minimal composite indexes, in-memory filtering
- **Real-time Listeners**: Efficient snapshot listeners with proper cleanup
- **Offline Caching**: Leverages Firebase SDK offline support

### 16.2 Memory Management
- **RecyclerView**: Efficient view recycling
- **Image Loading**: No heavy image operations
- **Lifecycle Management**: Proper fragment lifecycle handling
- **Listener Cleanup**: Snapshot listeners unregistered in onDestroy

### 16.3 Battery Optimization
- **WorkManager**: Efficient background task scheduling
- **AlarmManager**: Exact alarms used sparingly
- **Batch Operations**: Firestore writes batched where possible
- **Network Requests**: Minimized through real-time synchronization

### 16.4 Network Optimization
- **Offline Support**: Firebase SDK automatic caching
- **Data Compression**: Minimal data transfer through efficient queries
- **Retry Logic**: Firebase SDK automatic retry
- **Error Handling**: Graceful degradation on network failure

---

## 17. Future Enhancements

### 17.1 Short-term Improvements
- Add explicit schema/versioning docs for Firestore collections
- Add instrumentation tests around notification and receiver flows
- Add CI for assembleDebug + test on pull requests
- Move any remaining placeholder API-key flows out of code
- Add top-level README.md for contributors

### 17.2 Medium-term Features
- Add data export/import functionality
- Implement decision templates
- Add social features (anonymous sharing)
- Implement widget support
- Add dark mode improvements
- Enhance notification customization

### 17.3 Long-term Vision
- Cross-platform support (iOS, Web)
- Advanced AI features (predictive analytics)
- Integration with calendar apps
- Voice input support
- Multi-language support
- Advanced analytics dashboard

---

## 18. Maintenance and Support

### 18.1 Logging Strategy
- **Tag-based Logging**: Each class uses consistent TAG
- **Log Levels**: Appropriate use of DEBUG, INFO, WARN, ERROR
- **Error Tracking**: Firebase Crashlytics integration recommended
- **User Feedback**: In-app feedback mechanism

### 18.2 Monitoring
- **Firebase Analytics**: User behavior tracking
- **Crashlytics**: Crash reporting
- **Performance Monitoring**: App performance metrics
- **Firestore Monitoring**: Database performance and usage

### 18.3 Backup and Recovery
- **Firestore**: Automatic backups by Firebase
- **User Data Export**: CSV export functionality for users
- **Data Migration**: Versioned schema for future migrations

---

## 19. Appendix

### 19.1 File Structure
```
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
│   │   ├── service/                  # LLM service
│   │   ├── ui/                       # Feature fragments/activities
│   │   │   ├── auth/
│   │   │   ├── home/
│   │   │   ├── add/
│   │   │   ├── activities/
│   │   │   ├── insights/
│   │   │   ├── profile/
│   │   │   └── dashboard/
│   │   ├── util/                     # Utilities
│   │   └── viewmodel/                # MVVM view models
│   ├── src/main/res/                 # Layouts, drawables, menus, values
│   └── src/test/                     # Unit tests
├── firestore.rules
├── DOCUMENTATION.md
├── PROJECT_REFERENCE.md
├── TEST_CASES.md
├── PDR.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/ (libs.versions.toml, wrapper config)
```

### 19.2 Glossary
- **MVVM**: Model-View-ViewModel architecture pattern
- **Firestore**: Google's NoSQL cloud database
- **LiveData**: Android's observable data holder class
- **Repository**: Data access pattern that abstracts data sources
- **WorkManager**: Android's library for background task management
- **AlarmManager**: Android system service for scheduling alarms
- **Snapshot Listener**: Firestore real-time data synchronization mechanism
- **BOM**: Bill of Materials for dependency management
- **XP**: Experience points for gamification
- **LLM**: Large Language Model (Gemini in this project)

### 19.3 References
- [Android Developer Documentation](https://developer.android.com/docs)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Google Gemini API Documentation](https://ai.google.dev/docs)
- [Material Design Guidelines](https://material.io/design)

---

## Document Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | May 21, 2026 | Development Team | Initial PDR creation |

---

**End of Document**
