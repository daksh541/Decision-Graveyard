# Decision Graveyard Test Cases

## Scope

This document covers manual functional test cases for the Android app:

- Authentication
- Dashboard
- Decisions
- Activities
- Timeline
- Insights
- Profile
- Daily check-in
- Notifications
- Recovery system
- Settings
- Error handling and edge cases

Environment assumptions:

- Firebase Authentication is configured
- Firestore is connected
- Test device is Android 8+ and notifications are enabled where needed
- A clean test account is available

## Test Data

Suggested test user data:

- Email: `qa.decisiongraveyard@example.com`
- Password: `Qa@123456`

Suggested decision categories:

- Money
- Health
- Study
- Personal
- Work
- Relationship

Suggested activity categories:

- Study
- Health
- Work
- Personal

## Authentication

### AUTH-001 Register with valid email and password

- Preconditions: User is logged out
- Steps:
  1. Open app
  2. Tap `Register`
  3. Enter valid email
  4. Enter valid password
  5. Confirm password
  6. Tap `Create account`
- Expected:
  - Account is created successfully
  - User is redirected to the main app
  - User document is created in Firestore

### AUTH-002 Register with invalid email

- Preconditions: User is logged out
- Steps:
  1. Open register screen
  2. Enter invalid email format
  3. Enter valid passwords
  4. Tap `Create account`
- Expected:
  - Email field shows validation error
  - Account is not created

### AUTH-003 Register with short password

- Preconditions: User is logged out
- Steps:
  1. Open register screen
  2. Enter valid email
  3. Enter password shorter than 6 characters
  4. Tap `Create account`
- Expected:
  - Password validation error is shown
  - Account is not created

### AUTH-004 Register with mismatched confirm password

- Preconditions: User is logged out
- Steps:
  1. Open register screen
  2. Enter valid email
  3. Enter password
  4. Enter different confirm password
  5. Tap `Create account`
- Expected:
  - Confirm password error is shown
  - Account is not created

### AUTH-005 Login with valid email/password

- Preconditions: Account exists
- Steps:
  1. Open login screen
  2. Enter valid credentials
  3. Tap `Login`
- Expected:
  - Login succeeds
  - User lands on main screen

### AUTH-006 Login with wrong password

- Preconditions: Account exists
- Steps:
  1. Enter valid email
  2. Enter wrong password
  3. Tap `Login`
- Expected:
  - Error message is shown
  - User remains on login screen

### AUTH-007 Reset password with valid email

- Preconditions: Account exists
- Steps:
  1. Enter registered email on login screen
  2. Tap `Forgot password`
- Expected:
  - Reset email is sent
  - Confirmation toast/message is shown

### AUTH-008 Google sign-in success

- Preconditions: Google sign-in is configured
- Steps:
  1. Tap `Sign in with Google`
  2. Select valid Google account
- Expected:
  - User is authenticated
  - User is redirected to main screen
  - Firestore user document exists

### AUTH-009 Session persistence

- Preconditions: User is already logged in
- Steps:
  1. Force close app
  2. Re-open app
- Expected:
  - User stays logged in
  - App opens main screen directly

### AUTH-010 Logout

- Preconditions: User is logged in
- Steps:
  1. Open settings/profile logout action
  2. Confirm logout
- Expected:
  - User is signed out
  - User is redirected to login screen

## Dashboard

### DASH-001 Dashboard loads for new user

- Preconditions: Fresh account with no decisions or activities
- Steps:
  1. Login
  2. Open `Dashboard`
- Expected:
  - Screen loads without crash
  - Level, XP, streaks, and summary show zero/default values

### DASH-002 Overview tab opens by default

- Preconditions: User is logged in
- Steps:
  1. Open `Dashboard`
- Expected:
  - `Overview` tab is selected by default

### DASH-003 Profile tab opens from dashboard tabs

- Preconditions: User is logged in
- Steps:
  1. Open `Dashboard`
  2. Tap `Profile`
- Expected:
  - Profile content is shown
  - No crash during tab switch

### DASH-004 Dashboard updates after adding decision

- Preconditions: User is logged in
- Steps:
  1. Add a new decision
  2. Return to dashboard
- Expected:
  - Today decision count updates
  - No stale state or crash

### DASH-005 Dashboard updates after adding activity

- Preconditions: User is logged in
- Steps:
  1. Add a new activity
  2. Return to dashboard
- Expected:
  - Today activity count updates
  - No failed precondition error toast

## Decisions

### DEC-001 Create decision with valid data

- Preconditions: User is logged in
- Steps:
  1. Open `Decisions`
  2. Tap `New Decision`
  3. Enter title
  4. Enter description
  5. Select category
  6. Select review schedule
  7. Save
- Expected:
  - Decision is created
  - Reminder is scheduled
  - User returns to decision list

### DEC-002 Create decision with empty title

- Preconditions: User is logged in
- Steps:
  1. Open add decision screen
  2. Leave title empty
  3. Tap save
- Expected:
  - Inline title error is shown
  - Decision is not saved

### DEC-003 Create decision with custom past review date

- Preconditions: User is logged in
- Steps:
  1. Open add decision screen
  2. Select custom date in the past
  3. Save
- Expected:
  - Validation error is shown
  - Decision is not saved

### DEC-004 Duplicate decision prevention within 24 hours

- Preconditions: User already created a decision with same title/category
- Steps:
  1. Attempt to create same decision again within 24 hours
- Expected:
  - Duplicate warning/error is shown
  - Second decision is blocked

### DEC-005 Pre-decision warning is shown for risky pattern

- Preconditions: Category/time pattern exists with high regret rate
- Steps:
  1. Open add decision form
  2. Select risky category/time window
  3. Save
- Expected:
  - Warning dialog appears before final save

### DEC-006 Continue through risky warning flow

- Preconditions: Warning dialog is shown
- Steps:
  1. Tap `Continue Anyway`
  2. Wait for pause countdown
  3. Confirm decision
- Expected:
  - Decision is still saved successfully

### DEC-007 Cancel risky warning flow

- Preconditions: Warning dialog is shown
- Steps:
  1. Tap `Cancel`
- Expected:
  - Dialog closes
  - Decision is not saved

### DEC-008 Decision list loads with saved records

- Preconditions: User has saved decisions
- Steps:
  1. Open `Decisions`
- Expected:
  - Decision list renders correctly
  - Each row shows category, status, and review timing

### DEC-009 Swipe right to mark decision good

- Preconditions: Decision is ready for review
- Steps:
  1. Open `Decisions`
  2. Swipe decision right
- Expected:
  - Decision is evaluated as `good`
  - XP increases
  - UI refreshes

### DEC-010 Swipe left to mark decision bad

- Preconditions: Decision is ready for review
- Steps:
  1. Open `Decisions`
  2. Swipe decision left
- Expected:
  - Decision is evaluated as `bad`
  - XP penalty is applied
  - UI refreshes

### DEC-011 Decision detail screen opens

- Preconditions: Decision exists
- Steps:
  1. Tap a decision item
- Expected:
  - Detail screen opens
  - Correct decision data is displayed

### DEC-012 Delete decision

- Preconditions: Decision exists
- Steps:
  1. Open decision details
  2. Delete decision
- Expected:
  - Decision is removed from Firestore
  - Decision disappears from list

## Activities

### ACT-001 Create activity with valid data

- Preconditions: User is logged in
- Steps:
  1. Open `Activities`
  2. Tap `Add Activity`
  3. Enter title
  4. Select category
  5. Choose future time
  6. Save
- Expected:
  - Activity is created
  - Pre-miss reminder is scheduled
  - User returns to activity list

### ACT-002 Create activity with empty title

- Preconditions: User is logged in
- Steps:
  1. Open add activity screen
  2. Leave title empty
  3. Save
- Expected:
  - Inline title error appears
  - Activity is not saved

### ACT-003 Create activity without selecting time

- Preconditions: User is logged in
- Steps:
  1. Enter title
  2. Do not select time
  3. Save
- Expected:
  - Validation message is shown
  - Activity is not saved

### ACT-004 Activity list loads without Firestore index errors

- Preconditions: User has saved activities
- Steps:
  1. Open `Activities`
  2. Open `Dashboard`
- Expected:
  - Activities load successfully
  - No `FAILED_PRECONDITION` toast appears

### ACT-005 Filter activities by pending

- Preconditions: User has mixed-status activities
- Steps:
  1. Open `Activities`
  2. Tap `Pending`
- Expected:
  - Only pending activities are shown

### ACT-006 Filter activities by completed

- Preconditions: Completed activities exist
- Steps:
  1. Tap `Completed`
- Expected:
  - Only completed activities are shown

### ACT-007 Filter activities by missed

- Preconditions: Missed activities exist
- Steps:
  1. Tap `Missed`
- Expected:
  - Only missed activities are shown

### ACT-008 Mark activity completed

- Preconditions: Pending activity exists
- Steps:
  1. Tap `Complete` on an activity
- Expected:
  - Status changes to `completed`
  - XP increases
  - Pre-miss reminder is canceled

### ACT-009 Delete activity

- Preconditions: Activity exists
- Steps:
  1. Tap `Delete`
- Expected:
  - Activity is removed
  - Reminder is canceled

### ACT-010 Missed activity auto-detection

- Preconditions: Activity scheduled in near future
- Steps:
  1. Create activity
  2. Wait past scheduled time without completing it
  3. Re-open activities/dashboard
- Expected:
  - Activity status becomes `missed`

## Timeline

### TIME-001 Timeline opens without crash

- Preconditions: User is logged in
- Steps:
  1. Open `Timeline`
- Expected:
  - Screen opens
  - App does not crash

### TIME-002 Timeline shows empty state for new account

- Preconditions: No decisions or activities exist
- Steps:
  1. Open `Timeline`
- Expected:
  - Empty-state message is visible

### TIME-003 Timeline shows decision entries

- Preconditions: At least one decision exists
- Steps:
  1. Open `Timeline`
- Expected:
  - Day card is visible
  - Decision appears under correct day

### TIME-004 Timeline shows activity entries

- Preconditions: At least one activity exists
- Steps:
  1. Open `Timeline`
- Expected:
  - Activity appears under correct day

### TIME-005 Timeline color coding

- Preconditions: Good, bad, completed, missed, pending events exist
- Steps:
  1. Open `Timeline`
- Expected:
  - Positive events show success color
  - Negative events show danger color
  - Pending events show neutral/pending color

### TIME-006 Timeline refreshes after new data

- Preconditions: Timeline screen has already been opened once
- Steps:
  1. Add a decision/activity
  2. Return to timeline
- Expected:
  - Timeline refreshes and shows latest data

## Insights

### INS-001 Insights loads without crash

- Preconditions: User is logged in
- Steps:
  1. Open `Insights`
- Expected:
  - Screen loads successfully

### INS-002 Insights empty state for new user

- Preconditions: No evaluated decisions exist
- Steps:
  1. Open insights
- Expected:
  - Empty insights state is displayed

### INS-003 Insights quality score after evaluations

- Preconditions: User has evaluated decisions
- Steps:
  1. Open insights
- Expected:
  - Quality score is shown
  - Good/bad/neutral percentages are correct

### INS-004 Brutal mode toggle

- Preconditions: Insights screen loaded
- Steps:
  1. Enable brutal mode
  2. Disable brutal mode
- Expected:
  - Insights refresh accordingly
  - No crash or duplicate rows

### INS-005 Category insight generation

- Preconditions: Evaluated decisions exist across categories
- Steps:
  1. Open insights
- Expected:
  - Category bars/cards appear
  - Worst categories are highlighted correctly

### INS-006 Cause-effect insight generation

- Preconditions: Missed activities and bad decisions exist in correlated pattern
- Steps:
  1. Open insights
- Expected:
  - Cause-effect statements appear

## Profile

### PROF-001 Profile tab opens

- Preconditions: User is logged in
- Steps:
  1. Open `Dashboard`
  2. Tap `Profile`
- Expected:
  - Profile screen renders inside dashboard tab container

### PROF-002 Profile shows email

- Preconditions: Logged-in user exists
- Steps:
  1. Open profile tab
- Expected:
  - Logged-in email is displayed correctly

### PROF-003 Profile shows level and XP

- Preconditions: User profile exists
- Steps:
  1. Open profile tab
- Expected:
  - Level label is shown
  - XP progress is shown

### PROF-004 Profile shows decision stats

- Preconditions: Decisions exist
- Steps:
  1. Open profile tab
- Expected:
  - Total, evaluated, and pending counts are correct

### PROF-005 Profile shows activity completion rate

- Preconditions: Activities exist
- Steps:
  1. Open profile tab
- Expected:
  - Completion rate matches profile metrics

### PROF-006 Profile logout works

- Preconditions: User is logged in
- Steps:
  1. Open profile tab
  2. Tap `Logout`
  3. Confirm
- Expected:
  - User is logged out
  - Login screen opens

## Daily Check-In

### CHECK-001 Daily check-in appears once per day

- Preconditions: User is logged in, check-in not shown today
- Steps:
  1. Open app
  2. Dismiss check-in
  3. Re-open app same day
- Expected:
  - Check-in appears only once that day

### CHECK-002 Complete daily check-in

- Preconditions: Check-in dialog is visible
- Steps:
  1. Answer check-in prompts
  2. Submit
- Expected:
  - Check-in is saved
  - Streak logic updates

### CHECK-003 Skip daily check-in

- Preconditions: Check-in dialog is visible
- Steps:
  1. Skip dialog
- Expected:
  - App continues normally
  - Dialog is not shown again the same day

## Notifications

### NOTIF-001 Notification permission prompt on supported Android version

- Preconditions: Android 13+ device, first app launch
- Steps:
  1. Open app
- Expected:
  - Notification permission prompt is shown

### NOTIF-002 Decision reminder notification

- Preconditions: Decision with short evaluation time exists
- Steps:
  1. Save decision
  2. Wait until evaluation time
- Expected:
  - Reminder notification is received

### NOTIF-003 Pre-miss reminder notification

- Preconditions: Activity exists with near deadline
- Steps:
  1. Create activity
  2. Wait until pre-miss window
- Expected:
  - Pre-miss reminder is received

### NOTIF-004 Completed activity cancels pre-miss reminder

- Preconditions: Activity and pre-miss reminder are scheduled
- Steps:
  1. Complete activity before deadline
- Expected:
  - Reminder is canceled

### NOTIF-005 Boot receiver reschedules reminders

- Preconditions: Notifications were scheduled
- Steps:
  1. Restart device
  2. Open app if needed
- Expected:
  - Scheduled reminders still work

## Recovery System

### REC-001 Recovery mission created after repeated misses

- Preconditions: User has repeated missed activities pattern
- Steps:
  1. Miss several activities in a row
  2. Open insights/dashboard
- Expected:
  - Recovery mission is created

### REC-002 Recovery mission progress updates

- Preconditions: Active recovery mission exists
- Steps:
  1. Complete required activities
- Expected:
  - Mission progress increases correctly

### REC-003 Recovery mission completion reward

- Preconditions: Recovery mission is near completion
- Steps:
  1. Complete final required activity
- Expected:
  - Mission is marked complete
  - XP reward is applied

## Settings

### SET-001 Settings screen opens

- Preconditions: User is logged in
- Steps:
  1. Open settings from toolbar
- Expected:
  - Settings screen opens without crash

### SET-002 Save selected goals

- Preconditions: Settings screen open
- Steps:
  1. Select one or more goals
  2. Save
- Expected:
  - Goals are stored in Firestore
  - Success message is shown

### SET-003 Save normal notification mode

- Preconditions: Settings screen open
- Steps:
  1. Select `Normal`
  2. Save
- Expected:
  - Preference is persisted

### SET-004 Save brutal notification mode

- Preconditions: Settings screen open
- Steps:
  1. Select `Brutal`
  2. Save
- Expected:
  - Preference is persisted

### SET-005 Settings persistence after restart

- Preconditions: Settings saved
- Steps:
  1. Force close app
  2. Re-open settings
- Expected:
  - Previously saved values are restored

## Error Handling and Stability

### ERR-001 App handles empty Firestore collections

- Preconditions: Fresh user account
- Steps:
  1. Open all major tabs
- Expected:
  - No crashes
  - Empty states shown where needed

### ERR-002 App handles offline mode during list loading

- Preconditions: User has existing data, internet turned off
- Steps:
  1. Disable network
  2. Open dashboard/activities/decisions
- Expected:
  - App does not crash
  - Error messaging is shown where appropriate

### ERR-003 App handles offline mode during save

- Preconditions: Network off
- Steps:
  1. Try to save decision
  2. Try to save activity
- Expected:
  - Error is shown
  - App remains stable

### ERR-004 Timeline with mixed data does not crash

- Preconditions: User has decisions and activities with multiple statuses
- Steps:
  1. Open timeline
  2. Scroll through all visible days
- Expected:
  - No crash
  - All rows render properly

### ERR-005 Rapid tab switching

- Preconditions: User logged in
- Steps:
  1. Quickly switch between bottom tabs and dashboard tabs
- Expected:
  - No fragment overlap
  - No crash

### ERR-006 App survives screen rotation

- Preconditions: Any major screen open
- Steps:
  1. Rotate device
- Expected:
  - App does not crash
  - Current screen restores safely

### ERR-007 Notification permission denied

- Preconditions: Android 13+ device
- Steps:
  1. Deny notification permission
  2. Continue using app
- Expected:
  - App remains usable
  - No crash

### ERR-008 Exact alarm permission denied

- Preconditions: Android 12+ device
- Steps:
  1. Deny exact alarm permission
  2. Continue using app
- Expected:
  - App remains usable
  - Warning flow is shown when relevant

## Regression Smoke Suite

Run this before every release:

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

## Suggested Automation Targets

Best candidates for unit/instrumentation automation:

- Auth validation logic
- Add decision validation
- Add activity validation
- Dashboard metric calculations
- Decision quality score calculation
- Activity completion rate calculation
- Timeline event mapping
- Recovery mission creation logic
- Firestore repository filtering logic

