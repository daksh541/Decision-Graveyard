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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        holder.bind(days.get(position));
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    static class TimelineDayViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDayDate;
        private final RecyclerView recyclerViewEvents;
        private final TimelineEventAdapter eventAdapter;
        private final SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        private final SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        TimelineDayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayDate = itemView.findViewById(R.id.tvDayDate);
            recyclerViewEvents = itemView.findViewById(R.id.recyclerViewEvents);

            recyclerViewEvents.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            eventAdapter = new TimelineEventAdapter();
            recyclerViewEvents.setAdapter(eventAdapter);
        }

        void bind(TimelineRepository.TimelineDay day) {
            tvDayDate.setText(buildDayLabel(day));

            List<TimelineEvent> sortedEvents = day.events;
            sortedEvents.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            eventAdapter.updateEvents(sortedEvents);
        }

        private String buildDayLabel(TimelineRepository.TimelineDay day) {
            String relative = day.getRelativeDate();
            if ("Today".equals(relative)) {
                return "Today";
            }
            if ("Yesterday".equals(relative)) {
                return "Yesterday, " + monthDayFormat.format(new Date(day.timestamp));
            }
            return fullFormat.format(new Date(day.timestamp));
        }
    }
}
