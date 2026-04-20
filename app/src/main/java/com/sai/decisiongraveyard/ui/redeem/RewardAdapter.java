package com.sai.decisiongraveyard.ui.redeem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.Reward;

import java.util.ArrayList;
import java.util.List;

public class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.RewardViewHolder> {

    private List<Reward> rewards = new ArrayList<>();
    private OnRewardClickListener listener;

    public interface OnRewardClickListener {
        void onRewardClick(Reward reward);
    }

    public RewardAdapter(OnRewardClickListener listener) {
        this.listener = listener;
    }

    public void setRewards(List<Reward> rewards) {
        this.rewards = rewards;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        Reward reward = rewards.get(position);
        holder.bind(reward, listener);
    }

    @Override
    public int getItemCount() {
        return rewards.size();
    }

    static class RewardViewHolder extends RecyclerView.ViewHolder {
        private TextView rewardIcon;
        private TextView rewardTitle;
        private TextView rewardDescription;
        private TextView xpCostText;
        private TextView categoryText;
        private View rewardCard;

        public RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            rewardIcon = itemView.findViewById(R.id.reward_icon);
            rewardTitle = itemView.findViewById(R.id.reward_title);
            rewardDescription = itemView.findViewById(R.id.reward_description);
            xpCostText = itemView.findViewById(R.id.xp_cost_text);
            categoryText = itemView.findViewById(R.id.category_text);
            rewardCard = itemView.findViewById(R.id.reward_card);
        }

        public void bind(Reward reward, OnRewardClickListener listener) {
            rewardIcon.setText(reward.getIcon());
            rewardTitle.setText(reward.getTitle());
            rewardDescription.setText(reward.getDescription());
            xpCostText.setText(String.valueOf(reward.getXpCost()) + " XP");
            categoryText.setText(formatCategory(reward.getCategory()));

            rewardCard.setOnClickListener(v -> listener.onRewardClick(reward));
        }

        private String formatCategory(String category) {
            switch (category) {
                case "streak_protection":
                    return "🛡️ Streak Protection";
                case "xp_boost":
                    return "⚡ XP Boost";
                case "theme":
                    return "🎨 Theme";
                case "badge":
                    return "🏅 Badge";
                case "special":
                    return "✨ Special";
                default:
                    return category;
            }
        }
    }
}
