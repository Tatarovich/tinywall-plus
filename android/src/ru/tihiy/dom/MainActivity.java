package ru.tihiy.dom;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextClock;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQ_PHOTO = 42;
    private static final int SLOTS = 5;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int[] ACCENTS = {
            0xFFFF9F43, 0xFF4F8DFF, 0xFFFF4D6D, 0xFF3DDC97, 0xFFFFB020, 0xFF4F9CFF
    };

    private SharedPreferences prefs;
    private Palette p;
    private boolean light;
    private FrameLayout root;
    private FrameLayout mainLayer;
    private BackgroundController background;
    private LinearLayout cardsRow;
    private FrameLayout footerRight;
    private TextClock clock;
    private LinearLayout clockBox;
    private TextView dateView;
    private Handler handler;
    private Runnable dateRunnable;

    private List<AppInfo> allApps = new ArrayList<>();
    private final String[] slots = new String[SLOTS];
    private List<SceneInfo> scenes = new ArrayList<>();
    private WeatherService weatherService;
    private TextView weatherIcon;
    private TextView weatherTemp;
    private TextView weatherCity;
    private TextView weatherCond;
    private TextView settingsCityValue;
    private String weatherCityName;
    private double weatherLat = WeatherService.DEFAULT_LAT;
    private double weatherLon = WeatherService.DEFAULT_LON;
    private int weatherTempValue = Integer.MIN_VALUE;
    private int weatherCode = 0;
    private long weatherUpdated = 0L;
    private FrameLayout cityOverlay;
    private EditText cityInput;
    private LinearLayout cityResults;
    private TextView cityStatus;
    private boolean cityOpen;

    private FrameLayout settingsOverlay;
    private LinearLayout settingsContent;
    private ScrollView settingsScroll;
    private final List<List<View>> settingsRows = new ArrayList<>();
    private View bgRow;
    private TextView bgValue;
    private FrameLayout bgPickerOverlay;
    private final List<List<View>> bgPickerRows = new ArrayList<>();
    private final List<BgTile> bgTiles = new ArrayList<>();
    private boolean bgPickerOpen;
    private ToggleView switchBg;
    private ToggleView switchMotion;
    private ToggleView switchClock;
    private Segmented blurSeg;
    private Segmented dimSeg;
    private Segmented themeSeg;
    private Segmented fontSeg;
    private Segmented sizeSeg;
    private TextView tilesValue;
    private View tilesRow;
    private TextView homeButton;

    private FrameLayout appsOverlay;
    private LinearLayout appsGrid;
    private final List<AppTile> appTiles = new ArrayList<>();
    private final List<LinearLayout> slotButtons = new ArrayList<>();
    private final List<TextView> slotNames = new ArrayList<>();
    private TextView pickerCount;
    private TextView appsCount;
    private int pickerSlot = -1;

    private ViewGroup activeFocusRoot;
    private boolean settingsOpen;
    private boolean appsOpen;
    private boolean updatingSwitches;

    private int tileSize;
    private int dimLevel;
    private boolean bgEnabled;
    private boolean motion;
    private boolean clockOn;
    private boolean english;
    private int blurLevel = 1;
    private int fontLevel;
    private String scene;
    private int displayW;
    private int displayH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        int[] display = displaySize();
        displayW = display[0];
        displayH = display[1];
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        english = !"ru".equals(prefs.getString("lang", "en"));
        L.en = english;
        light = prefs.getBoolean("light", false);
        p = Palette.of(light);
        tileSize = prefs.getInt("tile", 1);
        dimLevel = prefs.getInt("dim", 1);
        bgEnabled = prefs.getBoolean("bg_on", true);
        motion = prefs.getBoolean("motion", true);
        clockOn = prefs.getBoolean("clock", true);
        if (prefs.contains("blur_level")) {
            blurLevel = prefs.getInt("blur_level", 1);
        } else {
            blurLevel = prefs.getBoolean("blur", true) ? 1 : 0;
            prefs.edit().putInt("blur_level", blurLevel).apply();
        }
        if (blurLevel < 0 || blurLevel > 2) blurLevel = 1;
        fontLevel = prefs.getInt("font", 1);
        Ui.fontScale = fontLevel == 0 ? 1.22f : fontLevel == 2 ? 1.38f : 1.30f;
        scenes = SceneRepository.load(this);
        scene = prefs.getString("scene", scenes.get(0).id);
        if (!"custom".equals(scene) && !SceneRepository.has(scenes, scene)) {
            scene = scenes.get(0).id;
            prefs.edit().putString("scene", scene).apply();
        }
        if ("custom".equals(scene) && !BackgroundController.customFile(this).exists()) {
            scene = scenes.get(0).id;
            prefs.edit().putString("scene", scene).apply();
        }

        hideSystemUi();

        root = new FrameLayout(this);
        GradientDrawable page = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{p.bgTop, p.bgBottom});
        root.setBackground(page);
        setContentView(root);

        background = new BackgroundController(this, root, p, scene, motion, bgEnabled, dimLevel, blurLevel);
        weatherService = new WeatherService();
        mainLayer = new FrameLayout(this);
        mainLayer.setClipChildren(false);
        mainLayer.setClipToPadding(false);
        root.addView(mainLayer, new FrameLayout.LayoutParams(MATCH, MATCH));

        loadWeatherCache();
        buildMain();
        loadApps();
        buildSettings();
        refreshCards();
        refreshHomeState();
        refreshWeather();
        refreshWeatherIfStale();

        handler = new Handler(Looper.getMainLooper());
        dateRunnable = () -> {
            updateDate();
            refreshWeatherIfStale();
            handler.postDelayed(dateRunnable, 30000L);
        };
        updateDate();
        handler.postDelayed(dateRunnable, 30000L);

        activeFocusRoot = mainLayer;
        mainLayer.post(this::focusFirst);
        mainLayer.postDelayed(this::focusFirstIfNeeded, 180L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (!settingsOpen && !appsOpen) background.resume();
        refreshHomeState();
        refreshWeatherIfStale();
        if (settingsOverlay != null) refreshSettings();
        if (!settingsOpen && !appsOpen) {
            mainLayer.post(this::focusFirst);
            mainLayer.postDelayed(this::focusFirstIfNeeded, 180L);
        }
    }

    @Override
    protected void onPause() {
        background.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (handler != null && dateRunnable != null) handler.removeCallbacks(dateRunnable);
        background.release();
        if (weatherService != null) weatherService.shutdown();
        super.onDestroy();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
            if (getCurrentFocus() == null) {
                focusFirst();
                mainLayer.postDelayed(this::focusFirstIfNeeded, 180L);
            }
        }
    }

    // ------------------------------------------------------------------ main

    private void buildMain() {
        buildHeader();
        buildCardsRow();
        buildFooter();
    }

    private void buildHeader() {
        FrameLayout header = new FrameLayout(this);
        mainLayer.addView(header, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP));

        LinearLayout headerLeft = Ui.row(this);
        headerLeft.setGravity(Gravity.TOP);
        FrameLayout.LayoutParams leftLp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.TOP | Gravity.START);
        leftLp.setMargins(Ui.px(96), Ui.px(52), 0, 0);
        header.addView(headerLeft, leftLp);

        clockBox = Ui.col(this);
        headerLeft.addView(clockBox, Ui.lp(WRAP, WRAP));

        clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.px(92));
        clock.setTextColor(p.text);
        clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        clock.setIncludeFontPadding(false);
        clock.setLetterSpacing(-0.035f);
        clock.setSingleLine(true);
        clockBox.setVisibility(clockOn ? View.VISIBLE : View.GONE);
        clockBox.addView(clock, Ui.lp(WRAP, WRAP));

        dateView = Ui.text(this, "", 22, p.muted, false);
        dateView.setPadding(0, Ui.px(10), 0, 0);
        clockBox.addView(dateView, Ui.lp(WRAP, WRAP));

        LinearLayout weatherBox = Ui.row(this);
        weatherBox.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams weatherLp = Ui.lp(WRAP, WRAP);
        weatherLp.leftMargin = Ui.px(34);
        headerLeft.addView(weatherBox, weatherLp);

        View weatherSep = new View(this);
        weatherSep.setBackgroundColor(p.cardBorder);
        weatherBox.addView(weatherSep, Ui.lp(Ui.px(1), Ui.px(84)));

        LinearLayout weatherContent = Ui.row(this);
        weatherContent.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams contentLp = Ui.lp(WRAP, WRAP);
        contentLp.leftMargin = Ui.px(24);
        weatherBox.addView(weatherContent, contentLp);

        weatherIcon = Ui.text(this, "☁", 34, p.accent, false);
        weatherIcon.setGravity(Gravity.CENTER);
        weatherIcon.setPadding(0, 0, Ui.px(12), 0);
        weatherContent.addView(weatherIcon, Ui.lp(WRAP, WRAP));

        weatherTemp = Ui.text(this, "—°", 42, p.text, true);
        weatherTemp.setSingleLine(true);
        weatherContent.addView(weatherTemp, Ui.lp(WRAP, WRAP));

        LinearLayout weatherMeta = Ui.col(this);
        LinearLayout.LayoutParams metaLp = Ui.lp(WRAP, WRAP);
        metaLp.leftMargin = Ui.px(12);
        weatherContent.addView(weatherMeta, metaLp);
        weatherCity = Ui.text(this, L.s("Moscow", "Москва"), 16, p.text, true);
        weatherCity.setSingleLine(true);
        weatherCity.setEllipsize(TextUtils.TruncateAt.END);
        weatherCity.setMaxWidth(Ui.px(220));
        weatherMeta.addView(weatherCity, Ui.lp(WRAP, WRAP));
        weatherCond = Ui.text(this, L.s("Cloudy", "Облачно"), 13, p.muted, false);
        weatherCond.setSingleLine(true);
        weatherCond.setEllipsize(TextUtils.TruncateAt.END);
        weatherCond.setMaxWidth(Ui.px(220));
        LinearLayout.LayoutParams condLp = Ui.lp(WRAP, WRAP);
        condLp.topMargin = Ui.px(3);
        weatherMeta.addView(weatherCond, condLp);

        LinearLayout topRight = Ui.row(this);
        topRight.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams rightLp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.TOP | Gravity.END);
        rightLp.setMargins(0, Ui.px(58), Ui.px(96), 0);
        header.addView(topRight, rightLp);

        TextView brand = Ui.text(this, "TinyWall+ " + versionName(), 15, p.accent, true);
        brand.setGravity(Gravity.CENTER);
        brand.setPadding(Ui.px(14), Ui.px(10), Ui.px(14), Ui.px(10));
        brand.setBackground(Ui.roundStroke(p.card, 999, p.cardBorder, 1));
        topRight.addView(brand, Ui.lp(WRAP, WRAP));

        LinearLayout status = Ui.row(this);
        status.setBackground(Ui.roundStroke(p.card, 999, p.cardBorder, 1));
        status.setPadding(Ui.px(14), Ui.px(10), Ui.px(17), Ui.px(10));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        statusLp.leftMargin = Ui.px(10);
        topRight.addView(status, statusLp);

        ImageView wifi = new ImageView(this);
        wifi.setImageResource(R.drawable.ic_wifi);
        wifi.setColorFilter(p.muted);
        status.addView(wifi, Ui.lp(Ui.px(19), Ui.px(19)));
        TextView wifiText = Ui.text(this, "Wi-Fi", 16, p.muted, false);
        LinearLayout.LayoutParams wt = Ui.lp(WRAP, WRAP);
        wt.leftMargin = Ui.px(9);
        status.addView(wifiText, wt);
        View sep = new View(this);
        sep.setBackgroundColor(p.cardBorder);
        LinearLayout.LayoutParams sl = Ui.lp(1, Ui.px(16));
        sl.leftMargin = Ui.px(12);
        sl.rightMargin = Ui.px(12);
        status.addView(sep, sl);

        status.addView(Ui.text(this, resolutionLabel(), 16, p.text, true), Ui.lp(WRAP, WRAP));
    }

    private int[] displaySize() {
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm != null) wm.getDefaultDisplay().getRealMetrics(metrics);
        int w = Math.max(metrics.widthPixels, metrics.heightPixels);
        int h = Math.min(metrics.widthPixels, metrics.heightPixels);
        return new int[]{w, h};
    }

    private String resolutionLabel() {
        int[] display = displaySize();
        return display[0] > 0 && display[1] > 0 ? display[0] + "×" + display[1] : "—";
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0.10";
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int[] display = displaySize();
        if (display[0] != displayW || display[1] != displayH) {
            // The whole layout is scaled from the real display size.
            recreate();
        }
    }

    private void buildCardsRow() {
        cardsRow = new LinearLayout(this);
        cardsRow.setOrientation(LinearLayout.HORIZONTAL);
        cardsRow.setGravity(Gravity.CENTER_VERTICAL);
        cardsRow.setClipChildren(false);
        cardsRow.setClipToPadding(false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER);
        mainLayer.addView(cardsRow, lp);
    }

    private void buildFooter() {
        FrameLayout footer = new FrameLayout(this);
        mainLayer.addView(footer, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM));
        footer.setPadding(Ui.px(96), 0, Ui.px(96), Ui.px(46));

        LinearLayout dock = Ui.row(this);
        dock.setBackground(Ui.roundStroke(p.dock, 999, p.dockBorder, 1));
        dock.setPadding(Ui.px(7), Ui.px(7), Ui.px(7), Ui.px(7));
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER);
        footer.addView(dock, dockLp);

        dock.addView(new DockButton(R.drawable.ic_settings, L.s("Settings", "Настройки"), this::openSettings),
                Ui.lp(WRAP, WRAP));
        dock.addView(new DockButton(R.drawable.ic_grid, L.s("All apps", "Все приложения"), () -> openApps(-1)),
                Ui.lp(WRAP, WRAP));
        dock.addView(new DockButton(R.drawable.ic_edit, L.s("Tiles", "Плитки"), () -> openApps(firstEmptySlot())),
                Ui.lp(WRAP, WRAP));

        footerRight = new FrameLayout(this);
        FrameLayout.LayoutParams rightLp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.END | Gravity.CENTER_VERTICAL);
        footer.addView(footerRight, rightLp);
    }

    private void refreshHomeState() {
        if (footerRight == null) return;
        footerRight.removeAllViews();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.END | Gravity.CENTER_VERTICAL);
        if (isDefaultHome()) {
            TextView hint = Ui.text(this, L.s("OK — open", "OK — открыть"), 15, p.muted2, false);
            footerRight.addView(hint, lp);
        } else {
            TextView button = actionButton(L.s("Make default", "Сделать домашним"), true);
            button.setOnClickListener(v -> requestHome());
            footerRight.addView(button, lp);
        }
    }

    // ------------------------------------------------------------------ apps

    private void loadApps() {
        allApps = AppRepository.load(this);
        java.util.Arrays.fill(slots, null);
        String saved = prefs.getString("slots", null);
        if (saved != null) {
            String[] parts = saved.split("\\|", -1);
            for (int i = 0; i < SLOTS && i < parts.length; i++) {
                if (!parts[i].isEmpty()) slots[i] = parts[i];
            }
        } else {
            // 0.6 intentionally starts with five empty positions.
            saveSlots();
        }
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null && findApp(slots[i]) == null) slots[i] = null;
        }
        saveSlots();
    }

    private void saveSlots() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < SLOTS; i++) {
            if (i > 0) b.append('|');
            if (slots[i] != null) b.append(slots[i]);
        }
        prefs.edit().putString("slots", b.toString()).apply();
    }

    private AppInfo findApp(String key) {
        for (AppInfo a : allApps) if (a.key.equals(key)) return a;
        return null;
    }

    private AppInfo slotApp(int index) {
        if (index < 0 || index >= SLOTS || slots[index] == null) return null;
        return findApp(slots[index]);
    }

    private int filledSlots() {
        int n = 0;
        for (String s : slots) if (s != null) n++;
        return n;
    }

    private int firstEmptySlot() {
        for (int i = 0; i < SLOTS; i++) if (slots[i] == null) return i;
        return 0;
    }

    private void refreshCards() {
        if (cardsRow == null) return;
        cardsRow.removeAllViews();
        for (int i = 0; i < SLOTS; i++) {
            AppInfo app = slotApp(i);
            View card = app != null
                    ? new AppCard(app, i, ACCENTS[i % ACCENTS.length])
                    : new AddCard(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth(), cardHeight());
            if (i < SLOTS - 1) lp.rightMargin = Ui.px(26);
            cardsRow.addView(card, lp);
        }
        cardsRow.post(this::focusFirstIfNeeded);
        cardsRow.postDelayed(this::focusFirstIfNeeded, 180L);
    }

    private void focusFirstIfNeeded() {
        if (getCurrentFocus() == null && !settingsOpen && !appsOpen) focusFirst();
    }

    private int cardWidth() {
        return Ui.px(tileSize == 0 ? 250 : tileSize == 2 ? 286 : 280);
    }

    private int cardHeight() {
        return Ui.px(tileSize == 0 ? 180 : tileSize == 2 ? 220 : 205);
    }

    private void launch(AppInfo app) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(app.component);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            toast(L.s("Could not open “" + app.label + "”", "Не удалось открыть «" + app.label + "»"));
        }
    }

    // ------------------------------------------------------------------ settings

    private void buildSettings() {
        settingsOverlay = new FrameLayout(this);
        settingsOverlay.setBackgroundColor(p.overlay);
        settingsOverlay.setClickable(true);
        settingsOverlay.setVisibility(View.GONE);
        root.addView(settingsOverlay, new FrameLayout.LayoutParams(MATCH, MATCH));

        FrameLayout panel = new FrameLayout(this);
        panel.setBackgroundColor(p.panel);
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(Ui.px(760), MATCH, Gravity.END);
        settingsOverlay.addView(panel, panelLp);

        View edge = new View(this);
        edge.setBackgroundColor(p.panelBorder);
        panel.addView(edge, new FrameLayout.LayoutParams(Ui.px(1), MATCH, Gravity.START));

        settingsScroll = new ScrollView(this);
        settingsScroll.setFillViewport(true);
        settingsScroll.setClipToPadding(false);
        settingsScroll.setVerticalScrollBarEnabled(false);
        panel.addView(settingsScroll, new FrameLayout.LayoutParams(MATCH, MATCH));

        settingsContent = Ui.col(this);
        settingsContent.setPadding(Ui.px(46), Ui.px(30), Ui.px(46), Ui.px(24));
        settingsScroll.addView(settingsContent, new ScrollView.LayoutParams(MATCH, WRAP));
        settingsRows.clear();

        LinearLayout head = Ui.row(this);
        LinearLayout titles = Ui.col(this);
        TextView brand = Ui.text(this, "TINYWALL+", 10, p.accent, true);
        brand.setLetterSpacing(0.16f);
        titles.addView(brand, Ui.lp(WRAP, WRAP));
        TextView title = Ui.text(this, L.s("Settings", "Настройки"), 32, p.text, true);
        title.setPadding(0, Ui.px(4), 0, 0);
        titles.addView(title, Ui.lp(WRAP, WRAP));
        head.addView(titles, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView close = actionButton(L.s("Close", "Закрыть"), false);
        close.setOnClickListener(v -> closeSettings());
        head.addView(close, Ui.lp(WRAP, WRAP));
        settingsContent.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));
        settingsRows.add(single(close));

        LinearLayout bgGroup = addGroup(L.s("BACKGROUND", "ФОН"));
        bgRow = rowLink(L.s("Background", "Фон"),
                L.s("11 wallpapers", "11 обоев"), bgValue = Ui.text(this, sceneLabel(), 16, p.accent, true),
                this::openBackgroundPicker);
        addGroupRow(bgGroup, bgRow, true);
        settingsRows.add(single(bgRow));

        switchBg = makeSwitch(bgEnabled, checked -> {
            if (updatingSwitches) return;
            bgEnabled = checked;
            prefs.edit().putBoolean("bg_on", checked).apply();
            background.setEnabled(checked);
        });
        addGroupRow(bgGroup, settingRow(L.s("Show background", "Показывать фон"),
                null, switchBg), false);
        settingsRows.add(single(switchBg));

        switchMotion = makeSwitch(motion, checked -> {
            if (updatingSwitches) return;
            motion = checked;
            prefs.edit().putBoolean("motion", checked).apply();
            background.setMotion(checked);
        });
        addGroupRow(bgGroup, settingRow(L.s("Background motion", "Движение фона"),
                null, switchMotion), false);
        settingsRows.add(single(switchMotion));

        blurSeg = new Segmented(new String[]{
                L.s("Off", "Выкл."), L.s("Soft", "Лёгкое"), L.s("Strong", "Сильное")
        }, blurLevel, index -> {
            if (updatingSwitches) return;
            blurLevel = index;
            prefs.edit().putInt("blur_level", index).apply();
            background.setBlurLevel(index);
        });
        addGroupRow(bgGroup, settingRow(L.s("Background blur", "Размытие фона"),
                null, blurSeg.root), false);
        settingsRows.add(new ArrayList<View>(blurSeg.items));

        dimSeg = new Segmented(new String[]{
                L.s("Soft", "Мягкое"), L.s("Medium", "Среднее"), L.s("Deep", "Глубокое")
        }, dimLevel, index -> {
            if (updatingSwitches) return;
            dimLevel = index;
            prefs.edit().putInt("dim", index).apply();
            background.setDimLevel(index);
        });
        addGroupRow(bgGroup, settingRow(L.s("Dimming", "Затемнение"),
                null, dimSeg.root), false);
        settingsRows.add(new ArrayList<View>(dimSeg.items));

        LinearLayout homeGroup = addGroup(L.s("HOME SCREEN", "ГЛАВНЫЙ ЭКРАН"));
        tilesValue = Ui.text(this, "", 15, p.accent, true);
        tilesRow = rowLink(L.s("Home tiles", "Плитки на главном"),
                L.s("Choose an app for each position", "Приложение на каждую позицию"),
                tilesValue, () -> openApps(firstEmptySlot()));
        addGroupRow(homeGroup, tilesRow, true);
        settingsRows.add(single(tilesRow));

        sizeSeg = new Segmented(new String[]{"S", "M", "L"}, tileSize, index -> {
            if (updatingSwitches) return;
            prefs.edit().putInt("tile", index).commit();
            recreate();
        });
        addGroupRow(homeGroup, settingRow(L.s("Tile size", "Размер плиток"),
                null, sizeSeg.root), false);
        settingsRows.add(new ArrayList<View>(sizeSeg.items));

        switchClock = makeSwitch(clockOn, checked -> {
            if (updatingSwitches) return;
            clockOn = checked;
            prefs.edit().putBoolean("clock", checked).apply();
            clockBox.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        addGroupRow(homeGroup, settingRow(L.s("Clock", "Часы"),
                null, switchClock), false);
        settingsRows.add(single(switchClock));

        LinearLayout appearance = addGroup(L.s("APPEARANCE", "ОФОРМЛЕНИЕ"));
        themeSeg = new Segmented(new String[]{
                L.s("Graphite", "Графит"), L.s("Light", "Светлая")
        }, light ? 1 : 0, index -> {
            if (updatingSwitches) return;
            prefs.edit().putBoolean("light", index == 1).commit();
            recreate();
        });
        addGroupRow(appearance, settingRow(L.s("Theme", "Тема"),
                null, themeSeg.root), true);
        settingsRows.add(new ArrayList<View>(themeSeg.items));

        fontSeg = new Segmented(new String[]{
                L.s("Normal", "Обычный"), L.s("Large", "Крупный"), L.s("Very large", "Очень крупный")
        }, fontLevel, index -> {
            if (updatingSwitches) return;
            prefs.edit().putInt("font", index).commit();
            recreate();
        });
        addGroupRow(appearance, settingRow(L.s("Font size", "Размер шрифта"),
                null, fontSeg.root), false);
        settingsRows.add(new ArrayList<View>(fontSeg.items));

        Segmented language = new Segmented(new String[]{"English", "Русский"}, english ? 0 : 1, index -> {
            if (updatingSwitches) return;
            prefs.edit().putString("lang", index == 0 ? "en" : "ru").commit();
            L.en = index == 0;
            recreate();
        });
        addGroupRow(appearance, settingRow(L.s("Language", "Язык"),
                null, language.root), false);
        settingsRows.add(new ArrayList<View>(language.items));

        LinearLayout weatherGroup = addGroup(L.s("WEATHER", "ПОГОДА"));
        settingsCityValue = Ui.text(this, weatherCityName == null ? "—" : weatherCityName, 15, p.accent, true);
        LinearLayout cityRow = rowLink(L.s("Weather city", "Город погоды"),
                L.s("Search by city name", "Поиск по названию города"),
                settingsCityValue, this::openCitySearch);
        addGroupRow(weatherGroup, cityRow, true);
        settingsRows.add(single(cityRow));

        homeButton = actionButton(L.s("Make default", "Сделать домашним"), true);
        homeButton.setGravity(Gravity.CENTER);
        homeButton.setOnClickListener(v -> requestHome());
        LinearLayout.LayoutParams homeLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        homeLp.topMargin = Ui.px(20);
        settingsContent.addView(homeButton, homeLp);
        TextView hint = Ui.text(this, L.s(
                        "If the system chooser does not appear, set TinyWall+ manually in Android settings.",
                        "Если системный выбор не появится — назначьте TinyWall+ вручную в настройках Android."),
                12, p.muted2, false);
        hint.setPadding(0, Ui.px(8), 0, 0);
        settingsContent.addView(hint, new LinearLayout.LayoutParams(MATCH, WRAP));
        settingsRows.add(single(homeButton));

        TextView version = Ui.text(this, "TinyWall+ · " + versionName() + " · Android 9", 12, p.muted2, false);
        LinearLayout.LayoutParams versionLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        versionLp.topMargin = Ui.px(18);
        settingsContent.addView(version, versionLp);
    }

    private List<View> single(View view) {
        List<View> list = new ArrayList<>();
        list.add(view);
        return list;
    }

    private LinearLayout addGroup(String label) {
        TextView header = Ui.text(this, label, 11, p.muted2, true);
        header.setLetterSpacing(0.16f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        hlp.setMargins(Ui.px(22), Ui.px(18), 0, Ui.px(7));
        settingsContent.addView(header, hlp);
        LinearLayout group = Ui.col(this);
        group.setBackground(Ui.roundStroke(p.card, 16, p.cardBorder, 1));
        settingsContent.addView(group, new LinearLayout.LayoutParams(MATCH, WRAP));
        return group;
    }

    private void addGroupRow(LinearLayout group, View row, boolean firstRow) {
        if (!firstRow) {
            View line = new View(this);
            line.setBackgroundColor(p.panelBorder);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH, 1);
            llp.setMargins(Ui.px(22), 0, Ui.px(22), 0);
            group.addView(line, llp);
        }
        row.setPadding(Ui.px(22), Ui.px(14), Ui.px(22), Ui.px(14));
        group.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private LinearLayout rowLink(String label, String sub, TextView value, Runnable action) {
        LinearLayout row = Ui.row(this);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setClickable(true);
        row.setBackground(Ui.states(Ui.round(0x00000000, 12),
                Ui.round(Ui.alpha(p.accent, 0.13f), 12)));
        LinearLayout labels = Ui.col(this);
        labels.addView(Ui.text(this, label, 19, p.text, true));
        TextView s = Ui.text(this, sub, 13, p.muted, false);
        s.setPadding(0, Ui.px(4), 0, 0);
        s.setMaxWidth(Ui.px(360));
        labels.addView(s);
        row.addView(labels, new LinearLayout.LayoutParams(0, WRAP, 1f));
        if (value != null) {
            value.setSingleLine(true);
            value.setEllipsize(TextUtils.TruncateAt.END);
            value.setMaxWidth(Ui.px(220));
            row.addView(value, Ui.lp(WRAP, WRAP));
        }
        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_arrow);
        chevron.setColorFilter(p.muted);
        LinearLayout.LayoutParams clp = Ui.lp(Ui.px(18), Ui.px(18));
        clp.leftMargin = Ui.px(8);
        row.addView(chevron, clp);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private LinearLayout settingRow(String label, String sub, View control) {
        LinearLayout row = Ui.row(this);
        LinearLayout labels = Ui.col(this);
        labels.addView(Ui.text(this, label, 19, p.text, true));
        if (sub != null && sub.length() > 0) {
            TextView s = Ui.text(this, sub, 13, p.muted, false);
            s.setPadding(0, Ui.px(4), 0, 0);
            s.setMaxWidth(Ui.px(360));
            labels.addView(s);
        }
        row.addView(labels, new LinearLayout.LayoutParams(0, WRAP, 1f));
        if (control != null) {
            LinearLayout.LayoutParams lp = Ui.lp(WRAP, WRAP);
            lp.leftMargin = Ui.px(16);
            row.addView(control, lp);
        }
        return row;
    }

    private String sceneLabel() {
        if ("custom".equals(scene)) return L.s("Custom photo", "Своё фото");
        return SceneRepository.label(scenes, scene);
    }

    private void refreshSettings() {
        if (switchBg == null) return;
        updatingSwitches = true;
        switchBg.setChecked(bgEnabled);
        switchMotion.setChecked(motion);
        switchClock.setChecked(clockOn);
        blurSeg.setSelected(blurLevel);
        dimSeg.setSelected(dimLevel);
        themeSeg.setSelected(light ? 1 : 0);
        fontSeg.setSelected(fontLevel);
        sizeSeg.setSelected(tileSize);
        updatingSwitches = false;

        if (bgValue != null) bgValue.setText(sceneLabel());
        if (tilesValue != null) tilesValue.setText(filledSlots() + L.s(" of 5", " из 5"));
        if (settingsCityValue != null) settingsCityValue.setText(weatherCityName == null ? "—" : weatherCityName);
        if (homeButton != null) {
            if (isDefaultHome()) {
                homeButton.setText(L.s("Already default", "Уже домашний"));
                homeButton.setEnabled(false);
                homeButton.setAlpha(0.55f);
            } else {
                homeButton.setText(L.s("Make default", "Сделать домашним"));
                homeButton.setEnabled(true);
                homeButton.setAlpha(1f);
            }
        }
        refreshBgTiles();
    }

    private void openSettings() {
        if (appsOpen) closeApps();
        if (settingsOpen) return;
        settingsOpen = true;
        settingsOverlay.setVisibility(View.VISIBLE);
        background.pause();
        refreshSettings();
        activeFocusRoot = settingsOverlay;
        settingsOverlay.post(() -> {
            if (bgRow != null) bgRow.requestFocus();
        });
    }

    private void closeSettings() {
        if (cityOpen) closeCitySearch();
        if (bgPickerOpen) closeBackgroundPicker();
        if (!settingsOpen) return;
        settingsOpen = false;
        settingsOverlay.setVisibility(View.GONE);
        background.resume();
        activeFocusRoot = mainLayer;
        mainLayer.post(this::focusFirst);
        mainLayer.postDelayed(this::focusFirstIfNeeded, 180L);
    }

    private void openBackgroundPicker() {
        if (bgPickerOpen) return;
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(p.overlay);
        overlay.setClickable(true);
        bgPickerOverlay = overlay;

        LinearLayout panel = Ui.col(this);
        panel.setBackground(Ui.round(p.panel, 34));
        panel.setPadding(Ui.px(46), Ui.px(38), Ui.px(46), Ui.px(32));
        int count = scenes.size() + 1;
        int rows = Math.max(1, (count + 2) / 3);
        int panelHeight = Math.min(Ui.px(960), Math.max(Ui.px(560), Ui.px(280 + rows * 250)));
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(Ui.px(1100), panelHeight, Gravity.CENTER);
        overlay.addView(panel, panelLp);

        LinearLayout head = Ui.row(this);
        LinearLayout headText = Ui.col(this);
        headText.addView(Ui.text(this, L.s("Background", "Фон"), 34, p.text, true));
        TextView headSub = Ui.text(this, L.s("Choose a scene or your own photo", "Выберите сцену или своё фото"), 17, p.muted, false);
        headSub.setPadding(0, Ui.px(4), 0, 0);
        headText.addView(headSub);
        head.addView(headText, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView close = actionButton(L.s("Back", "Назад"), false);
        close.setOnClickListener(v -> closeBackgroundPicker());
        head.addView(close, Ui.lp(WRAP, WRAP));
        panel.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        scrollLp.topMargin = Ui.px(22);
        panel.addView(scroll, scrollLp);

        LinearLayout grid = Ui.col(this);
        scroll.addView(grid, new ScrollView.LayoutParams(MATCH, WRAP));

        bgPickerRows.clear();
        bgPickerRows.add(single(close));
        bgTiles.clear();

        List<BgTile> all = new ArrayList<>();
        for (SceneInfo info : scenes) all.add(new BgTile(info.id, info.label(), false));
        all.add(new BgTile("custom", L.s("Custom photo", "Своё фото"), true));

        for (int start = 0; start < all.size(); start += 3) {
            LinearLayout row = Ui.row(this);
            List<View> rowViews = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                int index = start + j;
                if (index < all.size()) {
                    BgTile tile = all.get(index);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
                    lp.setMargins(Ui.px(8), Ui.px(8), Ui.px(8), Ui.px(8));
                    row.addView(tile.root, lp);
                    rowViews.add(tile.root);
                    bgTiles.add(tile);
                } else {
                    Space space = new Space(this);
                    row.addView(space, new LinearLayout.LayoutParams(0, Ui.px(10), 1f));
                }
            }
            grid.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
            bgPickerRows.add(rowViews);
        }

        LinearLayout foot = Ui.row(this);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        footLp.topMargin = Ui.px(16);
        panel.addView(foot, footLp);
        foot.addView(Ui.text(this, L.s("OK — select · Back", "OK — выбрать · Назад"), 15, p.muted2, false),
                new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView done = actionButton(L.s("Done", "Готово"), true);
        done.setOnClickListener(v -> closeBackgroundPicker());
        foot.addView(done, Ui.lp(WRAP, WRAP));
        bgPickerRows.add(single(done));

        root.addView(overlay, new FrameLayout.LayoutParams(MATCH, MATCH));
        bgPickerOpen = true;
        activeFocusRoot = overlay;
        refreshBgTiles();

        BgTile target = null;
        for (BgTile t : bgTiles) if (t.id.equals(scene)) target = t;
        if (target == null && !bgTiles.isEmpty()) target = bgTiles.get(0);
        final BgTile focusTarget = target;
        overlay.post(() -> {
            if (focusTarget != null) focusTarget.root.requestFocus();
        });
    }

    private void closeBackgroundPicker() {
        if (!bgPickerOpen) return;
        bgPickerOpen = false;
        if (bgPickerOverlay != null) root.removeView(bgPickerOverlay);
        bgPickerOverlay = null;
        activeFocusRoot = settingsOverlay;
        settingsOverlay.post(() -> {
            if (bgRow != null) bgRow.requestFocus();
        });
    }

    private void refreshBgTiles() {
        for (BgTile t : bgTiles) t.refresh();
    }

    private final class BgTile {
        final String id;
        final String title;
        final boolean custom;
        final LinearLayout root;
        final FrameLayout imageBox;
        final ImageView image;
        final ImageView check;
        final TextView label;

        BgTile(String id, String title, boolean custom) {
            this.id = id;
            this.title = title;
            this.custom = custom;
            root = Ui.col(MainActivity.this);
            root.setFocusable(true);
            root.setFocusableInTouchMode(true);
            root.setClickable(true);
            root.setPadding(Ui.px(4), Ui.px(4), Ui.px(4), Ui.px(6));

            imageBox = new FrameLayout(MainActivity.this);
            imageBox.setBackground(Ui.round(p.card, 16));
            imageBox.setClipToOutline(true);
            root.addView(imageBox, new LinearLayout.LayoutParams(MATCH, Ui.px(168)));

            image = new ImageView(MainActivity.this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageBox.addView(image, new FrameLayout.LayoutParams(MATCH, MATCH));

            check = new ImageView(MainActivity.this);
            check.setImageResource(R.drawable.ic_check);
            check.setColorFilter(0xFFFFFFFF);
            check.setPadding(Ui.px(7), Ui.px(7), Ui.px(7), Ui.px(7));
            check.setBackground(Ui.round(p.accent, 999));
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.px(32), Ui.px(32),
                    Gravity.TOP | Gravity.END);
            clp.setMargins(0, Ui.px(8), Ui.px(8), 0);
            imageBox.addView(check, clp);

            label = Ui.text(MainActivity.this, title, 19, p.muted, false);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH, WRAP);
            llp.topMargin = Ui.px(7);
            root.addView(label, llp);

            root.setOnClickListener(v -> choose());
            root.setOnFocusChangeListener((v, hasFocus) -> refresh());
            loadImage();
            refresh();
        }

        void choose() {
            if (custom && !BackgroundController.customFile(MainActivity.this).exists()) {
                pickPhoto();
                return;
            }
            scene = id;
            prefs.edit().putString("scene", id).apply();
            if (custom) background.setCustom();
            else background.setScene(id);
            if (bgValue != null) bgValue.setText(sceneLabel());
            refreshBgTiles();
        }

        void loadImage() {
            if (custom) {
                File file = BackgroundController.customFile(MainActivity.this);
                if (!file.exists()) {
                    image.setImageResource(R.drawable.ic_plus);
                    image.setColorFilter(p.muted);
                    image.setPadding(Ui.px(58), Ui.px(58), Ui.px(58), Ui.px(58));
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    return;
                }
                image.setColorFilter(null);
                image.setPadding(0, 0, 0, 0);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 4;
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath(), o);
                if (bm != null) image.setImageBitmap(bm);
                return;
            }
            try (InputStream is = getAssets().open("bg_" + id + ".webp")) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 2;
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bm = BitmapFactory.decodeStream(is, null, o);
                if (bm != null) image.setImageBitmap(bm);
            } catch (Exception ignored) {
            }
        }

        void refresh() {
            boolean selected = id.equals(scene);
            boolean focus = root.hasFocus();
            int border = focus ? p.focus : (selected ? p.accent : p.cardBorder);
            int width = focus || selected ? 3 : 1;
            root.setBackground(Ui.roundStroke(selected ? Ui.alpha(p.accent, 0.10f) : p.card,
                    20, border, width));
            check.setVisibility(selected ? View.VISIBLE : View.GONE);
            label.setTextColor(selected ? p.text : p.muted);
            label.setTypeface(Typeface.create(selected ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        }
    }

    // ------------------------------------------------------------------ apps overlay

    private void openApps(int slot) {
        if (appsOpen) closeApps();
        pickerSlot = slot;
        appsOpen = true;
        background.pause();
        buildAppsOverlay();
        activeFocusRoot = (ViewGroup) appsOverlay;
        if (!appTiles.isEmpty()) appTiles.get(0).root.requestFocus();
        else if (!slotButtons.isEmpty()) slotButtons.get(0).requestFocus();
    }

    private void buildAppsOverlay() {
        allApps = AppRepository.load(this);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(p.overlay);
        overlay.setClickable(true);
        appsOverlay = overlay;

        boolean picker = pickerSlot >= 0;
        LinearLayout panel = Ui.col(this);
        panel.setBackground(Ui.round(p.panel, 34));
        panel.setPadding(Ui.px(42), Ui.px(32), Ui.px(42), Ui.px(26));
        int rows = Math.max(1, (allApps.size() + 5) / 6);
        int panelHeight = Math.min(Ui.px(940), Math.max(Ui.px(picker ? 700 : 470),
                Ui.px(220 + (picker ? 96 : 0) + rows * 146)));
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(Ui.px(1360), panelHeight, Gravity.CENTER);
        overlay.addView(panel, panelLp);

        LinearLayout head = Ui.row(this);
        LinearLayout headText = Ui.col(this);
        String titleText = picker ? L.s("Home tiles", "Плитки на главном")
                : L.s("All apps", "Все приложения");
        headText.addView(Ui.text(this, titleText, 30, p.text, true), Ui.lp(WRAP, WRAP));
        if (picker) {
            TextView sub = Ui.text(this, L.s("Choose an app for the selected position.",
                    "Выберите приложение для выбранной позиции."), 14, p.muted, false);
            sub.setPadding(0, Ui.px(5), 0, 0);
            headText.addView(sub, Ui.lp(WRAP, WRAP));
        }
        head.addView(headText, new LinearLayout.LayoutParams(0, WRAP, 1f));
        appsCount = Ui.text(this, "", 15, p.muted, false);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(WRAP, WRAP);
        cl.rightMargin = Ui.px(18);
        head.addView(appsCount, cl);
        TextView close = actionButton(L.s("Back", "Назад"), false);
        close.setOnClickListener(v -> closeApps());
        head.addView(close, Ui.lp(WRAP, WRAP));
        panel.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        if (picker) {
            LinearLayout slotRow = Ui.row(this);
            LinearLayout.LayoutParams slotRowLp = new LinearLayout.LayoutParams(MATCH, WRAP);
            slotRowLp.topMargin = Ui.px(18);
            panel.addView(slotRow, slotRowLp);
            slotButtons.clear();
            slotNames.clear();
            for (int i = 0; i < SLOTS; i++) {
                final int index = i;
                LinearLayout box = Ui.col(this);
                box.setGravity(Gravity.CENTER);
                box.setFocusable(true);
                box.setFocusableInTouchMode(true);
                box.setClickable(true);
                box.setPadding(Ui.px(8), Ui.px(8), Ui.px(8), Ui.px(8));
                box.setOnClickListener(v -> {
                    pickerSlot = index;
                    refreshAppsOverlay();
                });
                TextView no = Ui.text(this, String.format(Locale.US, "%02d", i + 1),
                        11, p.muted2, false);
                no.setGravity(Gravity.CENTER);
                box.addView(no, Ui.lp(MATCH, WRAP));
                TextView name = Ui.text(this, "", 13, p.muted, false);
                name.setGravity(Gravity.CENTER);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams nlp = Ui.lp(MATCH, WRAP);
                nlp.topMargin = Ui.px(3);
                box.addView(name, nlp);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
                lp.setMargins(Ui.px(4), 0, Ui.px(4), 0);
                slotRow.addView(box, lp);
                slotButtons.add(box);
                slotNames.add(name);
            }
            slotRow.setVisibility(View.VISIBLE);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        scrollLp.topMargin = Ui.px(18);
        panel.addView(scroll, scrollLp);

        appsGrid = Ui.col(this);
        scroll.addView(appsGrid, new ScrollView.LayoutParams(MATCH, WRAP));

        appTiles.clear();
        for (int start = 0; start < allApps.size(); start += 6) {
            LinearLayout row = Ui.row(this);
            for (int j = 0; j < 6; j++) {
                int index = start + j;
                if (index < allApps.size()) {
                    AppTile tile = new AppTile(allApps.get(index));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.px(132), 1f);
                    lp.setMargins(Ui.px(5), Ui.px(5), Ui.px(5), Ui.px(5));
                    row.addView(tile.root, lp);
                    appTiles.add(tile);
                } else {
                    Space space = new Space(this);
                    row.addView(space, new LinearLayout.LayoutParams(0, Ui.px(132), 1f));
                }
            }
            appsGrid.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        LinearLayout foot = Ui.row(this);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        footLp.topMargin = Ui.px(16);
        panel.addView(foot, footLp);
        pickerCount = Ui.text(this, "", 14, p.muted, false);
        foot.addView(pickerCount, new LinearLayout.LayoutParams(0, WRAP, 1f));
        if (picker) {
            TextView clear = actionButton(L.s("Clear position", "Очистить позицию"), false);
            clear.setOnClickListener(v -> {
                if (pickerSlot >= 0) {
                    slots[pickerSlot] = null;
                    saveSlots();
                    refreshCards();
                    refreshAppsOverlay();
                }
            });
            LinearLayout.LayoutParams clp = Ui.lp(WRAP, WRAP);
            clp.rightMargin = Ui.px(10);
            foot.addView(clear, clp);
            TextView done = actionButton(L.s("Done", "Готово"), true);
            done.setOnClickListener(v -> closeApps());
            foot.addView(done, Ui.lp(WRAP, WRAP));
        } else {
            foot.addView(Ui.text(this, L.s("OK — open", "OK — открыть"), 14, p.muted2, false),
                    Ui.lp(WRAP, WRAP));
        }

        refreshAppsOverlay();
        root.addView(overlay, new FrameLayout.LayoutParams(MATCH, MATCH));
    }

    private void refreshAppsOverlay() {
        if (appsCount == null) return;
        appsCount.setText(allApps.size() + L.s(" apps", " приложений"));
        for (AppTile t : appTiles) t.refresh();
        if (pickerCount != null) {
            pickerCount.setText(filledSlots() + L.s(" of 5 filled", " из 5 заполнено"));
        }
        for (int i = 0; i < slotButtons.size(); i++) {
            LinearLayout b = slotButtons.get(i);
            boolean selected = i == pickerSlot;
            b.setBackground(Ui.states(
                    Ui.round(selected ? Ui.alpha(p.accent, 0.18f) : p.card, 12),
                    Ui.roundStroke(selected ? Ui.alpha(p.accent, 0.18f) : p.card, 12, p.focus, 2)));
            if (i < slotNames.size()) {
                AppInfo a = slotApp(i);
                slotNames.get(i).setText(a == null ? L.s("Empty", "Пусто") : a.label);
                slotNames.get(i).setTextColor(selected ? p.text : p.muted);
            }
        }
    }

    private void closeApps() {
        if (!appsOpen) return;
        appsOpen = false;
        if (appsOverlay != null) root.removeView(appsOverlay);
        appsOverlay = null;
        saveSlots();
        refreshCards();
        if (settingsOpen) {
            activeFocusRoot = (ViewGroup) settingsOverlay;
            refreshSettings();
            if (tilesRow != null) tilesRow.requestFocus();
        } else {
            background.resume();
            activeFocusRoot = mainLayer;
            mainLayer.post(this::focusFirst);
        }
        pickerSlot = -1;
    }

    // ------------------------------------------------------------------ weather

    private void loadWeatherCache() {
        weatherCityName = prefs.getString("weather_city", null);
        if (weatherCityName == null || weatherCityName.isEmpty()) {
            weatherCityName = L.s("Moscow", "Москва");
            weatherLat = WeatherService.DEFAULT_LAT;
            weatherLon = WeatherService.DEFAULT_LON;
            prefs.edit().putString("weather_city", weatherCityName)
                    .putString("weather_lat", String.valueOf(weatherLat))
                    .putString("weather_lon", String.valueOf(weatherLon)).apply();
        } else {
            weatherLat = parseDouble(prefs.getString("weather_lat", null), WeatherService.DEFAULT_LAT);
            weatherLon = parseDouble(prefs.getString("weather_lon", null), WeatherService.DEFAULT_LON);
            if ("Moscow".equals(weatherCityName) || "Москва".equals(weatherCityName)) {
                weatherCityName = L.s("Moscow", "Москва");
                prefs.edit().putString("weather_city", weatherCityName).apply();
            }
        }
        weatherTempValue = prefs.getInt("weather_temp", Integer.MIN_VALUE);
        weatherCode = prefs.getInt("weather_code", 0);
        weatherUpdated = prefs.getLong("weather_updated", 0L);
    }

    private double parseDouble(String value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void refreshWeather() {
        if (weatherCity != null) weatherCity.setText(weatherCityName == null
                ? L.s("City", "Город") : weatherCityName);
        boolean has = weatherTempValue != Integer.MIN_VALUE;
        if (weatherTemp != null) weatherTemp.setText(has ? weatherTempValue + "°" : "—°");
        if (weatherCond != null) weatherCond.setText(has
                ? WeatherService.condition(weatherCode, english) : L.s("No data", "Нет данных"));
        if (weatherIcon != null) weatherIcon.setText(has ? WeatherService.icon(weatherCode) : "☁");
    }

    private void refreshWeatherIfStale() {
        if (weatherCityName == null) return;
        long now = System.currentTimeMillis();
        if (weatherTempValue != Integer.MIN_VALUE && now - weatherUpdated < 30 * 60 * 1000L) return;
        weatherService.fetch(weatherLat, weatherLon, english, (current, error) -> {
            if (current != null) {
                weatherTempValue = current.temp;
                weatherCode = current.code;
                weatherUpdated = System.currentTimeMillis();
                prefs.edit().putInt("weather_temp", weatherTempValue)
                        .putInt("weather_code", weatherCode)
                        .putLong("weather_updated", weatherUpdated).apply();
                refreshWeather();
            }
        });
    }

    private void buildCityOverlay() {
        if (cityOverlay != null) return;
        cityOverlay = new FrameLayout(this);
        cityOverlay.setBackgroundColor(p.overlay);
        cityOverlay.setClickable(true);
        cityOverlay.setVisibility(View.GONE);
        root.addView(cityOverlay, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout panel = Ui.col(this);
        panel.setBackground(Ui.round(p.panel, 28));
        panel.setPadding(Ui.px(38), Ui.px(32), Ui.px(38), Ui.px(28));
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(Ui.px(780), WRAP, Gravity.CENTER);
        cityOverlay.addView(panel, panelLp);

        LinearLayout head = Ui.row(this);
        LinearLayout titles = Ui.col(this);
        titles.addView(Ui.text(this, L.s("Weather city", "Город погоды"), 30, p.text, true), Ui.lp(WRAP, WRAP));
        TextView sub = Ui.text(this, L.s("Search by city name.", "Поиск по названию города."), 14, p.muted, false);
        sub.setPadding(0, Ui.px(5), 0, 0);
        titles.addView(sub, Ui.lp(WRAP, WRAP));
        head.addView(titles, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView close = actionButton(L.s("Back", "Назад"), false);
        close.setOnClickListener(v -> closeCitySearch());
        head.addView(close, Ui.lp(WRAP, WRAP));
        panel.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout searchRow = Ui.row(this);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        searchLp.topMargin = Ui.px(22);
        panel.addView(searchRow, searchLp);
        cityInput = new EditText(this);
        cityInput.setSingleLine(true);
        cityInput.setTextColor(p.text);
        cityInput.setHintTextColor(p.muted2);
        cityInput.setHint(L.s("City name", "Название города"));
        cityInput.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.px(18));
        cityInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        cityInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        cityInput.setBackground(Ui.roundStroke(p.card, 12, p.cardBorder, 1));
        cityInput.setPadding(Ui.px(16), Ui.px(11), Ui.px(16), Ui.px(11));
        searchRow.addView(cityInput, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView search = actionButton(L.s("Search", "Найти"), true);
        LinearLayout.LayoutParams searchBtn = Ui.lp(WRAP, WRAP);
        searchBtn.leftMargin = Ui.px(10);
        searchRow.addView(search, searchBtn);
        search.setOnClickListener(v -> searchCity());
        cityInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchCity();
                return true;
            }
            return false;
        });

        cityStatus = Ui.text(this, L.s("Start typing a city name.", "Начните вводить название города."),
                13, p.muted, false);
        cityStatus.setPadding(0, Ui.px(10), 0, 0);
        panel.addView(cityStatus, new LinearLayout.LayoutParams(MATCH, WRAP));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(MATCH, Ui.px(330));
        scrollLp.topMargin = Ui.px(10);
        panel.addView(scroll, scrollLp);
        cityResults = Ui.col(this);
        scroll.addView(cityResults, new ScrollView.LayoutParams(MATCH, WRAP));
    }

    private void openCitySearch() {
        if (cityOpen) return;
        buildCityOverlay();
        cityOpen = true;
        cityInput.setText("");
        cityResults.removeAllViews();
        cityStatus.setText(L.s("Start typing a city name.", "Начните вводить название города."));
        cityOverlay.setVisibility(View.VISIBLE);
        activeFocusRoot = (ViewGroup) cityOverlay;
        cityInput.requestFocus();
        cityInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(cityInput, InputMethodManager.SHOW_IMPLICIT);
        }, 120);
    }

    private void closeCitySearch() {
        if (!cityOpen) return;
        cityOpen = false;
        cityOverlay.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(cityInput.getWindowToken(), 0);
        activeFocusRoot = settingsOpen ? (ViewGroup) settingsOverlay : mainLayer;
        if (settingsOpen && bgRow != null) bgRow.requestFocus();
        else mainLayer.post(this::focusFirst);
    }

    private void searchCity() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(cityInput.getWindowToken(), 0);
        String query = cityInput.getText().toString().trim();
        if (query.isEmpty()) {
            cityStatus.setText(L.s("Type a city name.", "Введите название города."));
            return;
        }
        cityStatus.setText(L.s("Searching…", "Ищем…"));
        cityResults.removeAllViews();
        weatherService.search(query, english, (places, error) -> {
            if (!cityOpen) return;
            cityResults.removeAllViews();
            if (error != null) {
                cityStatus.setText(error);
                return;
            }
            cityStatus.setText(places.size() + L.s(" results", " результатов"));
            for (WeatherService.Place place : places) {
                LinearLayout row = Ui.col(this);
                row.setFocusable(true);
                row.setFocusableInTouchMode(true);
                row.setClickable(true);
                row.setPadding(Ui.px(16), Ui.px(12), Ui.px(16), Ui.px(12));
                row.setBackground(Ui.states(Ui.round(p.card, 12),
                        Ui.roundStroke(p.card, 12, p.focus, 2)));
                row.addView(Ui.text(this, place.name, 18, p.text, true), Ui.lp(WRAP, WRAP));
                if (place.region != null && !place.region.isEmpty()) {
                    TextView r = Ui.text(this, place.region, 12, p.muted, false);
                    r.setPadding(0, Ui.px(3), 0, 0);
                    row.addView(r, Ui.lp(WRAP, WRAP));
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
                lp.topMargin = Ui.px(6);
                cityResults.addView(row, lp);
                row.setOnClickListener(v -> selectCity(place));
            }
        });
    }

    private void selectCity(WeatherService.Place place) {
        weatherCityName = place.name;
        weatherLat = place.lat;
        weatherLon = place.lon;
        weatherTempValue = Integer.MIN_VALUE;
        weatherCode = 0;
        weatherUpdated = 0L;
        prefs.edit().putString("weather_city", weatherCityName)
                .putString("weather_lat", String.valueOf(weatherLat))
                .putString("weather_lon", String.valueOf(weatherLon))
                .putInt("weather_temp", Integer.MIN_VALUE)
                .putInt("weather_code", 0)
                .putLong("weather_updated", 0L).apply();
        refreshWeather();
        refreshSettings();
        closeCitySearch();
        refreshWeatherIfStale();
    }

    private void requestHome() {
        try {
            Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent("android.settings.HOME_SETTINGS"));
            } catch (Exception e2) {
                toast(L.s("Open Settings → Apps → Default apps → Home", "Откройте Настройки → Приложения → По умолчанию → Домой"));
            }
        } catch (Exception e) {
            toast(L.s("Could not open the home app chooser", "Не удалось открыть выбор домашнего приложения"));
        }
    }

    private boolean isDefaultHome() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null && ri.activityInfo != null
                    && getPackageName().equals(ri.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ focus

    private void focusFirst() {
        if (activeFocusRoot == null) return;
        List<View> views = new ArrayList<>();
        collectFocusables(activeFocusRoot, views);
        if (!views.isEmpty()) views.get(0).requestFocus();
    }

    private void collectFocusables(View v, List<View> out) {
        if (v == null || v.getVisibility() != View.VISIBLE || !v.isShown()) return;
        if (v != activeFocusRoot && v.isFocusable() && v.isEnabled() && v.getWidth() > 0 && v.getHeight() > 0) {
            out.add(v);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collectFocusables(g.getChildAt(i), out);
        }
    }

    private boolean moveFocus(int dx, int dy) {
        View focused = getCurrentFocus();
        if (focused == null) {
            focusFirst();
            return true;
        }
        List<View> views = new ArrayList<>();
        collectFocusables(activeFocusRoot, views);
        int[] loc = new int[2];
        int pad = Ui.px(24);
        focused.getLocationOnScreen(loc);
        float cx = loc[0] + focused.getWidth() / 2f;
        float cy = loc[1] + focused.getHeight() / 2f;

        View best = null;
        double bestScore = Double.MAX_VALUE;
        for (View v : views) {
            if (v == focused) continue;
            v.getLocationOnScreen(loc);
            float x = loc[0] + v.getWidth() / 2f;
            float y = loc[1] + v.getHeight() / 2f;
            float ddx = x - cx;
            float ddy = y - cy;
            float primary;
            float secondary;
            if (dx > 0) {
                if (ddx <= pad || ddx < Math.abs(ddy) * 0.25f) continue;
                primary = ddx; secondary = Math.abs(ddy);
            } else if (dx < 0) {
                if (ddx >= -pad || -ddx < Math.abs(ddy) * 0.25f) continue;
                primary = -ddx; secondary = Math.abs(ddy);
            } else if (dy > 0) {
                if (ddy <= pad || ddy < Math.abs(ddx) * 0.25f) continue;
                primary = ddy; secondary = Math.abs(ddx);
            } else {
                if (ddy >= -pad || -ddy < Math.abs(ddx) * 0.25f) continue;
                primary = -ddy; secondary = Math.abs(ddx);
            }
            double score = primary + secondary * 2.1;
            if (score < bestScore) {
                bestScore = score;
                best = v;
            }
        }
        if (best != null) {
            best.requestFocus();
            best.requestRectangleOnScreen(new Rect(0, 0, best.getWidth(), best.getHeight()), true);
            return true;
        }
        return false;
    }

    private boolean moveInActiveRows(int dx, int dy) {
        if (cityOpen) return moveFocus(dx, dy);
        if (bgPickerOpen) return moveRows(bgPickerRows, dx, dy);
        if (appsOpen) return moveFocus(dx, dy);
        if (settingsOpen) return moveRows(settingsRows, dx, dy);
        return moveFocus(dx, dy);
    }

    private boolean moveRows(List<List<View>> rows, int dx, int dy) {
        View focused = getCurrentFocus();
        int rowIndex = -1;
        int colIndex = -1;
        for (int r = 0; r < rows.size(); r++) {
            int c = rows.get(r).indexOf(focused);
            if (c >= 0) {
                rowIndex = r;
                colIndex = c;
                break;
            }
        }
        if (rowIndex < 0) {
            focusFirstInRows(rows);
            return true;
        }
        if (dy != 0) {
            for (int r = rowIndex + dy; r >= 0 && r < rows.size(); r += dy) {
                View target = firstFocusable(rows.get(r));
                if (target != null) {
                    focusRowView(target);
                    return true;
                }
            }
            return false;
        }
        if (dx != 0) {
            List<View> row = rows.get(rowIndex);
            for (int c = colIndex + dx; c >= 0 && c < row.size(); c += dx) {
                View target = row.get(c);
                if (isRowFocusable(target)) {
                    focusRowView(target);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void focusFirstInRows(List<List<View>> rows) {
        for (List<View> row : rows) {
            View target = firstFocusable(row);
            if (target != null) {
                focusRowView(target);
                return;
            }
        }
    }

    private View firstFocusable(List<View> row) {
        for (View v : row) if (isRowFocusable(v)) return v;
        return null;
    }

    private boolean isRowFocusable(View v) {
        return v != null && v.getVisibility() == View.VISIBLE && v.isShown()
                && v.isFocusable() && v.isEnabled();
    }

    private void focusRowView(View v) {
        v.requestFocus();
        v.requestRectangleOnScreen(new Rect(0, 0, v.getWidth(), v.getHeight()), true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return moveInActiveRows(-1, 0) || true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return moveInActiveRows(1, 0) || true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return moveInActiveRows(0, -1) || true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return moveInActiveRows(0, 1) || true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    View focused = getCurrentFocus();
                    if (focused != null && focused.isClickable()) {
                        focused.performClick();
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_BACK:
                    if (cityOpen) { closeCitySearch(); return true; }
                    if (bgPickerOpen) { closeBackgroundPicker(); return true; }
                    if (appsOpen) { closeApps(); return true; }
                    if (settingsOpen) { closeSettings(); return true; }
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    openSettings();
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (cityOpen) closeCitySearch();
        else if (bgPickerOpen) closeBackgroundPicker();
        else if (appsOpen) closeApps();
        else if (settingsOpen) closeSettings();
        // On the home screen Back does nothing: this is a launcher.
    }

    private void updateDate() {
        if (dateView == null) return;
        Locale locale = english ? Locale.ENGLISH : new Locale("ru", "RU");
        String value = new SimpleDateFormat("EEEE, d MMMM", locale).format(new Date());
        if (value.length() > 0) value = Character.toUpperCase(value.charAt(0)) + value.substring(1);
        dateView.setText(value);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ photo

    private void pickPhoto() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_PHOTO);
        } catch (Exception e) {
            toast(L.s("File picker not found", "Не найден системный выбор файлов"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PHOTO || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        File out = BackgroundController.customFile(this);
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IllegalArgumentException("empty");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) os.write(buffer, 0, n);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap check = BitmapFactory.decodeFile(out.getAbsolutePath(), options);
            if (check == null) {
                toast(L.s("Could not read the image", "Не удалось прочитать изображение"));
                return;
            }
            check.recycle();
            scene = "custom";
            prefs.edit().putString("scene", "custom").apply();
            background.setCustom();
            if (bgPickerOpen) {
                for (BgTile t : bgTiles) t.loadImage();
            }
            refreshSettings();
            toast(L.s("Background updated", "Фон обновлён"));
        } catch (Exception e) {
            toast(L.s("Could not save the image", "Не удалось сохранить изображение"));
        }
    }

    // ------------------------------------------------------------------ widgets

    private ToggleView makeSwitch(boolean checked, SwitchListener listener) {
        ToggleView s = new ToggleView(this, p, checked);
        s.setOnCheckedChangeListener(listener::onChecked);
        return s;
    }

    private TextView actionButton(String text, boolean primary) {
        TextView t = Ui.text(this, text, 17, primary ? (light ? 0xFFFFFFFF : 0xFF08101C) : p.text, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.px(22), Ui.px(13), Ui.px(22), Ui.px(13));
        t.setFocusable(true);
        t.setFocusableInTouchMode(true);
        t.setClickable(true);
        GradientDrawable normal = Ui.round(primary ? p.accent : p.card, 12);
        GradientDrawable focused = Ui.roundStroke(primary ? p.accent : p.card, 12, p.focus, 2);
        if (!primary) normal.setStroke(Math.max(1, Ui.px(1)), p.cardBorder);
        t.setBackground(Ui.states(normal, focused));
        return t;
    }

    private interface SwitchListener {
        void onChecked(boolean checked);
    }

    private interface SegmentListener {
        void onSelected(int index);
    }

    private final class Segmented {
        final LinearLayout root;
        final List<TextView> items = new ArrayList<>();
        int selected;

        Segmented(String[] labels, int selected, SegmentListener listener) {
            this.selected = selected;
            root = Ui.row(ActivityCompat());
            root.setBackground(Ui.roundStroke(p.card, 12, p.cardBorder, 1));
            root.setPadding(Ui.px(4), Ui.px(4), Ui.px(4), Ui.px(4));
            for (int i = 0; i < labels.length; i++) {
                final int index = i;
                TextView t = Ui.text(ActivityCompat(), labels[i], 14, p.muted, false);
                t.setGravity(Gravity.CENTER);
                t.setPadding(Ui.px(12), Ui.px(9), Ui.px(12), Ui.px(9));
                t.setMinWidth(Ui.px(56));
                t.setFocusable(true);
        t.setFocusableInTouchMode(true);
                t.setClickable(true);
                t.setOnClickListener(v -> {
                    if (this.selected == index) return;
                    this.selected = index;
                    applyStates();
                    listener.onSelected(index);
                });
                t.setOnFocusChangeListener((v, hasFocus) -> applyStates());
                items.add(t);
                root.addView(t, Ui.lp(WRAP, WRAP));
            }
            applyStates();
        }

        void setSelected(int index) {
            selected = index;
            applyStates();
        }

        void applyStates() {
            for (int i = 0; i < items.size(); i++) {
                TextView t = items.get(i);
                boolean isSelected = i == selected;
                GradientDrawable normal = isSelected
                        ? Ui.round(Ui.alpha(p.accent, 0.18f), 8)
                        : Ui.round(0x00000000, 8);
                GradientDrawable focused = Ui.roundStroke(
                        isSelected ? Ui.alpha(p.accent, 0.18f) : 0x00000000, 8, p.focus, 1);
                t.setBackground(Ui.states(normal, focused));
                t.setTextColor(isSelected ? p.text : p.muted);
            }
        }
    }

    private Activity ActivityCompat() {
        return this;
    }

    private final class AppCard extends FrameLayout {
        final AppInfo app;
        final int slot;

        AppCard(AppInfo app, int slot, int accent) {
            super(MainActivity.this);
            this.app = app;
            this.slot = slot;
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClickable(true);
            setElevation(Ui.px(8));
            setBackground(Ui.roundStroke(p.card, 30, p.cardBorder, 1));
            setOnClickListener(v -> launch(app));
            setOnLongClickListener(v -> {
                openApps(slot);
                return true;
            });

            View bar = new View(MainActivity.this);
            bar.setBackground(Ui.round(accent, 999));
            FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                    Ui.px(44), Ui.px(4), Gravity.TOP | Gravity.START);
            barLp.setMargins(Ui.px(26), Ui.px(24), 0, 0);
            addView(bar, barLp);

            TextView name = Ui.text(MainActivity.this, app.label,
                    tileSize == 0 ? 24 : tileSize == 2 ? 28 : 27, p.text, true);
            name.setMaxLines(2);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setAutoSizeTextTypeUniformWithConfiguration(
                    Ui.px(18), Ui.px(38), 1, TypedValue.COMPLEX_UNIT_PX);
            FrameLayout.LayoutParams nameLp = new FrameLayout.LayoutParams(
                    MATCH, WRAP, Gravity.CENTER_VERTICAL);
            nameLp.setMargins(Ui.px(26), Ui.px(8), Ui.px(26), Ui.px(8));
            addView(name, nameLp);

            TextView slotNo = Ui.text(MainActivity.this, String.format(Locale.US, "%02d", slot + 1),
                    12, p.muted2, false);
            FrameLayout.LayoutParams slotLp = new FrameLayout.LayoutParams(WRAP, WRAP,
                    Gravity.TOP | Gravity.END);
            slotLp.setMargins(0, Ui.px(23), Ui.px(24), 0);
            addView(slotNo, slotLp);

            ImageView arrow = new ImageView(MainActivity.this);
            arrow.setImageResource(R.drawable.ic_arrow);
            arrow.setColorFilter(accent);
            arrow.setAlpha(0f);
            FrameLayout.LayoutParams arrowLp = new FrameLayout.LayoutParams(
                    Ui.px(22), Ui.px(22), Gravity.BOTTOM | Gravity.END);
            arrowLp.setMargins(0, 0, Ui.px(23), Ui.px(21));
            addView(arrow, arrowLp);

            setOnFocusChangeListener((v, hasFocus) -> {
                animate().scaleX(hasFocus ? 1.045f : 1f).scaleY(hasFocus ? 1.045f : 1f)
                        .translationY(hasFocus ? -Ui.px(9) : 0f).setDuration(160).start();
                setBackground(Ui.roundStroke(hasFocus ? p.cardFocus : p.card, 30,
                        hasFocus ? p.focus : p.cardBorder, hasFocus ? 3 : 1));
                setElevation(hasFocus ? Ui.px(24) : Ui.px(8));
                arrow.animate().alpha(hasFocus ? 1f : 0f).setDuration(140).start();
            });
            setLayoutParams(new LinearLayout.LayoutParams(cardWidth(), cardHeight()));
        }
    }

    private final class AddCard extends FrameLayout {
        final int slot;

        AddCard(int slot) {
            super(MainActivity.this);
            this.slot = slot;
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClickable(true);
            setBackground(Ui.roundStroke(0x00000000, 30, p.cardBorder, 1));
            setOnClickListener(v -> openApps(slot));

            TextView slotNo = Ui.text(MainActivity.this, String.format(Locale.US, "%02d", slot + 1),
                    12, p.muted2, false);
            FrameLayout.LayoutParams sn = new FrameLayout.LayoutParams(WRAP, WRAP,
                    Gravity.TOP | Gravity.START);
            sn.setMargins(Ui.px(26), Ui.px(23), 0, 0);
            addView(slotNo, sn);

            ImageView plus = new ImageView(MainActivity.this);
            plus.setImageResource(R.drawable.ic_plus);
            plus.setColorFilter(p.muted);
            FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.px(40), Ui.px(40), Gravity.CENTER);
            addView(plus, plp);

            TextView label = Ui.text(MainActivity.this, L.s("Add app", "Добавить"), 17, p.muted, false);
            FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(WRAP, WRAP,
                    Gravity.BOTTOM | Gravity.START);
            llp.setMargins(Ui.px(26), 0, 0, Ui.px(23));
            addView(label, llp);

            setOnFocusChangeListener((v, hasFocus) -> {
                animate().scaleX(hasFocus ? 1.045f : 1f).scaleY(hasFocus ? 1.045f : 1f)
                        .translationY(hasFocus ? -Ui.px(9) : 0f).setDuration(160).start();
                setBackground(Ui.roundStroke(hasFocus ? p.cardFocus : 0x00000000, 30,
                        hasFocus ? p.focus : p.cardBorder, hasFocus ? 3 : 1));
                plus.setColorFilter(hasFocus ? p.text : p.muted);
                label.setTextColor(hasFocus ? p.text : p.muted);
            });
            setLayoutParams(new LinearLayout.LayoutParams(cardWidth(), cardHeight()));
        }
    }

    private final class DockButton extends LinearLayout {
        DockButton(int iconRes, String text, Runnable action) {
            super(MainActivity.this);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setFocusable(true); setFocusableInTouchMode(true);
            setClickable(true);
            setPadding(Ui.px(22), Ui.px(15), Ui.px(22), Ui.px(15));
            setBackground(Ui.states(Ui.round(0x00000000, 999),
                    Ui.round(Ui.alpha(p.accent, 0.16f), 999)));
            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageResource(iconRes);
            icon.setColorFilter(p.muted);
            addView(icon, Ui.lp(Ui.px(22), Ui.px(22)));
            TextView label = Ui.text(MainActivity.this, text, 19, p.muted, false);
            LinearLayout.LayoutParams lp = Ui.lp(WRAP, WRAP);
            lp.leftMargin = Ui.px(9);
            addView(label, lp);
            setOnClickListener(v -> action.run());
            setOnFocusChangeListener((v, hasFocus) -> {
                icon.setColorFilter(hasFocus ? p.text : p.muted);
                label.setTextColor(hasFocus ? p.text : p.muted);
            });
        }
    }

    private final class AppTile {
        final AppInfo app;
        final FrameLayout root;
        final ImageView check;
        TextView label;

        AppTile(AppInfo app) {
            this.app = app;
            root = new FrameLayout(MainActivity.this);
            root.setFocusable(true);
            root.setFocusableInTouchMode(true);
            root.setClickable(true);
            root.setBackground(Ui.roundStroke(0x00000000, 22, 0x00000000, 1));
            root.setOnClickListener(v -> {
                if (pickerSlot >= 0) {
                    slots[pickerSlot] = app.key;
                    saveSlots();
                    refreshCards();
                    refreshAppsOverlay();
                } else {
                    closeApps();
                    launch(app);
                }
            });
            root.setOnFocusChangeListener((v, hasFocus) -> refresh());

            LinearLayout content = Ui.col(MainActivity.this);
            content.setGravity(Gravity.CENTER);
            root.addView(content, new FrameLayout.LayoutParams(MATCH, MATCH));

            FrameLayout mini = new FrameLayout(MainActivity.this);
            mini.setBackground(Ui.round(Ui.alpha(0xFF7AA2FF, 0.16f), 16));
            content.addView(mini, Ui.lp(Ui.px(56), Ui.px(56)));
            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageDrawable(app.icon != null ? app.icon : getDrawable(R.drawable.ic_app_placeholder));
            mini.addView(icon, new FrameLayout.LayoutParams(Ui.px(34), Ui.px(34), Gravity.CENTER));

            label = Ui.text(MainActivity.this, app.label, 16, p.muted, false);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH, WRAP);
            llp.topMargin = Ui.px(10);
            content.addView(label, llp);

            check = new ImageView(MainActivity.this);
            check.setImageResource(R.drawable.ic_check);
            check.setColorFilter(p.accent);
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.px(20), Ui.px(20),
                    Gravity.TOP | Gravity.END);
            clp.setMargins(0, Ui.px(8), Ui.px(8), 0);
            root.addView(check, clp);
            refresh();
        }

        void refresh() {
            boolean current = pickerSlot >= 0 && app.key.equals(slots[pickerSlot]);
            check.setVisibility(current ? View.VISIBLE : View.GONE);
            int stroke = root.hasFocus() ? p.focus : (current ? p.accent : 0x00000000);
            root.setBackground(Ui.roundStroke(root.hasFocus() ? p.card : 0x00000000, 22, stroke,
                    stroke == 0 ? 1 : 2));
            if (label != null) label.setTextColor(root.hasFocus() ? p.text : p.muted);
        }
    }
}
