package com.sai.decisiongraveyard.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    private static final String PREFS_NAME = "decision_graveyard_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ThemeManager() {
    }

    public static void applySavedTheme(Context context) {
        boolean darkMode = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static void toggleTheme(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        preferences.edit().putBoolean(KEY_DARK_MODE, !darkMode).apply();
        applySavedTheme(context);
    }
}
