package com.sai.decisiongraveyard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.TimelineEvent;
import com.sai.decisiongraveyard.repository.TimelineRepository;

import java.util.List;

public class TimelineDayAdapter extends RecyclerView.Adapter<TimelineDayAdapter.TimelineDayViewHolder> {

    private List<TimelineRepository.TimelineDay> days;

    public TimelineDayAdapter(List<TimelineRepository.TimelineDay> days) {
        this.days = days;
    }

    public void updateData(List<TimelineRepository.TimelineDay> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_day, parent, false);
        return new TimelineDayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineDayViewHolder holder, int position) {
        TimelineRepository.TimelineDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    static class TimelineDayViewHolder extends RecyclerView.ViewHolder {

        private TextView tvDayDate;
        private TextView tvDayScore;
        private TextView tvDecisionCount;
        private TextView tvActivityCount;
        private TextView tvCompletionRate;
        private RecyclerView recyclerViewEvents;
        private TimelineEventAdapter eventAdapter;

        public TimelineDayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayDate = itemView.findViewById(R.id.tvDayDate);
            tvDayScore = itemView.findViewById(R.id.tvDayScore);
            tvDecisionCount = itemView.findViewById(R.id.tvDecisionCount);
            tvActivityCount = itemView.findViewById(R.id.tvActivityCount);
            tvCompletionRate = itemView.findViewById(R.id.tvCompletionRate);
            recyclerViewEvents = itemView.findViewById(R.id.recyclerViewEvents);
            
            recyclerViewEvents.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            eventAdapter = new TimelineEventAdapter();
            recyclerViewEvents.setAdapter(eventAdapter);
        }

        public void bind(TimelineRepository.TimelineDay day) {
            tvDayDate.setText(day.getRelativeDate());
            
            // Set score with color
            tvDayScore.setText("Score: " + day.score);
            if (day.score >= 0) {
                tvDayScore.setTextColor(itemView.getContext().getColor(R.color.success));
            } else {
                tvDayScore.setTextColor(itemView.getContext().getColor(R.color.danger));
            }
            
            tvDecisionCount.setText(String.valueOf(day.decisionCount));
            tvActivityCount.setText(String.valueOf(day.activityCount));
            tvCompletionRate.setText(String.format("%.0f%%", day.completionRate));
            
            // Sort events by timestamp
            List<TimelineEvent> sortedEvents = day.events;
            sortedEvents.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            
            eventAdapter.updateEvents(sortedEvents);
        }
    }
}
