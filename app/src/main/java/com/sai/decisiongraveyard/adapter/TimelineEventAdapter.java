package com.sai.decisiongraveyard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.TimelineEvent;

import java.util.ArrayList;
import java.util.List;

public class TimelineEventAdapter extends RecyclerView.Adapter<TimelineEventAdapter.TimelineEventViewHolder> {

    private List<TimelineEvent> events = new ArrayList<>();

    public void updateEvents(List<TimelineEvent> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_event, parent, false);
        return new TimelineEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineEventViewHolder holder, int position) {
        TimelineEvent event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class TimelineEventViewHolder extends RecyclerView.ViewHolder {

        private View viewColorIndicator;
        private TextView tvEventTitle;
        private TextView tvEventCategory;
        private TextView tvEventStatus;

        public TimelineEventViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorIndicator = itemView.findViewById(R.id.viewColorIndicator);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventCategory = itemView.findViewById(R.id.tvEventCategory);
            tvEventStatus = itemView.findViewById(R.id.tvEventStatus);
        }

        public void bind(TimelineEvent event) {
            String title = event.getTitle();
            String category = event.getCategory();
            tvEventTitle.setText(title == null || title.trim().isEmpty() ? "Untitled event" : title);
            tvEventCategory.setText(category == null || category.trim().isEmpty() ? "General" : capitalize(category));
            
            // Set color based on status
            int color;
            String statusText;
            
            if (event.isPositive()) {
                color = itemView.getContext().getColor(R.color.success);
                statusText = capitalize(event.getStatus());
            } else if (event.isNegative()) {
                color = itemView.getContext().getColor(R.color.danger);
                statusText = capitalize(event.getStatus());
            } else {
                color = itemView.getContext().getColor(android.R.color.darker_gray);
                statusText = "Pending";
            }

            viewColorIndicator.setBackgroundColor(color);
            
            tvEventStatus.setText(statusText);
            tvEventStatus.setTextColor(color);
        }

        private String capitalize(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }
}
