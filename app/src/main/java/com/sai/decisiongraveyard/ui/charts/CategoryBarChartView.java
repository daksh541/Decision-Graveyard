package com.sai.decisiongraveyard.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sai.decisiongraveyard.R;

import java.util.ArrayList;
import java.util.List;

public class CategoryBarChartView extends View {

    public static class BarData {
        public String label;
        public float value;
        public int color;

        public BarData(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private List<BarData> barDataList = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private float maxValue = 100f;
    private RectF barRect = new RectF();

    public CategoryBarChartView(Context context) {
        super(context);
        init();
    }

    public CategoryBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.divider));
    }

    public void setBarData(List<BarData> data) {
        this.barDataList = data;
        if (!data.isEmpty()) {
            maxValue = 0;
            for (BarData bar : data) {
                if (bar.value > maxValue) {
                    maxValue = bar.value;
                }
            }
            if (maxValue == 0) maxValue = 100f;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (barDataList.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float padding = 32f;
        float barWidth = (width - (padding * 2) - (barDataList.size() - 1) * 16f) / barDataList.size();
        float maxBarHeight = height - padding * 2 - 40f; // Leave space for text

        for (int i = 0; i < barDataList.size(); i++) {
            BarData bar = barDataList.get(i);
            float x = padding + i * (barWidth + 16f);
            float barHeight = (bar.value / maxValue) * maxBarHeight;
            float y = height - padding - 40f - barHeight;

            // Draw bar background
            barRect.set(x, height - padding - 40f - maxBarHeight, x + barWidth, height - padding - 40f);
            canvas.drawRoundRect(barRect, 8f, 8f, backgroundPaint);

            // Draw actual bar
            barPaint.setColor(bar.color);
            barRect.set(x, y, x + barWidth, height - padding - 40f);
            canvas.drawRoundRect(barRect, 8f, 8f, barPaint);

            // Draw label
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            textPaint.setTextSize(24f);
            canvas.drawText(bar.label, x + barWidth / 2f, height - padding, textPaint);

            // Draw value
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            textPaint.setTextSize(28f);
            canvas.drawText((int) bar.value + "%", x + barWidth / 2f, y - 8f, textPaint);
        }
    }
}
