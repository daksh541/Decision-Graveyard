package com.sai.decisiongraveyard.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.HeatmapCell;

import java.util.ArrayList;
import java.util.List;

public class HabitHeatmapView extends View {

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private List<HeatmapCell> cells = new ArrayList<>();

    public HabitHeatmapView(Context context) {
        super(context);
        init();
    }

    public HabitHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HabitHeatmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_tertiary));
        labelPaint.setTextSize(dp(11f));
    }

    public void setCells(List<HeatmapCell> cells) {
        this.cells = cells == null ? new ArrayList<>() : new ArrayList<>(cells);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp(128f);
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, resolvedHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cells.isEmpty()) {
            return;
        }

        int columns = 7;
        int rows = (int) Math.ceil(cells.size() / (float) columns);
        float spacing = dp(8f);
        float topPadding = dp(8f);
        float bottomPadding = dp(20f);
        float usableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float cellSize = (usableWidth - ((columns - 1) * spacing)) / columns;

        for (int index = 0; index < cells.size(); index++) {
            HeatmapCell cell = cells.get(index);
            int row = index / columns;
            int column = index % columns;
            float left = getPaddingLeft() + column * (cellSize + spacing);
            float top = topPadding + row * (cellSize + spacing);

            rect.set(left, top, left + cellSize, top + cellSize);
            cellPaint.setColor(resolveIntensityColor(cell.getIntensity()));
            canvas.drawRoundRect(rect, dp(8f), dp(8f), cellPaint);
        }

        float labelY = topPadding + rows * (cellSize + spacing) + bottomPadding;
        String[] labels = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < labels.length; i++) {
            float left = getPaddingLeft() + i * (cellSize + spacing) + (cellSize / 2f);
            float textWidth = labelPaint.measureText(labels[i]);
            canvas.drawText(labels[i], left - (textWidth / 2f), labelY, labelPaint);
        }
    }

    private int resolveIntensityColor(int intensity) {
        switch (intensity) {
            case 4:
                return ContextCompat.getColor(getContext(), R.color.success);
            case 3:
                return ContextCompat.getColor(getContext(), R.color.primary);
            case 2:
                return ContextCompat.getColor(getContext(), R.color.pending_container);
            case 1:
                return ContextCompat.getColor(getContext(), R.color.surface_elevated);
            default:
                return ContextCompat.getColor(getContext(), R.color.surface_variant);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
