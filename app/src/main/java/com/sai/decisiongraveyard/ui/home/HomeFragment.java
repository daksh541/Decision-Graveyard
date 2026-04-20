package com.sai.decisiongraveyard.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.sai.decisiongraveyard.DecisionDetailActivity;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.adapter.DecisionAdapter;
import com.sai.decisiongraveyard.adapter.DecisionSwipeCallback;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.util.ExportUtils;
import com.sai.decisiongraveyard.viewmodel.HomeViewModel;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private DecisionAdapter decisionAdapter;
    private DecisionRepository repository;

    private TextView tvHomeHeadline;
    private TextView tvEmptyState;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getDecisionRepository();
        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(HomeViewModel.class);

        tvHomeHeadline = view.findViewById(R.id.tvHomeHeadline);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerDecisions);
        ChipGroup chipGroupStatus = view.findViewById(R.id.chipGroupStatus);
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabQuickAction = view.findViewById(R.id.fabQuickAction);

        decisionAdapter = new DecisionAdapter(record -> {
            Intent intent = new Intent(requireContext(), DecisionDetailActivity.class);
            intent.putExtra(DecisionDetailActivity.EXTRA_DECISION_ID, record.getDecision().getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(decisionAdapter);

        // Add swipe-to-evaluate functionality
        DecisionSwipeCallback swipeCallback = new DecisionSwipeCallback(decisionAdapter, new DecisionSwipeCallback.OnSwipeListener() {
            @Override
            public void onSwipedRight(int position) {
                // Swipe right = Good decision
                DecisionRecord record = decisionAdapter.getCurrentList().get(position);
                if (record != null && record.isReadyForReview() && !record.isEvaluated()) {
                    repository.saveEvaluation(record.getDecision().getId(), "good", "", new DecisionRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            viewModel.refresh();
                            android.widget.Toast.makeText(requireContext(), "Marked as Good (+10 XP)", android.widget.Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            android.widget.Toast.makeText(requireContext(), "Failed to save", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onSwipedLeft(int position) {
                // Swipe left = Bad decision
                DecisionRecord record = decisionAdapter.getCurrentList().get(position);
                if (record != null && record.isReadyForReview() && !record.isEvaluated()) {
                    repository.saveEvaluation(record.getDecision().getId(), "bad", "", new DecisionRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            viewModel.refresh();
                            android.widget.Toast.makeText(requireContext(), "Marked as Bad (-5 XP)", android.widget.Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            android.widget.Toast.makeText(requireContext(), "Failed to save", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.get(0) == R.id.chipAll) {
                viewModel.setStatusFilter("all");
            } else if (checkedIds.get(0) == R.id.chipReady) {
                viewModel.setStatusFilter("ready");
            } else {
                viewModel.setStatusFilter("reviewed");
            }
        });

        fabQuickAction.setOnClickListener(v -> showQuickActionDialog());

        viewModel.getScreenState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            tvHomeHeadline.setText(state.totalCount + " decisions total");
            decisionAdapter.submitList(state.filteredRecords);
            tvEmptyState.setVisibility(state.filteredRecords.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(state.filteredRecords.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void showQuickActionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Quick Add");
        builder.setItems(new CharSequence[]{"Add Decision", "Add Activity"}, (dialog, which) -> {
            if (which == 0) {
                // Navigate to Add Decision
                requireActivity().findViewById(R.id.nav_add).performClick();
            } else if (which == 1) {
                // Navigate to Activities
                try {
                    androidx.appcompat.app.AppCompatActivity activity = 
                            (androidx.appcompat.app.AppCompatActivity) requireActivity();
                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new com.sai.decisiongraveyard.ui.activities.ActivityListFragment())
                            .addToBackStack(null)
                            .commit();
                } catch (Exception e) {
                    android.util.Log.e("HomeFragment", "Navigation failed: " + e.getMessage(), e);
                    android.widget.Toast.makeText(requireContext(), "Failed to navigate to activities", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }
}
