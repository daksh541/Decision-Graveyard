package com.sai.decisiongraveyard.util;

import android.view.MotionEvent;
import android.view.View;

public final class ViewPressAnimations {

    private static final float PRESSED_SCALE = 0.95f;
    private static final long DURATION_MS = 90L;

    private ViewPressAnimations() {
    }

    public static void attach(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE).setDuration(DURATION_MS).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(DURATION_MS).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
