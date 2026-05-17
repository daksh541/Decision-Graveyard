package com.sai.decisiongraveyard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sai.decisiongraveyard.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InsightAdapter extends RecyclerView.Adapter<InsightAdapter.InsightViewHolder> {

    private final List<String> insights = new ArrayList<>();

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_insight,
                parent,
                false
        );
        return new InsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        holder.bind(insights.get(position));
    }

    @Override
    public int getItemCount() {
        return insights.size();
    }

    public void submitList(List<String> newInsights) {
        insights.clear();
        insights.addAll(newInsights);
        notifyDataSetChanged();
    }

    static class InsightViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final TextView tvInsightLabel;
        private final TextView tvInsightText;

        InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvInsightLabel = itemView.findViewById(R.id.tvInsightLabel);
            tvInsightText = itemView.findViewById(R.id.tvInsightText);
        }

        void bind(String insight) {
            String normalized = insight == null ? "" : insight.toLowerCase(Locale.getDefault());

            int backgroundColor;
            int strokeColor;
            int labelColor;
            String label;

            if (normalized.contains("improved") || normalized.contains("better") || normalized.contains("strong")) {
                backgroundColor = R.color.success_container;
                strokeColor = R.color.success;
                labelColor = R.color.success;
                label = "IMPROVEMENT";
            } else if (normalized.contains("inconsistent") || normalized.contains("warning") || normalized.contains("slipping")) {
                backgroundColor = R.color.warning_container;
                strokeColor = R.color.warning;
                labelColor = R.color.warning;
                label = "WARNING";
            } else {
                backgroundColor = R.color.danger_container;
                strokeColor = R.color.danger;
                labelColor = R.color.danger;
                label = "PATTERN DETECTED";
            }

            cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), backgroundColor));
            cardView.setStrokeWidth(2);
            cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), strokeColor));
            tvInsightLabel.setText(label);
            tvInsightLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), labelColor));
            tvInsightText.setText(insight);
        }
    }
}
