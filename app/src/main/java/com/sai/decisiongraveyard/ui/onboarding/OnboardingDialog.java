package com.sai.decisiongraveyard.ui.onboarding;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

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

        bindViews();
        setupClickListeners();
        updateStep();
    }

    private void bindViews() {
        ivOnboardingImage = findViewById(R.id.ivOnboardingImage);
        tvOnboardingTitle = findViewById(R.id.tvOnboardingTitle);
        tvOnboardingDescription = findViewById(R.id.tvOnboardingDescription);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> {
            if (currentStep < 3) {
                currentStep++;
                updateStep();
            } else {
                completeOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> skipOnboarding());
    }

    private void updateStep() {
        switch (currentStep) {
            case 0:
                ivOnboardingImage.setImageResource(R.drawable.ic_add);
                tvOnboardingTitle.setText("Welcome to Decision Graveyard");
                tvOnboardingDescription.setText("Track your decisions and evaluate them later to understand your decision-making patterns.");
                btnNext.setText("Next");
                break;
            case 1:
                ivOnboardingImage.setImageResource(R.drawable.ic_lock);
                tvOnboardingTitle.setText("Bury Your Decisions");
                tvOnboardingDescription.setText("Log a decision and set a review date. The decision will be locked until that date to prevent bias.");
                btnNext.setText("Next");
                break;
            case 2:
                ivOnboardingImage.setImageResource(R.drawable.ic_insights);
                tvOnboardingTitle.setText("Evaluate & Learn");
                tvOnboardingDescription.setText("When the review date arrives, evaluate if your decision was good, bad, or neutral. Learn from your patterns.");
                btnNext.setText("Next");
                break;
            case 3:
                ivOnboardingImage.setImageResource(R.drawable.ic_home);
                tvOnboardingTitle.setText("Quick Actions");
                tvOnboardingDescription.setText("Use the Quick Add button for fast entry. Swipe decisions right for 'Good' or left for 'Bad' to evaluate quickly.");
                btnNext.setText("Get Started");
                btnSkip.setVisibility(View.GONE);
                break;
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
}
