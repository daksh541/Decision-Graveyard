package com.sai.decisiongraveyard.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sai.decisiongraveyard.R;

public class RiskMeterView extends View {

    private static final float START_ANGLE = 160f;
    private static final float SWEEP_ANGLE = 220f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private float riskValue;

    public RiskMeterView(Context context) {
        super(context);
        init();
    }

    public RiskMeterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RiskMeterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.surface_variant));

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(14f), BlurMaskFilter.Blur.NORMAL));

        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        needlePaint.setStrokeWidth(dp(3f));
        needlePaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
    }

    public void setRiskValue(int value) {
        float target = Math.max(0f, Math.min(100f, value));
        ValueAnimator animator = ValueAnimator.ofFloat(riskValue, target);
        animator.setDuration(700L);
        animator.addUpdateListener(animation -> {
            riskValue = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float stroke = dp(14f);
        float glowStroke = dp(22f);
        float padding = glowStroke;
        float size = Math.min(getWidth(), getHeight() * 1.7f) - padding * 2f;
        float left = (getWidth() - size) / 2f;
        float top = getHeight() - size - dp(12f);
        arcBounds.set(left, top, left + size, top + size);

        trackPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeWidth(stroke);
        glowPaint.setStrokeWidth(glowStroke);

        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        float progressSweep = SWEEP_ANGLE * (riskValue / 100f);
        arcPaint.setColor(resolveRiskColor(riskValue));
        glowPaint.setColor(resolveRiskColor(riskValue));

        canvas.drawArc(arcBounds, START_ANGLE, progressSweep, false, glowPaint);
        canvas.drawArc(arcBounds, START_ANGLE, progressSweep, false, arcPaint);

        drawNeedle(canvas);
    }

    private void drawNeedle(Canvas canvas) {
        float angle = (float) Math.toRadians(START_ANGLE + (SWEEP_ANGLE * (riskValue / 100f)));
        float centerX = arcBounds.centerX();
        float centerY = arcBounds.centerY();
        float radius = arcBounds.width() / 2f - dp(22f);

        float endX = (float) (centerX + Math.cos(angle) * radius);
        float endY = (float) (centerY + Math.sin(angle) * radius);

        canvas.drawLine(centerX, centerY, endX, endY, needlePaint);
        canvas.drawCircle(centerX, centerY, dp(6f), needlePaint);
    }

    private int resolveRiskColor(float value) {
        if (value >= 75f) {
            return ContextCompat.getColor(getContext(), R.color.danger);
        }
        if (value >= 45f) {
            return ContextCompat.getColor(getContext(), R.color.warning);
        }
        return ContextCompat.getColor(getContext(), R.color.success);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
