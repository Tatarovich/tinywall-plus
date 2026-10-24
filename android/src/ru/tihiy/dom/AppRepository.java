package ru.tihiy.dom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AppRepository {
    private static final Collator COLLATOR = Collator.getInstance(new Locale("ru", "RU"));

    private AppRepository() {}

    static List<AppInfo> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(intent, 0);
        List<AppInfo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : found) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(context.getPackageName())) continue;
            String key = pkg + "/" + ri.activityInfo.name;
            if (!seen.add(key)) continue;
            CharSequence label = ri.loadLabel(pm);
            Drawable icon = ri.loadIcon(pm);
            out.add(new AppInfo(label == null ? pkg : label.toString(), pkg,
                    new ComponentName(pkg, ri.activityInfo.name), icon));
        }
        Collections.sort(out, (a, b) -> COLLATOR.compare(a.label, b.label));
        return out;
    }

    static String norm(String value) {
        if (value == null) return "";
        String s = value.toLowerCase(new Locale("ru", "RU")).replace('ё', 'е');
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) b.append(c);
        }
        return b.toString();
    }

    static boolean isKinopoisk(AppInfo a) {
        String n = norm(a.label);
        return n.contains("кинопоиск") || n.contains("kinopoisk") || a.pkg.equals("ru.kinopoisk");
    }

    static boolean isVkVideo(AppInfo a) {
        String n = norm(a.label);
        return ((n.contains("vk") || n.contains("вк")) && (n.contains("видео") || n.contains("video")))
                || a.pkg.equals("com.vk.vkvideo");
    }

    static boolean isMovieLab(AppInfo a) {
        String n = norm(a.label);
        return n.contains("movielab") || (n.contains("movie") && n.contains("lab"));
    }

    static boolean isMediaStation(AppInfo a) {
        String n = norm(a.label);
        return n.contains("mediastation") || (n.contains("media") && n.contains("station"));
    }

    static boolean isVlc(AppInfo a) {
        String n = norm(a.label);
        return n.equals("vlc") || n.contains("vlc") || a.pkg.equals("org.videolan.vlc");
    }

    static boolean isRuStore(AppInfo a) {
        String n = norm(a.label);
        return n.contains("rustore") || n.contains("рустор") || a.pkg.equals("ru.vk.store");
    }

    static List<String> defaultPins(List<AppInfo> apps) {
        List<String> out = new ArrayList<>();
        add(out, apps, 0);
        add(out, apps, 1);
        add(out, apps, 2);
        add(out, apps, 3);
        add(out, apps, 4);
        add(out, apps, 5);
        return out;
    }

    private static void add(List<String> out, List<AppInfo> apps, int which) {
        for (AppInfo a : apps) {
            if (out.contains(a.key)) continue;
            boolean ok;
            switch (which) {
                case 0: ok = isKinopoisk(a); break;
                case 1: ok = isVkVideo(a); break;
                case 2: ok = isMovieLab(a); break;
                case 3: ok = isMediaStation(a); break;
                case 4: ok = isVlc(a); break;
                default: ok = isRuStore(a); break;
            }
            if (ok) {
                out.add(a.key);
                return;
            }
        }
    }
}
