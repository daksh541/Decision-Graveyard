package com.sai.decisiongraveyard.adapter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.sai.decisiongraveyard.R;

public class DecisionSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface OnSwipeListener {
        void onSwipedRight(int position);
        void onSwipedLeft(int position);
    }

    private final DecisionAdapter adapter;
    private final OnSwipeListener listener;
    private final Paint backgroundPaint;
    private final Paint iconPaint;

    private static final float SWIPE_THRESHOLD = 0.25f;

    public DecisionSwipeCallback(DecisionAdapter adapter, OnSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.listener = listener;

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;

        if (direction == ItemTouchHelper.RIGHT) {
            listener.onSwipedRight(position);
        } else if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipedLeft(position);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return SWIPE_THRESHOLD;
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX,
                            float dY,
                            int actionState,
                            boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            float width = itemView.getWidth();
            float height = itemView.getHeight();

            if (dX > 0) {
                // Swiping right - Good (Green)
                backgroundPaint.setColor(ContextCompat.getColor(recyclerView.getContext(), R.color.success));
                RectF background = new RectF(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                canvas.drawRect(background, backgroundPaint);

                // Draw "GOOD" text
                drawSwipeText(canvas, "GOOD", itemView.getLeft() + dX * 0.3f, itemView.getTop() + height / 2f, R.color.white, recyclerView);
            } else if (dX < 0) {
                // Swiping left - Bad (Red)
                backgroundPaint.setColor(ContextCompat.getColor(recyclerView.getContext(), R.color.danger));
                RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                canvas.drawRect(background, backgroundPaint);

                // Draw "BAD" text
                drawSwipeText(canvas, "BAD", itemView.getRight() + dX * 0.3f, itemView.getTop() + height / 2f, R.color.white, recyclerView);
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawSwipeText(Canvas canvas, String text, float x, float y, int colorRes, RecyclerView recyclerView) {
        iconPaint.setColor(ContextCompat.getColor(recyclerView.getContext(), colorRes));
        iconPaint.setTextSize(48f);
        iconPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y + 16f, iconPaint);
    }
}
