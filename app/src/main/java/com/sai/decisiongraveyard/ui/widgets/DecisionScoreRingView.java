package com.sai.decisiongraveyard.ui.widgets;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sai.decisiongraveyard.R;

public class DecisionScoreRingView extends View {

    private static final float START_ANGLE = -90f;
    private static final float STROKE_WIDTH_DP = 12f;
    private static final float GLOW_WIDTH_DP = 22f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private float progress = 0f;
    private int trackColor;
    private int glowColor;

    public DecisionScoreRingView(Context context) {
        super(context);
        init(context);
    }

    public DecisionScoreRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DecisionScoreRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        trackColor = ContextCompat.getColor(context, R.color.surface_variant);
        glowColor = ContextCompat.getColor(context, R.color.primary);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.NORMAL));
    }

    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(100f, value));
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float strokeWidth = dp(STROKE_WIDTH_DP);
        float glowWidth = dp(GLOW_WIDTH_DP);
        float padding = glowWidth + dp(4f);

        trackPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeWidth(strokeWidth);
        glowPaint.setStrokeWidth(glowWidth);

        float size = Math.min(getWidth(), getHeight()) - padding * 2f;
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        arcBounds.set(left, top, left + size, top + size);

        trackPaint.setColor(trackColor);
        canvas.drawArc(arcBounds, START_ANGLE, 360f, false, trackPaint);

        if (progress <= 0f) {
            return;
        }

        int[] colors = new int[]{
                ContextCompat.getColor(getContext(), R.color.primary),
                ContextCompat.getColor(getContext(), R.color.secondary),
                ContextCompat.getColor(getContext(), R.color.primary)
        };
        float[] positions = new float[]{0f, 0.5f, 1f};
        SweepGradient gradient = new SweepGradient(
                arcBounds.centerX(),
                arcBounds.centerY(),
                colors,
                positions
        );
        progressPaint.setShader(gradient);
        glowPaint.setColor(glowColor);

        float sweep = 360f * (progress / 100f);
        canvas.drawArc(arcBounds, START_ANGLE, sweep, false, glowPaint);
        canvas.drawArc(arcBounds, START_ANGLE, sweep, false, progressPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
