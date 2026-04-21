package com.sai.decisiongraveyard.ui.timeline;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.adapter.TimelineDayAdapter;
import com.sai.decisiongraveyard.repository.TimelineRepository;

import java.util.Collections;

public class TimelineFragment extends Fragment {

    private TimelineRepository repository;
    private RecyclerView recyclerViewTimeline;
    private TimelineDayAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

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
                    
                    if (days.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerViewTimeline.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerViewTimeline.setVisibility(View.VISIBLE);
                        adapter.updateData(days);
                    }
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
}
