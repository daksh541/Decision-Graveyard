package com.sai.decisiongraveyard.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.adapter.ActivityAdapter;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.viewmodel.ActivityViewModel;

public class ActivityListFragment extends Fragment {

    private ActivityViewModel viewModel;
    private ActivityAdapter activityAdapter;

    private TextView tvActivityHeadline;
    private TextView tvPendingCount;
    private TextView tvCompletedCount;
    private TextView tvMissedCount;
    private TextView tvEmptyState;
    private LinearLayout layoutStats;
    private Button btnAddActivity;

    public ActivityListFragment() {
        super(R.layout.fragment_activity_list);
    }

    public static ActivityListFragment newInstance() {
        return new ActivityListFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(ActivityViewModel.class);

        tvActivityHeadline = view.findViewById(R.id.tvActivityHeadline);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvMissedCount = view.findViewById(R.id.tvMissedCount);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        layoutStats = view.findViewById(R.id.layoutStats);
        btnAddActivity = view.findViewById(R.id.btnAddActivity);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerActivities);
        ChipGroup chipGroupStatus = view.findViewById(R.id.chipGroupStatus);

        activityAdapter = new ActivityAdapter(new ActivityAdapter.OnActivityClickListener() {
            @Override
            public void onActivityClicked(Activity activity) {
                // Could show details dialog
            }

            @Override
            public void onCompleteClicked(Activity activity) {
                viewModel.markAsCompleted(activity.getActivityId());
                Toast.makeText(requireContext(), "Activity marked as completed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClicked(Activity activity) {
                viewModel.deleteActivity(activity.getActivityId());
                Toast.makeText(requireContext(), "Activity deleted", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(activityAdapter);

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.get(0) == R.id.chipAll) {
                viewModel.setStatusFilter("all");
            } else if (checkedIds.get(0) == R.id.chipPending) {
                viewModel.setStatusFilter("pending");
            } else if (checkedIds.get(0) == R.id.chipCompleted) {
                viewModel.setStatusFilter("completed");
            } else if (checkedIds.get(0) == R.id.chipMissed) {
                viewModel.setStatusFilter("missed");
            }
        });

        btnAddActivity.setOnClickListener(v -> {
            try {
                androidx.appcompat.app.AppCompatActivity activity = 
                        (androidx.appcompat.app.AppCompatActivity) requireActivity();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new AddActivityFragment())
                        .addToBackStack(null)
                        .commit();
            } catch (Exception e) {
                android.util.Log.e("ActivityListFragment", "Navigation failed: " + e.getMessage(), e);
            }
        });

        viewModel.getActivities().observe(getViewLifecycleOwner(), activities -> {
            updateStats(activities);
            activityAdapter.submitList(activities);
            tvEmptyState.setVisibility(activities.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(activities.isEmpty() ? View.GONE : View.VISIBLE);
            layoutStats.setVisibility(activities.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void updateStats(java.util.List<Activity> activities) {
        int pending = 0, completed = 0, missed = 0;

        for (Activity activity : activities) {
            String status = activity.getStatus();
            switch (status) {
                case "pending":
                    pending++;
                    break;
                case "completed":
                    completed++;
                    break;
                case "missed":
                    missed++;
                    break;
            }
        }

        tvActivityHeadline.setText(activities.size() + " activities tracked");
        tvPendingCount.setText(String.valueOf(pending));
        tvCompletedCount.setText(String.valueOf(completed));
        tvMissedCount.setText(String.valueOf(missed));

        if (missed > 0) {
            tvMissedCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
        } else {
            tvMissedCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
    }
}
