package com.sai.decisiongraveyard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.notifications.NotificationScheduler;
import com.sai.decisiongraveyard.ui.auth.LoginActivity;
import com.sai.decisiongraveyard.util.DateUtils;
import com.sai.decisiongraveyard.util.ThemeManager;
import com.sai.decisiongraveyard.viewmodel.DecisionDetailViewModel;

public class DecisionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DECISION_ID = "decision_id";

    private DecisionDetailViewModel viewModel;
    private long decisionId;

    private Chip chipCategory;
    private TextView tvDecisionTitle;
    private TextView tvDecisionDescription;
    private TextView tvStatusBadge;
    private TextView tvEvaluationState;
    private TextView tvCreatedDate;
    private TextView tvWaitingDate;
    private TextView tvEvaluatedDate;
    private View dotWaiting;
    private View dotEvaluated;
    private MaterialButtonToggleGroup outcomeToggleGroup;
    private MaterialButton btnGood;
    private MaterialButton btnNeutral;
    private MaterialButton btnBad;
    private EditText etReflection;
    private Button btnSaveEvaluation;
    private Button btnDeleteDecision;
    private MaterialCardView cardEvaluation;

    private String selectedOutcome = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decision_detail);

        decisionId = getIntent().getLongExtra(EXTRA_DECISION_ID, -1L);
        if (decisionId == -1L) {
            finish();
            return;
        }

        bindViews();
        setupOutcomeToggle();

        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(DecisionDetailViewModel.class);

        viewModel.getDecisionRecord().observe(this, this::renderDecision);
        viewModel.getActionState().observe(this, actionState -> {
            if (actionState == null) return;
            Toast.makeText(this, actionState.message, Toast.LENGTH_SHORT).show();
            if (actionState.deleted) {
                NotificationScheduler.cancelReminder(this, decisionId);
                finish();
                return;
            }
            if (actionState.saved) {
                NotificationScheduler.cancelReminder(this, decisionId);
                viewModel.loadDecision(decisionId);
            }
            viewModel.clearActionState();
        });

        btnSaveEvaluation.setOnClickListener(view -> saveEvaluation());
        btnDeleteDecision.setOnClickListener(view -> viewModel.deleteDecision(decisionId));

        viewModel.loadDecision(decisionId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void bindViews() {
        chipCategory = findViewById(R.id.chipCategory);
        tvDecisionTitle = findViewById(R.id.tvDecisionTitle);
        tvDecisionDescription = findViewById(R.id.tvDecisionDescription);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvEvaluationState = findViewById(R.id.tvEvaluationState);
        tvCreatedDate = findViewById(R.id.tvCreatedDate);
        tvWaitingDate = findViewById(R.id.tvWaitingDate);
        tvEvaluatedDate = findViewById(R.id.tvEvaluatedDate);
        dotWaiting = findViewById(R.id.dotWaiting);
        dotEvaluated = findViewById(R.id.dotEvaluated);
        outcomeToggleGroup = findViewById(R.id.outcomeToggleGroup);
        btnGood = findViewById(R.id.btnGood);
        btnNeutral = findViewById(R.id.btnNeutral);
        btnBad = findViewById(R.id.btnBad);
        etReflection = findViewById(R.id.etReflection);
        btnSaveEvaluation = findViewById(R.id.btnSaveEvaluation);
        btnDeleteDecision = findViewById(R.id.btnDeleteDecision);
        cardEvaluation = findViewById(R.id.cardEvaluation);
    }

    private void setupOutcomeToggle() {
        outcomeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                selectedOutcome = null;
                return;
            }
            if (checkedId == R.id.btnGood) {
                selectedOutcome = "good";
            } else if (checkedId == R.id.btnNeutral) {
                selectedOutcome = "neutral";
            } else if (checkedId == R.id.btnBad) {
                selectedOutcome = "bad";
            }
        });
    }

    private void renderDecision(DecisionRecord record) {
        if (record == null) {
            Toast.makeText(this, "Decision not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set category chip
        String category = record.getDecision().getCategory();
        chipCategory.setText(DateUtils.getCategoryDisplayName(category));
        chipCategory.setChipBackgroundColorResource(getCategoryColor(category));
        chipCategory.setTextColor(ContextCompat.getColor(this, R.color.white));

        // Set title and description
        tvDecisionTitle.setText(record.getDecision().getTitle());
        String description = record.getDecision().getDescription();
        tvDecisionDescription.setText(
                description == null || description.trim().isEmpty()
                        ? "No description added."
                        : description
        );

        // Timeline
        tvCreatedDate.setText(DateUtils.formatDateTime(record.getDecision().getCreatedAt()));
        tvWaitingDate.setText("Until " + DateUtils.formatDateTime(record.getDecision().getEvaluationTime()));

        // Status and evaluation
        if (record.isEvaluated()) {
            String outcome = record.getEvaluation().getOutcome();
            tvStatusBadge.setText(getOutcomeLabel(outcome));
            tvStatusBadge.setBackgroundResource(resolveStatusBackground(outcome));
            tvEvaluationState.setText(getEvaluationPrompt(outcome));

            // Timeline
            tvEvaluatedDate.setText(DateUtils.formatDateTime(record.getEvaluation().getEvaluatedAt()));
            dotEvaluated.setBackgroundResource(R.drawable.timeline_dot_active);
            dotWaiting.setBackgroundResource(R.drawable.timeline_dot_active);

            // Set toggle
            int toggleId = resolveOutcomeButton(outcome);
            outcomeToggleGroup.check(toggleId);
            setOutcomeToggleEnabled(false);

            etReflection.setText(record.getEvaluation().getReflectionNotes());
            etReflection.setEnabled(false);
            btnSaveEvaluation.setEnabled(false);
            btnSaveEvaluation.setText("Judgment recorded");
        } else if (record.isReadyForReview()) {
            tvStatusBadge.setText(R.string.status_ready);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvEvaluationState.setText(R.string.evaluation_title);

            // Timeline
            tvEvaluatedDate.setText("Pending...");
            dotWaiting.setBackgroundResource(R.drawable.timeline_dot_active);
            dotEvaluated.setBackgroundResource(R.drawable.timeline_dot_inactive);

            setOutcomeToggleEnabled(true);
            outcomeToggleGroup.clearChecked();
            etReflection.setEnabled(true);
            etReflection.setText("");
            btnSaveEvaluation.setEnabled(true);
            btnSaveEvaluation.setText("Save evaluation");
        } else {
            tvStatusBadge.setText(R.string.status_pending);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_neutral);
            tvEvaluationState.setText(
                    "Evaluation unlocks on " + DateUtils.formatDateTime(record.getDecision().getEvaluationTime())
            );

            // Timeline
            tvEvaluatedDate.setText("Not yet");
            dotWaiting.setBackgroundResource(R.drawable.timeline_dot_pending);
            dotEvaluated.setBackgroundResource(R.drawable.timeline_dot_inactive);

            outcomeToggleGroup.clearChecked();
            setOutcomeToggleEnabled(false);
            etReflection.setText("");
            etReflection.setEnabled(false);
            btnSaveEvaluation.setEnabled(false);
            btnSaveEvaluation.setText("Not available yet");
        }

        cardEvaluation.setAlpha(btnSaveEvaluation.isEnabled() ? 1f : 0.6f);
    }

    private void setOutcomeToggleEnabled(boolean enabled) {
        outcomeToggleGroup.setEnabled(enabled);
        btnGood.setEnabled(enabled);
        btnNeutral.setEnabled(enabled);
        btnBad.setEnabled(enabled);
    }

    private void saveEvaluation() {
        if (selectedOutcome == null) {
            Toast.makeText(this, "Select an outcome first.", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.saveEvaluation(decisionId, selectedOutcome, etReflection.getText().toString());
    }

    private int getCategoryColor(String category) {
        switch (category != null ? category.toLowerCase() : "") {
            case "money": return R.color.category_money;
            case "health": return R.color.category_health;
            case "study": return R.color.category_study;
            case "personal": return R.color.category_personal;
            case "work": return R.color.category_work;
            case "relationship": return R.color.category_relationship;
            default: return R.color.primary;
        }
    }

    private String getOutcomeLabel(String outcome) {
        switch (outcome) {
            case "good": return "GOOD";
            case "bad": return "BAD";
            default: return "MEH";
        }
    }

    private String getEvaluationPrompt(String outcome) {
        switch (outcome) {
            case "good": return getString(R.string.evaluation_prompt_good);
            case "bad": return getString(R.string.evaluation_prompt_bad);
            default: return getString(R.string.evaluation_prompt_neutral);
        }
    }

    private int resolveStatusBackground(String outcome) {
        if ("good".equals(outcome)) return R.drawable.bg_status_positive;
        if ("bad".equals(outcome)) return R.drawable.bg_status_negative;
        return R.drawable.bg_status_neutral;
    }

    private int resolveOutcomeButton(String outcome) {
        if ("good".equals(outcome)) return R.id.btnGood;
        if ("bad".equals(outcome)) return R.id.btnBad;
        return R.id.btnNeutral;
    }
}
