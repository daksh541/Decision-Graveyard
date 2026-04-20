package com.sai.decisiongraveyard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.notifications.ActivityScheduler;
import com.sai.decisiongraveyard.notifications.NotificationScheduler;
import com.sai.decisiongraveyard.notifications.SmartNotificationScheduler;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.ui.add.AddDecisionFragment;
import com.sai.decisiongraveyard.ui.activities.ActivityListFragment;
import com.sai.decisiongraveyard.ui.ai.AIAnalysisFragment;
import com.sai.decisiongraveyard.ui.checkin.DailyCheckInDialog;
import com.sai.decisiongraveyard.ui.dashboard.DashboardFragment;
import com.sai.decisiongraveyard.ui.home.HomeFragment;
import com.sai.decisiongraveyard.ui.insights.InsightsFragment;
import com.sai.decisiongraveyard.ui.profile.ProfileFragment;
import com.sai.decisiongraveyard.ui.settings.SettingsFragment;
import com.sai.decisiongraveyard.ui.timeline.TimelineFragment;
import com.sai.decisiongraveyard.adapter.DecisionAdapter;
import com.sai.decisiongraveyard.ui.auth.LoginActivity;
import com.sai.decisiongraveyard.ui.onboarding.OnboardingDialog;
import com.sai.decisiongraveyard.util.ThemeManager;

public class MainActivity extends AppCompatActivity implements DecisionAdapter.OnDecisionClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private DecisionRepository decisionRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.topAppBar);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this::onToolbarItemSelected);

        auth = FirebaseAuth.getInstance();
        decisionRepository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(this).getDecisionRepository();
        NotificationScheduler.createNotificationChannel(this);
        ActivityScheduler.scheduleMissedActivityCheck(this);
        
        // Initialize smart notification system
        SmartNotificationScheduler.scheduleDailySummary(this);
        SmartNotificationScheduler.scheduleWeeklyReport(this);
        SmartNotificationScheduler.scheduleBehavioralTrigger(this);

        // Request POST_NOTIFICATIONS permission on Android 13+
        requestNotificationPermission();

        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }

        // Check SCHEDULE_EXACT_ALARM permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = getSystemService(android.app.AlarmManager.class);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                // Guide user to settings to grant permission
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("To receive timely notifications for decisions and activities, please enable 'Alarms & Reminders' permission in settings.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied - notifications won't work
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        } else {
            // Show onboarding dialog for new users
            if (!OnboardingDialog.hasCompletedOnboarding(this)) {
                showOnboardingDialog();
            }
            // Show daily check-in dialog
            showDailyCheckInDialog();
        }
    }

    private void showOnboardingDialog() {
        OnboardingDialog dialog = new OnboardingDialog(this, new OnboardingDialog.OnboardingCompleteListener() {
            @Override
            public void onOnboardingCompleted() {
                // Onboarding completed
            }

            @Override
            public void onOnboardingSkipped() {
                // Onboarding skipped
            }
        });
        dialog.show();
    }

    private void showDailyCheckInDialog() {
        // Check if check-in has already been shown today
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = sdf.format(new java.util.Date());
        
        android.content.SharedPreferences prefs = getSharedPreferences("daily_checkin", MODE_PRIVATE);
        String lastCheckInDate = prefs.getString("last_checkin_date", "");
        
        if (today.equals(lastCheckInDate)) {
            // Already shown today, don't show again
            return;
        }
        
        DailyCheckInDialog dialog = new DailyCheckInDialog();
        dialog.setCheckInCompleteListener(new DailyCheckInDialog.CheckInCompleteListener() {
            @Override
            public void onCheckInComplete() {
                // Save that check-in was completed today
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("last_checkin_date", today);
                editor.apply();
            }

            @Override
            public void onCheckInSkipped() {
                // Save that check-in was shown today (even if skipped)
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("last_checkin_date", today);
                editor.apply();
            }
        });
        dialog.show(getSupportFragmentManager(), "daily_check_in");
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
                toolbar.setTitle("Dashboard");
            } else if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                toolbar.setTitle(R.string.app_name);
            } else if (itemId == R.id.nav_add) {
                selectedFragment = new AddDecisionFragment();
                toolbar.setTitle(R.string.add_decision);
            } else if (itemId == R.id.nav_insights) {
                selectedFragment = new InsightsFragment();
                toolbar.setTitle("Analytics");
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
                toolbar.setTitle("Settings");
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private boolean onToolbarItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_toggle_theme) {
            ThemeManager.toggleTheme(this);
            recreate();
            return true;
        } else if (itemId == R.id.action_logout) {
            auth.signOut();
            redirectToLogin();
            return true;
        }
        return false;
    }

    @Override
    public void onDecisionClicked(DecisionRecord record) {
        Intent intent = new Intent(this, DecisionDetailActivity.class);
        intent.putExtra(DecisionDetailActivity.EXTRA_DECISION_ID, record.getDecision().getId());
        startActivity(intent);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
