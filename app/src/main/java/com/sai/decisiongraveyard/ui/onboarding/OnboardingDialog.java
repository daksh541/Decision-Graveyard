package com.sai.decisiongraveyard.ui.onboarding;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.sai.decisiongraveyard.R;

public class OnboardingDialog extends Dialog {

    private static final String PREFS_NAME = "onboarding_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private int currentStep = 0;
    private ImageView ivOnboardingImage;
    private TextView tvOnboardingTitle;
    private TextView tvOnboardingDescription;
    private Button btnNext;
    private Button btnSkip;
    private Button btnBack;
    private View indicator1;
    private View indicator2;
    private LinearLayout layoutGoalsStep;
    private LinearLayout layoutBaselineStep;
    private SeekBar seekImpulseControl;
    private SeekBar seekDeepFocus;
    private SeekBar seekConsistency;
    private TextView tvImpulseValue;
    private TextView tvFocusValue;
    private TextView tvConsistencyValue;

    private final OnboardingCompleteListener listener;

    public interface OnboardingCompleteListener {
        void onOnboardingCompleted();
        void onOnboardingSkipped();
    }

    public OnboardingDialog(@NonNull Context context, OnboardingCompleteListener listener) {
        super(context, R.style.Theme_DecisionGraveyard);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_onboarding);
        setCancelable(false);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        bindViews();
        setupClickListeners();
        setupSeekBars();
        updateStep();
    }

    private void bindViews() {
        ivOnboardingImage = findViewById(R.id.ivOnboardingImage);
        tvOnboardingTitle = findViewById(R.id.tvOnboardingTitle);
        tvOnboardingDescription = findViewById(R.id.tvOnboardingDescription);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        btnBack = findViewById(R.id.btnBack);
        indicator1 = findViewById(R.id.indicator1);
        indicator2 = findViewById(R.id.indicator2);
        layoutGoalsStep = findViewById(R.id.layoutGoalsStep);
        layoutBaselineStep = findViewById(R.id.layoutBaselineStep);
        seekImpulseControl = findViewById(R.id.seekImpulseControl);
        seekDeepFocus = findViewById(R.id.seekDeepFocus);
        seekConsistency = findViewById(R.id.seekConsistency);
        tvImpulseValue = findViewById(R.id.tvImpulseValue);
        tvFocusValue = findViewById(R.id.tvFocusValue);
        tvConsistencyValue = findViewById(R.id.tvConsistencyValue);
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> {
            if (currentStep < 1) {
                currentStep++;
                updateStep();
            } else {
                completeOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> skipOnboarding());
        btnBack.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                updateStep();
            }
        });
    }

    private void setupSeekBars() {
        seekImpulseControl.setOnSeekBarChangeListener(simpleListener(value ->
                updateValuePill(tvImpulseValue, value, "Struggling", "Average", "Disciplined")));
        seekDeepFocus.setOnSeekBarChangeListener(simpleListener(value ->
                updateValuePill(tvFocusValue, value, "Distracted", "Average", "Locked In")));
        seekConsistency.setOnSeekBarChangeListener(simpleListener(value ->
                updateValuePill(tvConsistencyValue, value, "Needs Work", "Building", "Relentless")));

        updateValuePill(tvImpulseValue, seekImpulseControl.getProgress(), "Struggling", "Average", "Disciplined");
        updateValuePill(tvFocusValue, seekDeepFocus.getProgress(), "Distracted", "Average", "Locked In");
        updateValuePill(tvConsistencyValue, seekConsistency.getProgress(), "Needs Work", "Building", "Relentless");
    }

    private void updateStep() {
        updateIndicators();
        btnBack.setVisibility(currentStep == 0 ? View.INVISIBLE : View.VISIBLE);
        btnSkip.setVisibility(currentStep == 1 ? View.GONE : View.VISIBLE);
        layoutGoalsStep.setVisibility(currentStep == 0 ? View.VISIBLE : View.GONE);
        layoutBaselineStep.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);

        switch (currentStep) {
            case 0:
                ivOnboardingImage.setImageResource(R.drawable.ic_decisions);
                tvOnboardingTitle.setText("Calibrate Your Discipline Score");
                tvOnboardingDescription.setText("To accurately track your decisions, we need to understand your baseline. Select what matters most and where you usually lose control.");
                btnNext.setText("Next Step");
                break;
            case 1:
                ivOnboardingImage.setImageResource(R.drawable.ic_insights);
                tvOnboardingTitle.setText("Establish Your Baseline");
                tvOnboardingDescription.setText("Be honest with yourself. This helps the system calibrate interventions and feedback to your actual behavior patterns.");
                btnNext.setText("Finish Calibration");
                break;
        }
    }

    private void updateIndicators() {
        View[] indicators = {indicator1, indicator2};
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setBackgroundResource(i <= currentStep ? R.drawable.bg_badge_primary : R.drawable.bg_badge_surface);
        }
    }

    private SeekBar.OnSeekBarChangeListener simpleListener(ValueConsumer consumer) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                consumer.accept(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    private void updateValuePill(TextView textView, int value, String low, String medium, String high) {
        if (value < 35) {
            textView.setText(low);
            textView.setBackgroundResource(R.drawable.bg_badge_warning);
        } else if (value < 70) {
            textView.setText(medium);
            textView.setBackgroundResource(R.drawable.bg_badge_surface);
        } else {
            textView.setText(high);
            textView.setBackgroundResource(R.drawable.bg_badge_primary);
        }
    }

    private void completeOnboarding() {
        setOnboardingCompleted(true);
        dismiss();
        if (listener != null) {
            listener.onOnboardingCompleted();
        }
    }

    private void skipOnboarding() {
        setOnboardingCompleted(true);
        dismiss();
        if (listener != null) {
            listener.onOnboardingSkipped();
        }
    }

    private void setOnboardingCompleted(boolean completed) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public static boolean hasCompletedOnboarding(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    private interface ValueConsumer {
        void accept(int value);
    }
}
