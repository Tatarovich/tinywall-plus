package ru.tihiy.dom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

final class ToggleView extends View {
    interface Listener {
        void onChecked(boolean checked);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Palette palette;
    private boolean checked;
    private float position;
    private ValueAnimator animator;
    private Listener listener;

    ToggleView(Context context, Palette palette, boolean checked) {
        super(context);
        this.palette = palette;
        this.checked = checked;
        this.position = checked ? 1f : 0f;
        setFocusable(true); setFocusableInTouchMode(true);
        setClickable(true);
        setContentDescription("Переключатель");
    }

    void setOnCheckedChangeListener(Listener listener) {
        this.listener = listener;
    }

    boolean isChecked() {
        return checked;
    }

    void setChecked(boolean value) {
        if (checked == value) return;
        checked = value;
        animateTo(value ? 1f : 0f);
    }

    private void animateTo(float target) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(position, target);
        animator.setDuration(160L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            position = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Ui.px(72), Ui.px(42));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float inset = Ui.pxF(4f);
        rect.set(inset, inset, w - inset, h - inset);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float radius = rect.height() / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(checked ? palette.accent : Ui.alpha(palette.text, 0.16f));
        canvas.drawRoundRect(rect, radius, radius, paint);

        float circle = rect.height() - Ui.pxF(6f);
        float left = rect.left + Ui.pxF(3f) + position * (rect.width() - circle - Ui.pxF(6f));
        float top = rect.top + Ui.pxF(3f);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(left + circle / 2f, top + circle / 2f, circle / 2f, paint);

        if (hasFocus()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Ui.px(2));
            paint.setColor(palette.focus);
            RectF ring = new RectF(rect);
            ring.inset(-Ui.pxF(2f), -Ui.pxF(2f));
            canvas.drawRoundRect(ring, radius + Ui.pxF(2f), radius + Ui.pxF(2f), paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    @Override
    public boolean performClick() {
        checked = !checked;
        animateTo(checked ? 1f : 0f);
        if (listener != null) listener.onChecked(checked);
        super.performClick();
        return true;
    }
}
