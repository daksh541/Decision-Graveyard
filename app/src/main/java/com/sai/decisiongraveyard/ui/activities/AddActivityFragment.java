package com.sai.decisiongraveyard.ui.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.sai.decisiongraveyard.MainActivity;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.util.DateUtils;
import com.sai.decisiongraveyard.viewmodel.ActivityViewModel;

import java.util.Calendar;

public class AddActivityFragment extends Fragment {

    private ActivityViewModel viewModel;
    private TextInputLayout layoutTitle;
    private EditText etTitle;
    private AutoCompleteTextView actCategory;
    private TextView tvScheduledTime;
    private Button btnPickTime;
    private Button btnSaveActivity;
    private View rootView;

    private long scheduledTimeMillis;
    private static final String[] CATEGORIES = {"study", "health", "work", "personal"};

    public AddActivityFragment() {
        super(R.layout.fragment_add_activity);
    }

    public static AddActivityFragment newInstance() {
        return new AddActivityFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_activity, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(ActivityViewModel.class);

        layoutTitle = view.findViewById(R.id.layoutTitle);
        etTitle = view.findViewById(R.id.etTitle);
        actCategory = view.findViewById(R.id.actCategory);
        tvScheduledTime = view.findViewById(R.id.tvScheduledTime);
        btnPickTime = view.findViewById(R.id.btnPickTime);
        btnSaveActivity = view.findViewById(R.id.btnSaveActivity);

        setupCategoryDropdown();
        setupTimePicker();

        btnSaveActivity.setOnClickListener(button -> attemptSaveActivity());

        viewModel.getSaveState().observe(getViewLifecycleOwner(), saveState -> {
            if (saveState == null) return;
            Toast.makeText(requireContext(), saveState.message, Toast.LENGTH_SHORT).show();
            if (saveState.success) {
                clearForm();
                returnToActivityList();
            } else if ("Title is required.".equals(saveState.message)) {
                layoutTitle.setError(saveState.message);
            }
            viewModel.clearSaveState();
        });

        updateScheduledTimePreview();
    }

    private void setupCategoryDropdown() {
        String[] displayCategories = {"Study", "Health", "Work", "Personal"};
        actCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayCategories
        ));
        actCategory.setText(displayCategories[0], false);
    }

    private void setupTimePicker() {
        btnPickTime.setOnClickListener(button -> showTimePicker());
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(
                requireContext(),
                (timePicker, selectedHour, selectedMinute) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedCalendar.set(Calendar.MINUTE, selectedMinute);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    long selectedTime = selectedCalendar.getTimeInMillis();
                    
                    if (selectedTime <= System.currentTimeMillis()) {
                        selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
                        selectedTime = selectedCalendar.getTimeInMillis();
                    }

                    scheduledTimeMillis = selectedTime;
                    updateScheduledTimePreview();
                },
                hour,
                minute,
                false
        ).show();
    }

    private void updateScheduledTimePreview() {
        if (scheduledTimeMillis == 0) {
            tvScheduledTime.setText("Select a time");
        } else {
            tvScheduledTime.setText("Scheduled: " + DateUtils.formatDateTime(scheduledTimeMillis));
        }
    }

    private void attemptSaveActivity() {
        layoutTitle.setError(null);
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();

        if (title.isEmpty()) {
            layoutTitle.setError("Title is required.");
            return;
        }

        if (scheduledTimeMillis == 0) {
            Toast.makeText(requireContext(), "Please select a scheduled time.", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = getSelectedCategory();
        viewModel.saveActivity(title, category, scheduledTimeMillis);
    }

    private String getSelectedCategory() {
        String displayText = actCategory.getText() == null ? "" : actCategory.getText().toString();
        switch (displayText.toLowerCase()) {
            case "study": return "study";
            case "health": return "health";
            case "work": return "work";
            case "personal": return "personal";
            default: return "personal";
        }
    }

    private void clearForm() {
        etTitle.setText("");
        actCategory.setText("Study", false);
        scheduledTimeMillis = 0;
        updateScheduledTimePreview();
    }

    private void returnToActivityList() {
        if (!isAdded()) {
            return;
        }

        if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_activities);
        } else if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToTab(R.id.nav_activities);
        }
    }
}
