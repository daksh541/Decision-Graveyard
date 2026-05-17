package com.sai.decisiongraveyard.ui.add;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.sai.decisiongraveyard.MainActivity;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.notifications.NotificationScheduler;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.util.DateUtils;
import com.sai.decisiongraveyard.viewmodel.AddDecisionViewModel;

import java.util.Calendar;

public class AddDecisionFragment extends Fragment {

    private AddDecisionViewModel viewModel;
    private DecisionRepository decisionRepository;

    private TextInputLayout layoutTitle;
    private EditText etTitle;
    private EditText etDescription;
    private AutoCompleteTextView actCategory;
    private MaterialButtonToggleGroup scheduleToggleGroup;
    private MaterialButton btnPickDate;
    private TextView tvFutureYouMessage;
    private TextView tvEvaluationDate;
    private MaterialCardView cardRiskMeter;
    private ProgressBar riskProgress;
    private TextView tvRiskValue;
    private TextView tvRiskWarning;
    private View rootView;

    private String selectedSchedule = "3days";
    private int currentRiskLevel = 0;
    private static final String[] CATEGORIES = {"money", "health", "study", "personal", "work", "relationship"};

    public AddDecisionFragment() {
        super(R.layout.fragment_add_decision);
    }

    public static AddDecisionFragment newInstance() {
        return new AddDecisionFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_decision, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(AddDecisionViewModel.class);

        decisionRepository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getDecisionRepository();

        layoutTitle = view.findViewById(R.id.layoutTitle);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        actCategory = view.findViewById(R.id.actCategory);
        scheduleToggleGroup = view.findViewById(R.id.scheduleToggleGroup);
        btnPickDate = view.findViewById(R.id.btnPickDate);
        tvFutureYouMessage = view.findViewById(R.id.tvFutureYouMessage);
        tvEvaluationDate = view.findViewById(R.id.tvEvaluationDate);
        cardRiskMeter = view.findViewById(R.id.cardRiskMeter);
        riskProgress = view.findViewById(R.id.riskProgress);
        tvRiskValue = view.findViewById(R.id.tvRiskValue);
        tvRiskWarning = view.findViewById(R.id.tvRiskWarning);
        Button btnSaveDecision = view.findViewById(R.id.btnSaveDecision);

        setupCategoryDropdown();
        setupScheduleToggle();
        setupRiskMeter();

        btnPickDate.setOnClickListener(button -> showDatePicker());
        btnSaveDecision.setOnClickListener(button -> attemptSaveDecision());

        viewModel.getCustomDateMillis().observe(getViewLifecycleOwner(), selectedDate -> updateFutureYouPreview());
        viewModel.getSaveState().observe(getViewLifecycleOwner(), saveState -> {
            if (saveState == null) return;
            Toast.makeText(requireContext(), saveState.message, Toast.LENGTH_SHORT).show();
            if (saveState.success) {
                NotificationScheduler.scheduleReminder(
                        requireContext(),
                        saveState.decisionId,
                        saveState.title,
                        saveState.evaluationTime
                );
                clearForm();
                returnToDecisionList();
            } else if ("Title is required.".equals(saveState.message)) {
                layoutTitle.setError(saveState.message);
                restoreFormAfterFailedSave();
            } else {
                restoreFormAfterFailedSave();
            }
            viewModel.clearSaveState();
        });

        updateFutureYouPreview();
    }

    private void setupCategoryDropdown() {
        String[] displayCategories = {"Money", "Health", "Study", "Personal", "Work", "Relationship"};
        actCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayCategories
        ));
        actCategory.setText(displayCategories[0], false);

        actCategory.setOnItemClickListener((parent, view, position, id) -> {
            updateRiskMeter(CATEGORIES[position]);
        });
    }

    private void setupScheduleToggle() {
        scheduleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.btn3Days) {
                selectedSchedule = "3days";
                btnPickDate.setVisibility(View.GONE);
            } else if (checkedId == R.id.btn1Week) {
                selectedSchedule = "1week";
                btnPickDate.setVisibility(View.GONE);
            } else if (checkedId == R.id.btnCustom) {
                selectedSchedule = "custom";
                btnPickDate.setVisibility(View.VISIBLE);
            }
            updateFutureYouPreview();
        });

        // Select 3 days by default
        scheduleToggleGroup.check(R.id.btn3Days);
    }

    private void setupRiskMeter() {
        // Initially hidden until we have data
        cardRiskMeter.setVisibility(View.GONE);
    }

    private void updateRiskMeter(String category) {
        // Get category stats from repository
        new Thread(() -> {
            int regretRate = calculateRegretRate(category);
            requireActivity().runOnUiThread(() -> {
                if (regretRate > 0) {
                    cardRiskMeter.setVisibility(View.VISIBLE);
                    animateRiskProgress(regretRate);

                    String riskText;
                    int riskColor;
                    if (regretRate < 30) {
                        riskText = "Low risk category";
                        riskColor = R.color.success;
                        applyRiskVisuals(false, 0);
                    } else if (regretRate < 60) {
                        riskText = "Medium risk - be careful";
                        riskColor = R.color.warning;
                        applyRiskVisuals(false, 0);
                    } else {
                        riskText = "⚠️ HIGH RISK - You've made this mistake before";
                        riskColor = R.color.danger;
                        applyRiskVisuals(true, regretRate);
                    }
                    currentRiskLevel = regretRate;
                    tvRiskValue.setText(riskText);
                    tvRiskValue.setTextColor(ContextCompat.getColor(requireContext(), riskColor));
                } else {
                    cardRiskMeter.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private int calculateRegretRate(String category) {
        int evaluatedCount = 0;
        int badCount = 0;

        for (DecisionRecord record : decisionRepository.getAllDecisionRecords()) {
            if (!record.isEvaluated()) {
                continue;
            }

            String recordCategory = record.getDecision().getCategory();
            if (recordCategory == null || !recordCategory.equalsIgnoreCase(category)) {
                continue;
            }

            evaluatedCount++;
            if ("bad".equalsIgnoreCase(record.getEvaluation().getOutcome())) {
                badCount++;
            }
        }

        if (evaluatedCount == 0) {
            return 0;
        }
        return (int) Math.round(badCount * 100.0 / evaluatedCount);
    }

    private void animateRiskProgress(int targetProgress) {
        ValueAnimator animator = ValueAnimator.ofInt(riskProgress.getProgress(), targetProgress);
        animator.setDuration(800);
        animator.setInterpolator(new AnticipateOvershootInterpolator());
        animator.addUpdateListener(animation -> {
            riskProgress.setProgress((int) animation.getAnimatedValue());
        });
        animator.start();
    }

    private void applyRiskVisuals(boolean highRisk, int riskLevel) {
        if (rootView == null) return;
        
        if (highRisk && riskLevel >= 60) {
            // Apply reddish tint to the screen
            rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.risk_background));
            
            // Show pulse animation on risk card
            if (cardRiskMeter != null) {
                cardRiskMeter.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.danger));
                cardRiskMeter.setStrokeWidth(3);
                
                // Pulse animation
                ValueAnimator pulse = ValueAnimator.ofFloat(1f, 1.05f, 1f);
                pulse.setDuration(1000);
                pulse.setRepeatCount(ValueAnimator.INFINITE);
                pulse.addUpdateListener(animation -> {
                    float scale = (float) animation.getAnimatedValue();
                    cardRiskMeter.setScaleX(scale);
                    cardRiskMeter.setScaleY(scale);
                });
                pulse.start();
            }
            
            // Show warning text
            if (tvRiskWarning != null) {
                tvRiskWarning.setText("⚠️ This category has burned you before. Think twice.");
                tvRiskWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
                tvRiskWarning.setVisibility(View.VISIBLE);
            }
            
            // Vibrate warning
            vibrate(new long[]{0, 100, 100, 100}, -1);
        } else {
            // Reset to normal
            rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface));
            if (cardRiskMeter != null) {
                cardRiskMeter.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.divider));
                cardRiskMeter.setStrokeWidth(1);
                cardRiskMeter.setScaleX(1f);
                cardRiskMeter.setScaleY(1f);
            }
            if (tvRiskWarning != null) {
                tvRiskWarning.setVisibility(View.GONE);
            }
        }
    }

    private void vibrate(long[] pattern, int repeat) {
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat));
            } else {
                vibrator.vibrate(pattern, repeat);
            }
        }
    }

    private void vibrateOneShot(long duration) {
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    private void attemptSaveDecision() {
        layoutTitle.setError(null);
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();

        if (title.isEmpty()) {
            layoutTitle.setError("Title is required.");
            return;
        }

        // Check for pre-decision warning
        checkDecisionWarning(title);
    }

    private void checkDecisionWarning(String decisionTitle) {
        String category = getSelectedCategory();
        int currentHour = DateUtils.getHourOfDay(System.currentTimeMillis());

        new Thread(() -> {
            DecisionRepository.DecisionWarning warning = decisionRepository.checkDecisionWarning(category, currentHour);
            requireActivity().runOnUiThread(() -> {
                if (warning != null && warning.shouldWarn) {
                    showWarningDialog(decisionTitle, warning);
                } else {
                    showConsequenceMomentDialog(decisionTitle);
                }
            });
        }).start();
    }

    private void showWarningDialog(String decisionTitle, DecisionRepository.DecisionWarning warning) {
        String message = warning.message;
        if (warning.recentOutcomes != null && !warning.recentOutcomes.isEmpty()) {
            message += "\n\nRecent outcomes: " + TextUtils.join(", ", warning.recentOutcomes);
        }
        message += "\n\nRegret rate: " + warning.regretRate + "%";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Warning")
                .setMessage(message)
                .setPositiveButton("Continue Anyway", (dialog, which) -> {
                    // Force pause for risky decisions
                    showPauseCountdownDialog(decisionTitle);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void showPauseCountdownDialog(String decisionTitle) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pause_countdown, null);
        TextView tvCountdown = dialogView.findViewById(R.id.tvCountdown);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_DecisionGraveyard_FullScreenDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        final int[] countdown = {10};
        tvCountdown.setText(String.valueOf(countdown[0]));
        btnConfirm.setEnabled(false);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdown[0]--;
                if (countdown[0] > 0) {
                    tvCountdown.setText(String.valueOf(countdown[0]));
                    tvCountdown.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120)
                            .withEndAction(() -> tvCountdown.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                            .start();
                    handler.postDelayed(this, 1000);
                } else {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Decision");
                    tvCountdown.setText("0");
                    tvCountdown.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary));
                }
            }
        };
        handler.postDelayed(countdownRunnable, 1000);

        btnConfirm.setOnClickListener(v -> {
            handler.removeCallbacks(countdownRunnable);
            dialog.dismiss();
            showConsequenceMomentDialog(decisionTitle);
        });

        btnCancel.setOnClickListener(v -> {
            handler.removeCallbacks(countdownRunnable);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showConsequenceMomentDialog(String decisionTitle) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_consequence_moment, null);

        TextView tvLockDuration = dialogView.findViewById(R.id.tvLockDuration);
        TextView tvPreviewTitle = dialogView.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewCategory = dialogView.findViewById(R.id.tvPreviewCategory);
        TextView tvFutureMessage = dialogView.findViewById(R.id.tvFutureMessage);
        Button btnConfirmSeal = dialogView.findViewById(R.id.btnConfirmSeal);
        Button btnCancelSeal = dialogView.findViewById(R.id.btnCancelSeal);

        // Set lock duration text
        String durationText;
        String daysText;
        if ("1week".equals(selectedSchedule)) {
            durationText = "Locked for 7 days";
            daysText = "7 days";
        } else if ("custom".equals(selectedSchedule)) {
            durationText = "Locked until review date";
            daysText = "the scheduled date";
        } else {
            durationText = "Locked for 3 days";
            daysText = "3 days";
        }
        tvLockDuration.setText(durationText);

        // Set preview
        tvPreviewTitle.setText(decisionTitle);
        tvPreviewCategory.setText(DateUtils.getCategoryDisplayName(getSelectedCategory()));

        // Future Self Visualization
        tvFutureMessage.setText("In " + daysText + ", you will come back and judge this decision.\n\nWill your future self be proud or disappointed?");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_DecisionGraveyard_FullScreenDialog);
        builder.setView(dialogView);
        builder.setCancelable(false);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        btnConfirmSeal.setOnClickListener(v -> {
            dialog.dismiss();
            performSaveWithEffect(decisionTitle);
        });

        btnCancelSeal.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void performSaveWithEffect(String title) {
        android.util.Log.d("AddDecisionFragment", "performSaveWithEffect called with title: " + title);

        // Vibrate for impact
        vibrateOneShot(200);

        // Fade out effect
        if (rootView != null) {
            rootView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            try {
                                android.util.Log.d("AddDecisionFragment", "Animation ended, calling saveDecision");
                                String description = etDescription.getText() == null ? "" : etDescription.getText().toString();
                                String category = getSelectedCategory();
                                long evaluationTime = resolveEvaluationTime();
                                android.util.Log.d("AddDecisionFragment", "Calling viewModel.saveDecision with: title=" + title + ", category=" + category + ", evalTime=" + evaluationTime);
                                viewModel.saveDecision(title, description, category, evaluationTime);
                            } catch (Exception e) {
                                android.util.Log.e("AddDecisionFragment", "Error in onAnimationEnd: " + e.getMessage(), e);
                                e.printStackTrace();
                            }
                        }
                    });
        } else {
            try {
                android.util.Log.d("AddDecisionFragment", "No rootView, calling saveDecision directly");
                String description = etDescription.getText() == null ? "" : etDescription.getText().toString();
                String category = getSelectedCategory();
                long evaluationTime = resolveEvaluationTime();
                android.util.Log.d("AddDecisionFragment", "Calling viewModel.saveDecision with: title=" + title + ", category=" + category + ", evalTime=" + evaluationTime);
                viewModel.saveDecision(title, description, category, evaluationTime);
            } catch (Exception e) {
                android.util.Log.e("AddDecisionFragment", "Error in performSaveWithEffect: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }
    }

    private String getSelectedCategory() {
        String displayText = actCategory.getText() == null ? "" : actCategory.getText().toString();
        switch (displayText.toLowerCase()) {
            case "money": return "money";
            case "health": return "health";
            case "study": return "study";
            case "personal": return "personal";
            case "work": return "work";
            case "relationship":
            case "love": return "relationship";
            default: return "personal";
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) ->
                        viewModel.setCustomDateMillis(DateUtils.buildNineAmTimestamp(year, month, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateFutureYouPreview() {
        boolean customSelected = "custom".equals(selectedSchedule);
        btnPickDate.setVisibility(customSelected ? View.VISIBLE : View.GONE);

        long evaluationTime = resolveEvaluationTime();

        // Update Future You message based on selection
        String message;
        if (customSelected && viewModel.getCustomDateMillis().getValue() == null) {
            message = getString(R.string.future_you_custom);
        } else if ("1week".equals(selectedSchedule)) {
            message = getString(R.string.future_you_1week);
        } else {
            message = getString(R.string.future_you_3days);
        }
        tvFutureYouMessage.setText(message);

        // Animate date text change
        String dateText = "Review on " + DateUtils.formatDateTime(evaluationTime);
        tvEvaluationDate.setText(dateText);
    }

    private long resolveEvaluationTime() {
        long now = System.currentTimeMillis();
        if ("1week".equals(selectedSchedule)) {
            return now + 7L * 24L * 60L * 60L * 1000L;
        }
        if ("custom".equals(selectedSchedule)) {
            Long customDate = viewModel.getCustomDateMillis().getValue();
            return customDate != null ? customDate : now - 1L;
        }
        return now + 3L * 24L * 60L * 60L * 1000L;
    }

    private void clearForm() {
        etTitle.setText("");
        etDescription.setText("");
        actCategory.setText("Money", false);
        scheduleToggleGroup.check(R.id.btn3Days);
        selectedSchedule = "3days";
        viewModel.clearCustomDate();
        cardRiskMeter.setVisibility(View.GONE);
        updateFutureYouPreview();
    }

    private void restoreFormAfterFailedSave() {
        if (rootView != null) {
            rootView.animate().cancel();
            rootView.setAlpha(1f);
        }
    }

    private void returnToDecisionList() {
        if (!isAdded()) {
            return;
        }

        if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToTab(R.id.nav_decisions);
        }
    }
}
