package com.sai.decisiongraveyard.ui.redeem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.Reward;
import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.viewmodel.RedeemViewModel;

import java.util.List;

public class RedeemFragment extends Fragment {

    private RedeemViewModel viewModel;
    private RecyclerView rewardsRecyclerView;
    private RewardAdapter rewardAdapter;
    private TextView xpBalanceText;
    private TextView levelText;
    private View progressView;
    private TextView emptyViewText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_redeem, container, false);

        viewModel = new ViewModelProvider(this).get(RedeemViewModel.class);

        initializeViews(view);
        setupRecyclerView();
        observeViewModel();
        loadData();

        return view;
    }

    private void initializeViews(View view) {
        rewardsRecyclerView = view.findViewById(R.id.rewards_recycler_view);
        xpBalanceText = view.findViewById(R.id.xp_balance_text);
        levelText = view.findViewById(R.id.level_text);
        progressView = view.findViewById(R.id.progress_view);
        emptyViewText = view.findViewById(R.id.empty_view_text);
    }

    private void setupRecyclerView() {
        rewardAdapter = new RewardAdapter(new RewardAdapter.OnRewardClickListener() {
            @Override
            public void onRewardClick(Reward reward) {
                viewModel.showRedemptionConfirmation(reward);
            }
        });

        rewardsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        rewardsRecyclerView.setAdapter(rewardAdapter);
    }

    private void observeViewModel() {
        viewModel.getAvailableRewards().observe(getViewLifecycleOwner(), rewards -> {
            if (rewards != null && !rewards.isEmpty()) {
                rewardAdapter.setRewards(rewards);
                emptyViewText.setVisibility(View.GONE);
                rewardsRecyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyViewText.setVisibility(View.VISIBLE);
                rewardsRecyclerView.setVisibility(View.GONE);
            }
        });

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                xpBalanceText.setText(String.valueOf(profile.getCurrentXP()));
                levelText.setText(profile.getLevelName() + " (Lvl " + profile.getCurrentLevel() + ")");
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadData() {
        viewModel.loadAllRewards();
        viewModel.loadUserProfile();
    }
}
