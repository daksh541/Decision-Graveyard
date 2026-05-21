package com.sai.decisiongraveyard.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sai.decisiongraveyard.R;

import java.util.ArrayList;
import java.util.List;

public class TrendSparklineView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final List<Integer> values = new ArrayList<>();
    private float revealProgress = 1f;

    public TrendSparklineView(Context context) {
        super(context);
        init();
    }

    public TrendSparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrendSparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeWidth(dp(3f));

        fillPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
    }

    public void setValues(List<Integer> trendValues) {
        values.clear();
        if (trendValues != null) {
            values.addAll(trendValues);
        }
        revealProgress = 0f;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(650L);
        animator.addUpdateListener(animation -> {
            revealProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.size() < 2) {
            return;
        }

        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        float left = getPaddingLeft();
        float top = getPaddingTop();
        int min = 0;
        int max = 100;
        float stepX = width / (values.size() - 1f);

        linePath.reset();
        fillPath.reset();

        float firstX = left;
        float firstY = computeY(values.get(0), top, height, min, max);
        linePath.moveTo(firstX, firstY);
        fillPath.moveTo(firstX, top + height);
        fillPath.lineTo(firstX, firstY);

        int visiblePoints = Math.max(2, Math.round((values.size() - 1) * revealProgress) + 1);
        for (int i = 1; i < visiblePoints; i++) {
            float x = left + stepX * i;
            float y = computeY(values.get(i), top, height, min, max);
            linePath.lineTo(x, y);
            fillPath.lineTo(x, y);
        }

        float lastX = left + stepX * (visiblePoints - 1);
        fillPath.lineTo(lastX, top + height);
        fillPath.close();

        int primary = ContextCompat.getColor(getContext(), R.color.primary);
        int transparent = ContextCompat.getColor(getContext(), R.color.transparent);
        fillPaint.setShader(new LinearGradient(0f, top, 0f, top + height, primary, transparent, Shader.TileMode.CLAMP));
        linePaint.setColor(primary);

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        float finalY = computeY(values.get(visiblePoints - 1), top, height, min, max);
        canvas.drawCircle(lastX, finalY, dp(4f), pointPaint);
    }

    private float computeY(int value, float top, float height, int min, int max) {
        float normalized = (value - min) / (float) (max - min);
        return top + height - (normalized * height);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
