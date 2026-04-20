package com.sai.decisiongraveyard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sai.decisiongraveyard.R;

import java.util.ArrayList;
import java.util.List;

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
        holder.tvInsightText.setText(insights.get(position));
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

        private final TextView tvInsightText;

        InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInsightText = itemView.findViewById(R.id.tvInsightText);
        }
    }
}
