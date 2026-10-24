package ru.tihiy.dom;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

final class SceneRepository {
    private SceneRepository() {}

    static List<SceneInfo> load(Context context) {
        List<SceneInfo> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                context.getAssets().open("scenes.txt"), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", 3);
                if (p.length < 2 || p[0].trim().isEmpty()) continue;
                String id = p[0].trim();
                String en = p[1].trim();
                String ru = p.length >= 3 ? p[2].trim() : en;
                if (en.isEmpty()) en = id;
                if (ru.isEmpty()) ru = en;
                out.add(new SceneInfo(id, en, ru));
            }
        } catch (Exception ignored) {
        }
        if (out.isEmpty()) {
            out.add(new SceneInfo("mountains", "Mountains", "Горы"));
            out.add(new SceneInfo("fjord", "Fjord", "Фьорд"));
            out.add(new SceneInfo("city", "Night City", "Ночной город"));
        }
        return out;
    }

    static boolean has(List<SceneInfo> scenes, String id) {
        for (SceneInfo s : scenes) if (s.id.equals(id)) return true;
        return false;
    }

    static String label(List<SceneInfo> scenes, String id) {
        for (SceneInfo s : scenes) if (s.id.equals(id)) return s.label();
        return L.s("Custom photo", "Своё фото");
    }
}
