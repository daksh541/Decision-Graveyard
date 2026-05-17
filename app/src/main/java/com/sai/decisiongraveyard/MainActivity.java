package com.sai.decisiongraveyard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.adapter.DecisionAdapter;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.notifications.ActivityScheduler;
import com.sai.decisiongraveyard.notifications.NotificationScheduler;
import com.sai.decisiongraveyard.notifications.SmartNotificationScheduler;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.ui.activities.ActivityListFragment;
import com.sai.decisiongraveyard.ui.auth.LoginActivity;
import com.sai.decisiongraveyard.ui.checkin.DailyCheckInDialog;
import com.sai.decisiongraveyard.ui.dashboard.DashboardContainerFragment;
import com.sai.decisiongraveyard.ui.home.HomeFragment;
import com.sai.decisiongraveyard.ui.insights.InsightsFragment;
import com.sai.decisiongraveyard.ui.onboarding.OnboardingDialog;
import com.sai.decisiongraveyard.ui.profile.ProfileFragment;
import com.sai.decisiongraveyard.ui.settings.SettingsFragment;
import com.sai.decisiongraveyard.util.ThemeManager;
import com.sai.decisiongraveyard.util.ViewPressAnimations;

public class MainActivity extends AppCompatActivity implements DecisionAdapter.OnDecisionClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int[] NAV_IDS = {
            R.id.nav_dashboard,
            R.id.nav_activities,
            R.id.nav_decisions,
            R.id.nav_insights,
            R.id.nav_profile
    };

    private MaterialToolbar toolbar;
    private ViewGroup bottomNavigation;
    private FirebaseAuth auth;
    private DecisionRepository decisionRepository;
    private int selectedTabId = R.id.nav_dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.topAppBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this::onToolbarItemSelected);

        auth = FirebaseAuth.getInstance();
        decisionRepository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(this).getDecisionRepository();
        NotificationScheduler.createNotificationChannel(this);
        ActivityScheduler.scheduleMissedActivityCheck(this);
        SmartNotificationScheduler.scheduleDailySummary(this);
        SmartNotificationScheduler.scheduleWeeklyReport(this);
        SmartNotificationScheduler.scheduleBehavioralTrigger(this);

        requestNotificationPermission();
        setupBottomNavigation();
        applyBottomNavFrostedGlass();

        if (savedInstanceState == null) {
            navigateToRootDestination(R.id.nav_dashboard);
        } else {
            updateNavSelection(selectedTabId);
        }
    }

    private void setupBottomNavigation() {
        configureNavItem(R.id.nav_dashboard, R.drawable.ic_home, getString(R.string.dashboard));
        configureNavItem(R.id.nav_activities, R.drawable.ic_activities, getString(R.string.activities));
        configureNavItem(R.id.nav_decisions, R.drawable.ic_decisions, getString(R.string.decisions));
        configureNavItem(R.id.nav_insights, R.drawable.ic_insights, getString(R.string.insights));
        configureNavItem(R.id.nav_profile, R.drawable.ic_profile, getString(R.string.profile));
    }

    private void configureNavItem(int viewId, int iconRes, CharSequence label) {
        View itemView = findViewById(viewId);
        ImageView icon = itemView.findViewById(R.id.navIcon);
        TextView text = itemView.findViewById(R.id.navLabel);
        icon.setImageResource(iconRes);
        text.setText(label);
        itemView.setOnClickListener(v -> navigateToRootDestination(viewId));
        ViewPressAnimations.attach(itemView);
    }

    private void applyBottomNavFrostedGlass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bottomNavigation.setBackgroundResource(R.drawable.bg_bottom_nav);
            bottomNavigation.setClipToOutline(true);
        }
    }

    private void updateNavSelection(int selectedId) {
        for (int navId : NAV_IDS) {
            View item = findViewById(navId);
            boolean selected = navId == selectedId;
            item.setSelected(selected);
            item.setActivated(selected);
            item.setBackgroundResource(selected ? R.drawable.bg_bottom_nav_item_selected : android.R.color.transparent);

            View indicator = item.findViewById(R.id.navIndicator);
            if (indicator != null) {
                indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                if (selected) {
                    indicator.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
                }
            }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = getSystemService(android.app.AlarmManager.class);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        } else {
            if (!OnboardingDialog.hasCompletedOnboarding(this)) {
                showOnboardingDialog();
            }
            showDailyCheckInDialog();
        }
    }

    private void showOnboardingDialog() {
        OnboardingDialog dialog = new OnboardingDialog(this, new OnboardingDialog.OnboardingCompleteListener() {
            @Override
            public void onOnboardingCompleted() {
            }

            @Override
            public void onOnboardingSkipped() {
            }
        });
        dialog.show();
    }

    private void showDailyCheckInDialog() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = sdf.format(new java.util.Date());

        android.content.SharedPreferences prefs = getSharedPreferences("daily_checkin", MODE_PRIVATE);
        String lastCheckInDate = prefs.getString("last_checkin_date", "");

        if (today.equals(lastCheckInDate)) {
            return;
        }

        DailyCheckInDialog dialog = new DailyCheckInDialog();
        dialog.setCheckInCompleteListener(new DailyCheckInDialog.CheckInCompleteListener() {
            @Override
            public void onCheckInComplete() {
                prefs.edit().putString("last_checkin_date", today).apply();
            }

            @Override
            public void onCheckInSkipped() {
                prefs.edit().putString("last_checkin_date", today).apply();
            }
        });
        dialog.show(getSupportFragmentManager(), "daily_check_in");
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void navigateToTab(int itemId) {
        navigateToRootDestination(itemId);
    }

    private boolean onToolbarItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_toggle_theme) {
            ThemeManager.toggleTheme(this);
            recreate();
            return true;
        } else if (itemId == R.id.action_settings) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new SettingsFragment())
                    .addToBackStack("settings")
                    .commit();
            return true;
        } else if (itemId == R.id.action_logout) {
            auth.signOut();
            redirectToLogin();
            return true;
        }
        return false;
    }

    private boolean navigateToRootDestination(int itemId) {
        Fragment selectedFragment = createRootFragment(itemId);
        if (selectedFragment == null) {
            return false;
        }

        selectedTabId = itemId;
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(selectedFragment);
        updateNavSelection(itemId);
        return true;
    }

    private Fragment createRootFragment(int itemId) {
        if (itemId == R.id.nav_dashboard) {
            return new DashboardContainerFragment();
        }
        if (itemId == R.id.nav_activities) {
            return new ActivityListFragment();
        }
        if (itemId == R.id.nav_decisions) {
            return new HomeFragment();
        }
        if (itemId == R.id.nav_insights) {
            return new InsightsFragment();
        }
        if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return null;
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
