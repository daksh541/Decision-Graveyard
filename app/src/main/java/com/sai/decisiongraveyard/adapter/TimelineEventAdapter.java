package com.sai.decisiongraveyard.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.TimelineEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimelineEventAdapter extends RecyclerView.Adapter<TimelineEventAdapter.TimelineEventViewHolder> {

    private final List<TimelineEvent> events = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public void updateEvents(List<TimelineEvent> newEvents) {
        events.clear();
        events.addAll(newEvents);
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
        holder.bind(events.get(position), timeFormat);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class TimelineEventViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardTimelineEvent;
        private final TextView tvEventMarker;
        private final View viewTimelineLine;
        private final TextView tvEventType;
        private final TextView tvEventTime;
        private final TextView tvEventScore;
        private final TextView tvEventTitle;
        private final TextView tvEventTagPrimary;
        private final TextView tvEventTagSecondary;
        private final TextView tvEventDescription;
        private final View viewEventDivider;
        private final View layoutTags;

        TimelineEventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTimelineEvent = itemView.findViewById(R.id.cardTimelineEvent);
            tvEventMarker = itemView.findViewById(R.id.tvEventMarker);
            viewTimelineLine = itemView.findViewById(R.id.viewTimelineLine);
            tvEventType = itemView.findViewById(R.id.tvEventType);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvEventScore = itemView.findViewById(R.id.tvEventScore);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventTagPrimary = itemView.findViewById(R.id.tvEventTagPrimary);
            tvEventTagSecondary = itemView.findViewById(R.id.tvEventTagSecondary);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            viewEventDivider = itemView.findViewById(R.id.viewEventDivider);
            layoutTags = itemView.findViewById(R.id.layoutTags);
        }

        void bind(TimelineEvent event, SimpleDateFormat timeFormat) {
            boolean isDecision = "decision".equals(event.getEventType());
            int accentColor = resolveAccentColor(event);

            tvEventType.setText(isDecision ? "DECISION" : "ACTIVITY");
            tvEventType.setTextColor(ContextCompat.getColor(itemView.getContext(), accentColor));
            tvEventTime.setText(timeFormat.format(new Date(event.getTimestamp())));
            tvEventTitle.setText(event.getTitle() == null || event.getTitle().trim().isEmpty() ? "Untitled event" : event.getTitle());

            tvEventScore.setText(buildScoreLabel(event));
            tvEventScore.setTextColor(ContextCompat.getColor(itemView.getContext(), accentColor));

            tvEventMarker.setText(isDecision ? "D" : "A");
            tvEventMarker.setTextColor(ContextCompat.getColor(itemView.getContext(), accentColor));
            tvEventMarker.setBackground(makeMarkerBackground(accentColor));
            viewTimelineLine.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.divider));
            cardTimelineEvent.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.divider));

            bindTag(tvEventTagPrimary, buildPrimaryTag(event));
            bindTag(tvEventTagSecondary, buildSecondaryTag(event));

            String description = event.getDescription();
            boolean hasDescription = description != null && !description.trim().isEmpty();
            tvEventDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
            tvEventDescription.setText(hasDescription ? description : "");
            viewEventDivider.setVisibility((hasDescription || hasVisibleTags()) ? View.VISIBLE : View.GONE);
            layoutTags.setVisibility(hasVisibleTags() ? View.VISIBLE : View.GONE);
        }

        private boolean hasVisibleTags() {
            return tvEventTagPrimary.getVisibility() == View.VISIBLE || tvEventTagSecondary.getVisibility() == View.VISIBLE;
        }

        private void bindTag(TextView view, String value) {
            if (value == null || value.trim().isEmpty()) {
                view.setVisibility(View.GONE);
                return;
            }
            view.setVisibility(View.VISIBLE);
            view.setText(value);
        }

        private String buildPrimaryTag(TimelineEvent event) {
            if (event.isNegative()) {
                return "High Risk";
            }
            if (event.isPositive()) {
                return "Win";
            }
            return "Pending";
        }

        private String buildSecondaryTag(TimelineEvent event) {
            String category = event.getCategory();
            if (category == null || category.trim().isEmpty()) {
                return "";
            }
            return capitalize(category);
        }

        private String buildScoreLabel(TimelineEvent event) {
            if ("decision".equals(event.getEventType())) {
                if ("good".equals(event.getStatus())) {
                    return "+10 XP";
                }
                if ("bad".equals(event.getStatus())) {
                    return "-15 XP";
                }
                return "0 XP";
            }

            if ("completed".equals(event.getStatus())) {
                return "+20 XP";
            }
            if ("missed".equals(event.getStatus())) {
                return "-5 XP";
            }
            return "+5 XP";
        }

        private int resolveAccentColor(TimelineEvent event) {
            if (event.isPositive()) {
                return R.color.success;
            }
            if (event.isNegative()) {
                return R.color.danger;
            }
            if ("decision".equals(event.getEventType())) {
                return R.color.primary;
            }
            return R.color.text_secondary;
        }

        private GradientDrawable makeMarkerBackground(int strokeColorRes) {
            int strokeColor = ContextCompat.getColor(itemView.getContext(), strokeColorRes);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(ContextCompat.getColor(itemView.getContext(), R.color.surface));
            drawable.setStroke(2, strokeColor);
            return drawable;
        }

        private String capitalize(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }
            return value.substring(0, 1).toUpperCase(Locale.getDefault()) + value.substring(1);
        }
    }
}
