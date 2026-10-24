package ru.tihiy.dom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.InputStream;

final class BackgroundController {
    private final Context context;
    private final FrameLayout parent;
    private final ImageView a;
    private final ImageView b;
    private final ScrimView scrim;
    private final AssetManager assets;
    private final boolean light;

    private ImageView active;
    private ImageView inactive;
    private Bitmap activeBitmap;

    private ValueAnimator animator;
    private float progress = 0f;
    private String scene = "mountains";
    private boolean motion = true;
    private boolean enabled = true;
    private boolean paused = false;
    private int blurLevel = 1;
    private int dimLevel = 1;

    BackgroundController(Context context, FrameLayout parent, Palette palette,
                         String scene, boolean motion, boolean enabled, int dimLevel, int blurLevel) {
        this.context = context;
        this.parent = parent;
        this.assets = context.getAssets();
        this.light = palette.light;
        this.scene = scene == null ? "mountains" : scene;
        this.motion = motion;
        this.enabled = enabled;
        this.dimLevel = dimLevel;
        this.blurLevel = blurLevel;

        a = newImage();
        b = newImage();
        parent.addView(a, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        parent.addView(b, 1, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        scrim = new ScrimView(context);
        scrim.setLight(light);
        scrim.setDimLevel(dimLevel);
        parent.addView(scrim, 2, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        active = a;
        inactive = b;
        b.setAlpha(0f);

        parent.addOnLayoutChangeListener((v, l, t, r, bo, ol, ot, or, ob) -> applyTransform());
        parent.post(() -> {
            setScene(this.scene);
            applyEnabled();
            applyTransform();
        });
    }

    private ImageView newImage() {
        ImageView v = new ImageView(context);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        v.setAdjustViewBounds(false);
        v.setAlpha(0f);
        return v;
    }

    void setScene(String id) {
        if (id == null || id.length() == 0) return;
        if (id.equals(scene) && activeBitmap != null) return;
        Bitmap bitmap = decode(id);
        if (bitmap == null) return;
        scene = id;

        if (activeBitmap == null) {
            active.setImageBitmap(bitmap);
            active.setAlpha(1f);
            activeBitmap = bitmap;
            applyTransform();
            if (motion && !paused) startAnimator();
            return;
        }

        final ImageView oldActive = active;
        inactive.animate().cancel();
        oldActive.animate().cancel();

        inactive.setImageBitmap(bitmap);
        inactive.setAlpha(0f);
        inactive.setVisibility(View.VISIBLE);
        inactive.animate().alpha(1f).setDuration(520).start();
        oldActive.animate().alpha(0f).setDuration(520).start();

        active = inactive;
        activeBitmap = bitmap;
        inactive = oldActive;
        applyTransform();
        if (motion && !paused) startAnimator();
    }

    void setCustom() {
        scene = "";
        setScene("custom");
    }

    void setEnabled(boolean value) {
        enabled = value;
        applyEnabled();
    }

    private void applyEnabled() {
        int vis = enabled ? View.VISIBLE : View.GONE;
        a.setVisibility(vis);
        b.setVisibility(vis);
        scrim.setVisibility(vis);
        if (enabled && motion && !paused) resume();
        else pause();
    }

    void setMotion(boolean value) {
        motion = value;
        if (!motion) {
            pause();
            progress = 0f;
            applyTransform();
        } else if (enabled && !paused) {
            resume();
        }
    }

    void setDimLevel(int level) {
        dimLevel = level;
        scrim.setDimLevel(level);
    }

    void setBlurLevel(int value) {
        if (blurLevel == value) return;
        blurLevel = value;
        String current = scene;
        if (current != null && current.length() > 0) {
            scene = "";
            setScene(current);
        }
    }

    void pause() {
        paused = true;
        if (animator != null && animator.isStarted()) animator.pause();
    }

    void resume() {
        paused = false;
        if (!motion || !enabled) return;
        if (animator == null) startAnimator();
        else if (animator.isPaused()) animator.resume();
        else if (!animator.isStarted()) animator.start();
    }

    private void startAnimator() {
        if (paused) return;
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(48000L);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                applyTransform();
            });
        }
        if (!animator.isStarted()) animator.start();
        else if (animator.isPaused()) animator.resume();
    }

    private void applyTransform() {
        if (active == null) return;
        int w = parent.getWidth();
        int h = parent.getHeight();
        if (w <= 0 || h <= 0) return;
        float s = 1.02f + 0.10f * progress;
        active.setPivotX(w / 2f);
        active.setPivotY(h / 2f);
        active.setScaleX(s);
        active.setScaleY(s);
        active.setTranslationX((-0.006f + 0.014f * progress) * w);
        active.setTranslationY((-0.004f + 0.010f * progress) * h);
    }

    private Bitmap decode(String id) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            if ("custom".equals(id)) {
                File file = customFile(context);
                if (!file.exists()) return null;
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                int sample = 1;
                while (opts.outWidth / sample > 1920 || opts.outHeight / sample > 1080) sample *= 2;
                opts.inJustDecodeBounds = false;
                opts.inSampleSize = sample;
                Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                return blurLevel > 0 ? blur(bm, blurLevel) : bm;
            }
            try (InputStream is = assets.open("bg_" + id + ".webp")) {
                Bitmap bm = BitmapFactory.decodeStream(is, null, opts);
                return blurLevel > 0 ? blur(bm, blurLevel) : bm;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap blur(Bitmap src, int level) {
        if (src == null) return null;
        int targetW = Math.min(level >= 2 ? 420 : 720, src.getWidth());
        int targetH = Math.max(1, Math.round(targetW * (float) src.getHeight() / src.getWidth()));
        Bitmap small = Bitmap.createScaledBitmap(src, targetW, targetH, true);
        if (small != src) src.recycle();
        Bitmap out = small.copy(Bitmap.Config.ARGB_8888, true);
        if (out == null) return small;
        if (out != small) small.recycle();

        int w = out.getWidth();
        int h = out.getHeight();
        int[] pixels = new int[w * h];
        out.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] horizontal = new int[w * h];
        int radius = level >= 2 ? 10 : 2;

        int[] pr = new int[w + 1];
        int[] pg = new int[w + 1];
        int[] pb = new int[w + 1];
        for (int y = 0; y < h; y++) {
            pr[0] = pg[0] = pb[0] = 0;
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int p = pixels[row + x];
                pr[x + 1] = pr[x] + ((p >> 16) & 0xFF);
                pg[x + 1] = pg[x] + ((p >> 8) & 0xFF);
                pb[x + 1] = pb[x] + (p & 0xFF);
            }
            for (int x = 0; x < w; x++) {
                int l = Math.max(0, x - radius);
                int r = Math.min(w - 1, x + radius);
                int count = r - l + 1;
                int rr = (pr[r + 1] - pr[l]) / count;
                int gg = (pg[r + 1] - pg[l]) / count;
                int bb = (pb[r + 1] - pb[l]) / count;
                horizontal[row + x] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
        }

        int[] cr = new int[h + 1];
        int[] cg = new int[h + 1];
        int[] cb = new int[h + 1];
        for (int x = 0; x < w; x++) {
            cr[0] = cg[0] = cb[0] = 0;
            for (int y = 0; y < h; y++) {
                int p = horizontal[y * w + x];
                cr[y + 1] = cr[y] + ((p >> 16) & 0xFF);
                cg[y + 1] = cg[y] + ((p >> 8) & 0xFF);
                cb[y + 1] = cb[y] + (p & 0xFF);
            }
            for (int y = 0; y < h; y++) {
                int l = Math.max(0, y - radius);
                int r = Math.min(h - 1, y + radius);
                int count = r - l + 1;
                int rr = (cr[r + 1] - cr[l]) / count;
                int gg = (cg[r + 1] - cg[l]) / count;
                int bb = (cb[r + 1] - cb[l]) / count;
                pixels[y * w + x] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    static File customFile(Context c) {
        return new File(c.getFilesDir(), "custom_bg.jpg");
    }

    void release() {
        if (animator != null) animator.cancel();
        animator = null;
        active.setImageDrawable(null);
        inactive.setImageDrawable(null);
        if (activeBitmap != null) activeBitmap.recycle();
        activeBitmap = null;
    }

    private static final class ScrimView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float dim = 0.30f;
        private boolean light = false;

        ScrimView(Context context) {
            super(context);
        }

        void setDimLevel(int level) {
            if (level == 0) dim = 0.18f;
            else if (level == 2) dim = 0.48f;
            else dim = 0.30f;
            invalidate();
        }

        void setLight(boolean value) {
            light = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;
            int base = light ? 0xFFEEF1F6 : 0xFF000000;
            float alpha = dim + (light ? 0.22f : 0f);
            if (alpha > 0.86f) alpha = 0.86f;

            paint.setShader(null);
            paint.setColor(Ui.alpha(base, alpha));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new LinearGradient(0, 0, 0, h * 0.34f,
                    Ui.alpha(base, light ? 0.45f : 0.55f), 0x00000000, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h * 0.34f, paint);

            paint.setShader(new LinearGradient(0, h * 0.62f, 0, h,
                    0x00000000, Ui.alpha(base, light ? 0.40f : 0.52f), Shader.TileMode.CLAMP));
            canvas.drawRect(0, h * 0.62f, w, h, paint);
        }
    }
}
