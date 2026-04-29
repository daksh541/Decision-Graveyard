package com.sai.decisiongraveyard.ui.checkin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.DailyCheckIn;
import com.sai.decisiongraveyard.viewmodel.DailyCheckInViewModel;

public class DailyCheckInDialog extends DialogFragment {

    private DailyCheckInViewModel viewModel;
    private MaterialButtonToggleGroup toggleDecision;
    private MaterialButtonToggleGroup toggleActivity;
    private Button btnSubmit;
    private Button btnSkip;
    private boolean decisionMade = false;
    private boolean activityCompleted = false;

    public interface CheckInCompleteListener {
        void onCheckInComplete();
        void onCheckInSkipped();
    }

    private CheckInCompleteListener listener;

    public void setCheckInCompleteListener(CheckInCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_daily_checkin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new androidx.lifecycle.ViewModelProvider(
                this,
                androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(DailyCheckInViewModel.class);
        bindViews(view);
        setupListeners();
        loadCheckIn();
    }

    private void bindViews(View view) {
        toggleDecision = view.findViewById(R.id.toggleDecision);
        toggleActivity = view.findViewById(R.id.toggleActivity);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnSkip = view.findViewById(R.id.btnSkip);
    }

    private void setupListeners() {
        toggleDecision.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                decisionMade = checkedId == R.id.btnDecisionYes;
            }
        });

        toggleActivity.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                activityCompleted = checkedId == R.id.btnActivityYes;
            }
        });

        btnSubmit.setOnClickListener(v -> submitCheckIn());
        btnSkip.setOnClickListener(v -> skipCheckIn());
    }

    private void loadCheckIn() {
        viewModel.loadTodayCheckIn();
        viewModel.getTodayCheckIn().observe(getViewLifecycleOwner(), this::updateUI);
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnSubmit.setEnabled(!isLoading);
            btnSkip.setEnabled(!isLoading);
        });
        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                viewModel.clearActionSuccess();
                dismiss();
                if (listener != null) {
                    listener.onCheckInComplete();
                }
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void updateUI(DailyCheckIn checkIn) {
        if (checkIn == null) return;

        if (checkIn.isSubmitted()) {
            toggleDecision.check(checkIn.isDecisionsMade() ? R.id.btnDecisionYes : R.id.btnDecisionNo);
            toggleActivity.check(checkIn.isActivitiesCompleted() ? R.id.btnActivityYes : R.id.btnActivityNo);
        }

        decisionMade = checkIn.isDecisionsMade();
        activityCompleted = checkIn.isActivitiesCompleted();
    }

    private void submitCheckIn() {
        if (toggleDecision.getCheckedButtonId() == View.NO_ID || toggleActivity.getCheckedButtonId() == View.NO_ID) {
            Toast.makeText(getContext(), "Please answer both questions.", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.submitCheckIn(decisionMade, activityCompleted);
    }

    private void skipCheckIn() {
        dismiss();
        if (listener != null) {
            listener.onCheckInSkipped();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
