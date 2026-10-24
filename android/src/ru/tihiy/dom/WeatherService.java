package ru.tihiy.dom;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class WeatherService {
    static final double DEFAULT_LAT = 55.7558;
    static final double DEFAULT_LON = 37.6173;

    static final class Place {
        final String name;
        final String region;
        final double lat;
        final double lon;

        Place(String name, String region, double lat, double lon) {
            this.name = name;
            this.region = region;
            this.lat = lat;
            this.lon = lon;
        }
    }

    static final class Current {
        final int temp;
        final int code;

        Current(int temp, int code) {
            this.temp = temp;
            this.code = code;
        }
    }

    interface PlacesCallback {
        void onResult(List<Place> places, String error);
    }

    interface CurrentCallback {
        void onResult(Current current, String error);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    void search(String query, boolean english, PlacesCallback callback) {
        executor.execute(() -> {
            List<Place> places = new ArrayList<>();
            String error = null;
            try {
                String lang = english ? "en" : "ru";
                String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(query, "UTF-8")
                        + "&count=8&language=" + lang + "&format=json";
                JSONObject root = new JSONObject(get(url));
                JSONArray results = root.optJSONArray("results");
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject o = results.getJSONObject(i);
                        String name = o.optString("name", "").trim();
                        if (name.isEmpty()) continue;
                        String admin = o.optString("admin1", "").trim();
                        String country = o.optString("country", "").trim();
                        String region;
                        if (!admin.isEmpty() && !country.isEmpty()) region = admin + ", " + country;
                        else if (!country.isEmpty()) region = country;
                        else region = admin;
                        places.add(new Place(name, region, o.optDouble("latitude"),
                                o.optDouble("longitude")));
                    }
                }
                if (places.isEmpty()) {
                    error = L.s("No cities found", "Города не найдены");
                }
            } catch (Exception e) {
                error = L.s("Could not search. Check the internet connection.",
                        "Не удалось выполнить поиск. Проверьте интернет.");
            }
            final List<Place> result = places;
            final String err = error;
            main.post(() -> callback.onResult(result, err));
        });
    }

    void fetch(double lat, double lon, boolean english, CurrentCallback callback) {
        executor.execute(() -> {
            Current current = null;
            String error = null;
            try {
                String url = "https://wttr.in/" + lat + "," + lon
                        + "?format=j1&lang=" + (english ? "en" : "ru");
                JSONObject root = new JSONObject(get(url));
                JSONArray conditions = root.optJSONArray("current_condition");
                if (conditions == null || conditions.length() == 0) {
                    throw new IllegalStateException("no current");
                }
                JSONObject cur = conditions.getJSONObject(0);
                int temp = Integer.parseInt(cur.optString("temp_C", "0"));
                int code = wttrToWmo(cur.optInt("weatherCode", 122));
                current = new Current(temp, code);
            } catch (Exception e) {
                error = L.s("Weather is unavailable. Check the internet connection.",
                        "Погода недоступна. Проверьте интернет.");
            }
            final Current value = current;
            final String err = error;
            main.post(() -> callback.onResult(value, err));
        });
    }

    private static int wttrToWmo(int code) {
        if (code == 113) return 0;
        if (code == 116) return 2;
        if (code == 119 || code == 122) return 3;
        if (code == 143 || code == 248 || code == 260) return 45;
        if (code == 176 || code == 263 || code == 266 || code == 293 || code == 296
                || code == 353 || code == 356) return 61;
        if (code == 299 || code == 302 || code == 305 || code == 308 || code == 359) return 65;
        if (code == 179 || code == 227 || code == 230 || code == 323 || code == 326
                || code == 329 || code == 332 || code == 335 || code == 338
                || code == 368 || code == 371) return 73;
        if (code == 182 || code == 185 || code == 281 || code == 284 || code == 311
                || code == 314 || code == 317 || code == 320 || code == 362
                || code == 365 || code == 374 || code == 377) return 66;
        if (code == 200 || code == 386 || code == 389 || code == 392 || code == 395) return 95;
        return 3;
    }

    void shutdown() {
        executor.shutdownNow();
    }

    private static String get(String urlValue) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlValue).openConnection();
        c.setConnectTimeout(9000);
        c.setReadTimeout(9000);
        c.setRequestProperty("User-Agent", "TinyWall/0.5");
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
            try (InputStream in = c.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                return out.toString("UTF-8");
            }
        } finally {
            c.disconnect();
        }
    }

    static String icon(int code) {
        if (code == 0) return "☀";
        if (code <= 3) return "☁";
        if (code == 45 || code == 48) return "≋";
        if (code >= 51 && code <= 67) return "☂";
        if (code >= 71 && code <= 77) return "❄";
        if (code >= 80 && code <= 82) return "☂";
        if (code == 85 || code == 86) return "❄";
        if (code >= 95) return "⚡";
        return "☁";
    }

    static String condition(int code, boolean english) {
        if (code == 0) return L.s("Clear", "Ясно");
        if (code == 1) return L.s("Mostly clear", "Почти ясно");
        if (code == 2) return L.s("Partly cloudy", "Переменная облачность");
        if (code == 3) return L.s("Cloudy", "Облачно");
        if (code == 45 || code == 48) return L.s("Fog", "Туман");
        if (code >= 51 && code <= 57) return L.s("Drizzle", "Морось");
        if (code >= 61 && code <= 67) return L.s("Rain", "Дождь");
        if (code >= 71 && code <= 77) return L.s("Snow", "Снег");
        if (code >= 80 && code <= 82) return L.s("Showers", "Ливни");
        if (code == 85 || code == 86) return L.s("Snow showers", "Снегопад");
        if (code >= 95) return L.s("Thunderstorm", "Гроза");
        return L.s("Cloudy", "Облачно");
    }
}
