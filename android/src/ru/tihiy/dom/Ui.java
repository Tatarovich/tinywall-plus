package ru.tihiy.dom;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static float scale = 1f;
    static float fontScale = 1.22f;

    private Ui() {}

    static void init(Context context) {
        DisplayMetrics m = new DisplayMetrics();
        android.view.WindowManager wm = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) wm.getDefaultDisplay().getRealMetrics(m);
        else m = context.getResources().getDisplayMetrics();
        scale = Math.min(m.widthPixels / 1920f, m.heightPixels / 1080f);
        if (scale <= 0f) scale = 1f;
    }

    static int px(float design) {
        return Math.max(1, Math.round(design * scale));
    }

    static float pxF(float design) {
        return design * scale;
    }

    static int alpha(int color, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a * 255f)));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    static GradientDrawable round(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(px(radius));
        return d;
    }

    static GradientDrawable roundStroke(int color, float radius, int strokeColor, float strokeWidth) {
        GradientDrawable d = round(color, radius);
        d.setStroke(Math.max(1, px(strokeWidth)), strokeColor);
        return d;
    }

    static StateListDrawable states(GradientDrawable normal, GradientDrawable focused) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{}, normal);
        return s;
    }

    static StateListDrawable focusRing(int normalColor, int focusColor, int border, float radius) {
        GradientDrawable n = roundStroke(normalColor, radius, border, 1);
        GradientDrawable f = roundStroke(focusColor, radius, border, 1);
        return states(n, f);
    }

    static TextView text(Context c, String value, float size, int color, boolean medium) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size * fontScale));
        t.setTextColor(color);
        t.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static void margins(View v, int l, int t, int r, int b) {
        if (!(v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        p.setMargins(px(l), px(t), px(r), px(b));
        v.setLayoutParams(p);
    }

    static int w(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    static int h(Context c) {
        return c.getResources().getDisplayMetrics().heightPixels;
    }
}
