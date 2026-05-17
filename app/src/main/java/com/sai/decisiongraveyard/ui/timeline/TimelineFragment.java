package com.sai.decisiongraveyard.ui.timeline;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sai.decisiongraveyard.MainActivity;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.adapter.TimelineDayAdapter;
import com.sai.decisiongraveyard.model.TimelineEvent;
import com.sai.decisiongraveyard.repository.TimelineRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimelineFragment extends Fragment {

    private enum FilterMode {
        ALL,
        DECISIONS,
        ACTIVITIES,
        HIGH_RISK
    }

    private TimelineRepository repository;
    private RecyclerView recyclerViewTimeline;
    private TimelineDayAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private FilterMode filterMode = FilterMode.ALL;
    private List<TimelineRepository.TimelineDay> allDays = Collections.emptyList();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timeline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new TimelineRepository(requireContext());
        bindViews(view);
        setupRecyclerView();
        loadTimeline();
    }

    private void bindViews(View view) {
        recyclerViewTimeline = view.findViewById(R.id.recyclerViewTimeline);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        ImageButton btnCalendar = view.findViewById(R.id.btnCalendar);
        FloatingActionButton btnScrollTop = view.findViewById(R.id.btnScrollTop);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupTimelineFilter);

        btnBack.setOnClickListener(v -> navigateBack());
        btnCalendar.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Calendar view coming soon.", Toast.LENGTH_SHORT).show()
        );
        btnScrollTop.setOnClickListener(v -> recyclerViewTimeline.smoothScrollToPosition(0));

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.get(0) == R.id.chipAllEvents) {
                filterMode = FilterMode.ALL;
            } else if (checkedIds.get(0) == R.id.chipDecisionsOnly) {
                filterMode = FilterMode.DECISIONS;
            } else if (checkedIds.get(0) == R.id.chipActivitiesOnly) {
                filterMode = FilterMode.ACTIVITIES;
            } else if (checkedIds.get(0) == R.id.chipHighRisk) {
                filterMode = FilterMode.HIGH_RISK;
            }
            applyFilter();
        });
    }

    private void setupRecyclerView() {
        adapter = new TimelineDayAdapter(Collections.emptyList());
        recyclerViewTimeline.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewTimeline.setAdapter(adapter);
    }

    private void loadTimeline() {
        requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        });

        repository.getTimeline(30, new TimelineRepository.TimelineCallback() {
            @Override
            public void onSuccess(java.util.List<TimelineRepository.TimelineDay> days) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    allDays = days;
                    applyFilter();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    recyclerViewTimeline.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTimeline();
    }

    private void applyFilter() {
        List<TimelineRepository.TimelineDay> filteredDays = new ArrayList<>();

        for (TimelineRepository.TimelineDay day : allDays) {
            List<TimelineEvent> filteredEvents = new ArrayList<>();
            for (TimelineEvent event : day.events) {
                if (matchesFilter(event)) {
                    filteredEvents.add(event);
                }
            }

            if (!filteredEvents.isEmpty()) {
                TimelineRepository.TimelineDay filteredDay = new TimelineRepository.TimelineDay(day.dateKey, day.timestamp);
                filteredDay.events.addAll(filteredEvents);
                filteredDay.decisionCount = day.decisionCount;
                filteredDay.activityCount = day.activityCount;
                filteredDay.goodDecisions = day.goodDecisions;
                filteredDay.badDecisions = day.badDecisions;
                filteredDay.completedActivities = day.completedActivities;
                filteredDay.missedActivities = day.missedActivities;
                filteredDay.score = day.score;
                filteredDay.completionRate = day.completionRate;
                filteredDays.add(filteredDay);
            }
        }

        boolean empty = filteredDays.isEmpty();
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewTimeline.setVisibility(empty ? View.GONE : View.VISIBLE);
        adapter.updateData(filteredDays);
    }

    private boolean matchesFilter(TimelineEvent event) {
        switch (filterMode) {
            case DECISIONS:
                return "decision".equals(event.getEventType());
            case ACTIVITIES:
                return "activity".equals(event.getEventType());
            case HIGH_RISK:
                return event.isNegative() || event.isPending();
            case ALL:
            default:
                return true;
        }
    }

    private void navigateBack() {
        if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToTab(R.id.nav_dashboard);
            return;
        }

        requireActivity().onBackPressed();
    }
}
