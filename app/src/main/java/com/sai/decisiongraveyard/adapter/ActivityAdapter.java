package com.sai.decisiongraveyard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.util.DateUtils;

public class ActivityAdapter extends ListAdapter<Activity, ActivityAdapter.ActivityViewHolder> {

    public interface OnActivityClickListener {
        void onActivityClicked(Activity activity);
        void onCompleteClicked(Activity activity);
        void onDeleteClicked(Activity activity);
    }

    private final OnActivityClickListener listener;

    public ActivityAdapter(OnActivityClickListener listener) {
        super(new DiffUtil.ItemCallback<Activity>() {
            @Override
            public boolean areItemsTheSame(@NonNull Activity oldItem, @NonNull Activity newItem) {
                return oldItem.getActivityId() == newItem.getActivityId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Activity oldItem, @NonNull Activity newItem) {
                return oldItem.isCompleted() == newItem.isCompleted() &&
                        oldItem.getScheduledTime() == newItem.getScheduledTime() &&
                        oldItem.getTitle().equals(newItem.getTitle());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Activity activity = getItem(position);
        holder.bind(activity, listener);
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardActivity;
        private final TextView tvTitle;
        private final TextView tvCategory;
        private final TextView tvScheduledTime;
        private final TextView tvStatus;
        private final Button btnComplete;
        private final Button btnDelete;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            cardActivity = itemView.findViewById(R.id.cardActivity);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvScheduledTime = itemView.findViewById(R.id.tvScheduledTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnComplete = itemView.findViewById(R.id.btnComplete);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Activity activity, OnActivityClickListener listener) {
            tvTitle.setText(activity.getTitle());
            tvCategory.setText(activity.getCategory().substring(0, 1).toUpperCase() + 
                    activity.getCategory().substring(1));
            tvScheduledTime.setText("Scheduled: " + DateUtils.formatDateTime(activity.getScheduledTime()));

            String status = activity.getStatus();
            tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

            int statusColor;
            int cardStrokeColor;
            switch (status) {
                case "completed":
                    statusColor = R.color.success;
                    cardStrokeColor = R.color.success;
                    btnComplete.setVisibility(View.GONE);
                    break;
                case "missed":
                    statusColor = R.color.danger;
                    cardStrokeColor = R.color.danger;
                    btnComplete.setVisibility(View.VISIBLE);
                    break;
                default:
                    statusColor = R.color.warning;
                    cardStrokeColor = R.color.warning;
                    btnComplete.setVisibility(View.VISIBLE);
                    break;
            }

            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColor));
            cardActivity.setStrokeColor(ContextCompat.getColor(itemView.getContext(), cardStrokeColor));

            btnComplete.setOnClickListener(v -> listener.onCompleteClicked(activity));
            btnDelete.setOnClickListener(v -> listener.onDeleteClicked(activity));
            cardActivity.setOnClickListener(v -> listener.onActivityClicked(activity));
        }
    }
}
