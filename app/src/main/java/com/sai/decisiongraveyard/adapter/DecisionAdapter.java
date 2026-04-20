package com.sai.decisiongraveyard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.util.DateUtils;

public class DecisionAdapter extends ListAdapter<DecisionRecord, DecisionAdapter.DecisionViewHolder> {

    public interface OnDecisionClickListener {
        void onDecisionClicked(DecisionRecord record);
    }

    public interface OnSwipeActionListener {
        void onSwipeRight(DecisionRecord record, int position);
        void onSwipeLeft(DecisionRecord record, int position);
    }

    private final OnDecisionClickListener clickListener;
    private OnSwipeActionListener swipeListener;

    public DecisionAdapter(OnDecisionClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    public void setOnSwipeActionListener(OnSwipeActionListener listener) {
        this.swipeListener = listener;
    }

    private static final DiffUtil.ItemCallback<DecisionRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<DecisionRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull DecisionRecord oldItem, @NonNull DecisionRecord newItem) {
            return oldItem.getDecision().getId() == newItem.getDecision().getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull DecisionRecord oldItem, @NonNull DecisionRecord newItem) {
            return oldItem.getDecision().getTitle().equals(newItem.getDecision().getTitle()) &&
                    oldItem.isEvaluated() == newItem.isEvaluated() &&
                    oldItem.getOutcomeOrPending().equals(newItem.getOutcomeOrPending());
        }
    };

    @Override
    public void onBindViewHolder(@NonNull DecisionViewHolder holder, int position) {
        DecisionRecord record = getItem(position);
        Decision decision = record.getDecision();

        holder.tvDecisionTitle.setText(decision.getTitle());

        // Set category chip
        String category = decision.getCategory();
        holder.chipCategory.setText(DateUtils.getCategoryDisplayName(category));
        holder.chipCategory.setChipBackgroundColorResource(getCategoryColor(category));
        holder.chipCategory.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

        // Set description
        String description = decision.getDescription();
        holder.tvDecisionNote.setText(
                description == null || description.trim().isEmpty()
                        ? "No description added."
                        : description
        );

        // Set status badge and strip
        int statusBgRes;
        int stripBgRes;
        String statusText;
        int countdownColor;

        if (record.isEvaluated()) {
            String outcome = record.getEvaluation().getOutcome();
            statusText = getOutcomeLabel(outcome);
            statusBgRes = "good".equals(outcome) ? R.drawable.bg_status_positive :
                         "bad".equals(outcome) ? R.drawable.bg_status_negative :
                         R.drawable.bg_status_neutral;
            stripBgRes = statusBgRes;
            countdownColor = R.color.text_tertiary;
            holder.tvCountdownValue.setText("--");
            holder.tvCountdownLabel.setText("");
            holder.countdownProgress.setVisibility(View.GONE);
            holder.tvTimeRemaining.setText("Judgment complete");
        } else if (record.isReadyForReview()) {
            statusText = holder.itemView.getContext().getString(R.string.status_ready);
            statusBgRes = R.drawable.bg_status_pending;
            stripBgRes = R.drawable.bg_status_pending;
            countdownColor = R.color.pending;
            holder.tvCountdownValue.setText("NOW");
            holder.tvCountdownLabel.setText("ready");
            holder.countdownProgress.setProgress(100);
            holder.countdownProgress.setIndicatorColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
            holder.tvTimeRemaining.setText("Judgment ready - tap to evaluate");
        } else {
            statusText = holder.itemView.getContext().getString(R.string.status_pending);
            statusBgRes = R.drawable.bg_status_neutral;
            stripBgRes = R.drawable.bg_status_neutral;
            countdownColor = R.color.warning;
            // Calculate countdown with tension
            long remaining = decision.getEvaluationTime() - System.currentTimeMillis();
            long totalDuration = decision.getEvaluationTime() - decision.getCreatedAt();
            if (remaining > 0 && totalDuration > 0) {
                String countdown = DateUtils.formatCountdown(remaining);
                holder.tvCountdownValue.setText(countdown);
                holder.tvCountdownLabel.setText("left");
                
                // Calculate progress (inverse - shows how much time has passed)
                int progress = (int) (((totalDuration - remaining) * 100) / totalDuration);
                holder.countdownProgress.setProgress(progress);
                holder.countdownProgress.setVisibility(View.VISIBLE);
                
                // Detailed countdown text
                String detailedCountdown = DateUtils.formatDetailedCountdown(remaining);
                holder.tvTimeRemaining.setText("Judgment in " + detailedCountdown);
                
                // Change color based on urgency
                if (remaining < 24 * 60 * 60 * 1000) { // Less than 24 hours
                    holder.countdownProgress.setIndicatorColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.danger));
                    holder.tvTimeRemaining.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.danger));
                } else if (remaining < 3 * 24 * 60 * 60 * 1000) { // Less than 3 days
                    holder.countdownProgress.setIndicatorColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning));
                }
            } else {
                holder.tvCountdownValue.setText("0h");
                holder.tvCountdownLabel.setText("left");
                holder.countdownProgress.setProgress(100);
                holder.tvTimeRemaining.setText("Judgment pending...");
            }
        }

        holder.tvStatusBadge.setText(statusText);
        holder.tvStatusBadge.setBackgroundResource(statusBgRes);
        holder.viewStatusStrip.setBackgroundResource(stripBgRes);
        holder.tvCountdownValue.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), countdownColor));

        // Set created date
        holder.tvDecisionDate.setText("Buried " + DateUtils.getRelativeTime(decision.getCreatedAt()));

        // Click listener
        holder.cardDecision.setOnClickListener(view -> clickListener.onDecisionClicked(record));
    }

    private int getCategoryColor(String category) {
        switch (category != null ? category.toLowerCase() : "") {
            case "money":
                return R.color.category_money;
            case "health":
                return R.color.category_health;
            case "study":
                return R.color.category_study;
            case "personal":
                return R.color.category_personal;
            case "work":
                return R.color.category_work;
            case "relationship":
                return R.color.category_relationship;
            default:
                return R.color.primary;
        }
    }

    private String getOutcomeLabel(String outcome) {
        switch (outcome) {
            case "good":
                return "GOOD";
            case "bad":
                return "BAD";
            default:
                return "MEH";
        }
    }

    @NonNull
    @Override
    public DecisionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_decision,
                parent,
                false
        );
        return new DecisionViewHolder(view);
    }

    static class DecisionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardDecision;
        private final TextView tvDecisionTitle;
        private final TextView tvDecisionNote;
        private final TextView tvStatusBadge;
        private final TextView tvDecisionDate;
        private final TextView tvCountdownValue;
        private final TextView tvCountdownLabel;
        private final TextView tvTimeRemaining;
        private final LinearProgressIndicator countdownProgress;
        private final View viewStatusStrip;
        private final Chip chipCategory;

        DecisionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDecision = itemView.findViewById(R.id.cardDecision);
            tvDecisionTitle = itemView.findViewById(R.id.tvDecisionTitle);
            tvDecisionNote = itemView.findViewById(R.id.tvDecisionNote);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvDecisionDate = itemView.findViewById(R.id.tvDecisionDate);
            tvCountdownValue = itemView.findViewById(R.id.tvCountdownValue);
            tvCountdownLabel = itemView.findViewById(R.id.tvCountdownLabel);
            tvTimeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            countdownProgress = itemView.findViewById(R.id.countdownProgress);
            viewStatusStrip = itemView.findViewById(R.id.viewStatusStrip);
            chipCategory = itemView.findViewById(R.id.chipCategory);
        }
    }
}
