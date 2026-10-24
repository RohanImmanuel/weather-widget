package com.rohan.weatherwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherWidget extends AppWidgetProvider {
    // ponytail: fixed to Dublin; switch to device location if you travel with it
    static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=53.35&longitude=-6.26"
            + "&current=temperature_2m,apparent_temperature,weather_code"
            + "&daily=temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,uv_index_max,precipitation_sum"
            + "&hourly=precipitation_probability,precipitation&timezone=auto&forecast_days=2";
    static final String WARNINGS_URL = "https://www.met.ie/Open_Data/json/warning_EI07.json"; // EI07 = Dublin
    static final int HOURS = 12;
    static final String TICK = "com.rohan.weatherwidget.TICK";
    static final ZoneId MUMBAI = ZoneId.of("Asia/Kolkata");
    static final int RED = 0xFFD71921, GREY = 0xFF8C8C8C, DOT_GREY = 0xFFAAAAAA, OUTLINE = 0xFF262626;

    // 5x7 dot-matrix glyphs; '#' = lit dot
    static final Map<Character, String[]> GLYPHS = Map.ofEntries(
            Map.entry('0', new String[]{" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "}),
            Map.entry('1', new String[]{"  #  ", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "}),
            Map.entry('2', new String[]{" ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####"}),
            Map.entry('3', new String[]{" ### ", "#   #", "    #", "  ## ", "    #", "#   #", " ### "}),
            Map.entry('4', new String[]{"   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # "}),
            Map.entry('5', new String[]{"#####", "#    ", "#### ", "    #", "    #", "#   #", " ### "}),
            Map.entry('6', new String[]{" ### ", "#    ", "#    ", "#### ", "#   #", "#   #", " ### "}),
            Map.entry('7', new String[]{"#####", "    #", "   # ", "  #  ", "  #  ", "  #  ", "  #  "}),
            Map.entry('8', new String[]{" ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### "}),
            Map.entry('9', new String[]{" ### ", "#   #", "#   #", " ####", "    #", "    #", " ### "}),
            Map.entry('-', new String[]{"    ", "    ", "    ", "####", "    ", "    ", "    "}),
            Map.entry('.', new String[]{" ", " ", " ", " ", " ", " ", "#"}),
            Map.entry('°', new String[]{"###", "# #", "###", "   ", "   ", "   ", "   "}),
            Map.entry('A', new String[]{" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"}),
            Map.entry('B', new String[]{"#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### "}),
            Map.entry('C', new String[]{" ### ", "#   #", "#    ", "#    ", "#    ", "#   #", " ### "}),
            Map.entry('D', new String[]{"#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### "}),
            Map.entry('E', new String[]{"#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"}),
            Map.entry('F', new String[]{"#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    "}),
            Map.entry('G', new String[]{" ### ", "#   #", "#    ", "# ###", "#   #", "#   #", " ####"}),
            Map.entry('H', new String[]{"#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"}),
            Map.entry('I', new String[]{" ### ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "}),
            Map.entry('J', new String[]{"  ###", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  "}),
            Map.entry('K', new String[]{"#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #"}),
            Map.entry('L', new String[]{"#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####"}),
            Map.entry('M', new String[]{"#   #", "## ##", "# # #", "# # #", "#   #", "#   #", "#   #"}),
            Map.entry('N', new String[]{"#   #", "#   #", "##  #", "# # #", "#  ##", "#   #", "#   #"}),
            Map.entry('O', new String[]{" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "}),
            Map.entry('P', new String[]{"#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    "}),
            Map.entry('Q', new String[]{" ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #"}),
            Map.entry('R', new String[]{"#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"}),
            Map.entry('S', new String[]{" ####", "#    ", "#    ", " ### ", "    #", "    #", "#### "}),
            Map.entry('T', new String[]{"#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  "}),
            Map.entry('U', new String[]{"#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "}),
            Map.entry('V', new String[]{"#   #", "#   #", "#   #", "#   #", "#   #", " # # ", "  #  "}),
            Map.entry('W', new String[]{"#   #", "#   #", "#   #", "# # #", "# # #", "# # #", " # # "}),
            Map.entry('X', new String[]{"#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #"}),
            Map.entry('Y', new String[]{"#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  "}),
            Map.entry('Z', new String[]{"#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####"}),
            Map.entry(':', new String[]{" ", " ", "#", " ", "#", " ", " "}),
            Map.entry(' ', new String[]{"  ", "  ", "  ", "  ", "  ", "  ", "  "}));

    static final String[] SUN = {"    #    ", " #     # ", "   ###   ", "  #   #  ", "# #   # #", "  #   #  ", "   ###   ", " #     # ", "    #    "};
    static final String[] CLOUD = {"           ", "    ###    ", "  ##   #   ", " #      ## ", "#         #", "#         #", " ######### "};
    static final String[] PARTLY = {"  #        ", "#   #      ", "  ##  ###  ", " #  ##   # ", "   #      #", "  #       #", "   ####### "};
    static final String[] FOG = {"#########", "         ", " ####### ", "         ", "#########", "         ", "  #####  "};
    static final String[] CLOUD_TOP = {"    ###    ", "  ##   #   ", " #      ## ", "#         #", " ######### ", "           "};
    static final String[] RAIN = concat(CLOUD_TOP, "  #  #  #  ", " #  #  #   ");
    static final String[] SNOW = concat(CLOUD_TOP, " #   #   # ", "           ", "   #   #   ");
    static final String[] THUNDER = concat(CLOUD_TOP, "     ##    ", "    ##     ", "   ####    ", "    ##     ");
    static final String[] THERMO = {"  #  ", " # # ", " # # ", " ### ", " ### ", "#####", " ### "};
    static final String[] UV_SUN = {"   #   ", " #   # ", "  ###  ", "# ### #", "  ###  ", " #   # ", "   #   "};
    static final String[] DROP = {"   #   ", "   #   ", "  ###  ", " ##### ", " ##### ", "  ###  ", "       "};

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (!TICK.equals(a) && !Intent.ACTION_TIME_CHANGED.equals(a) && !Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            super.onReceive(c, i);
            return;
        }
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, WeatherWidget.class));
        if (ids.length == 0) return;
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        fillClock(c, v);
        m.partiallyUpdateAppWidget(ids, v);
        scheduleTick(c);
    }

    @Override
    public void onDisabled(Context c) {
        c.getSystemService(AlarmManager.class).cancel(tickIntent(c));
    }

    static PendingIntent tickIntent(Context c) {
        return PendingIntent.getBroadcast(c, 1, new Intent(c, WeatherWidget.class).setAction(TICK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // RTC (non-wakeup): ticks while the screen is on, catches up as soon as it wakes.
    // AllowWhileIdle so Battery Saver doesn't defer it (quota ~72/h, we use 60)
    static void scheduleTick(Context c) {
        long next = (System.currentTimeMillis() / 60_000 + 1) * 60_000 + 200; // just past the minute
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC, next, tickIntent(c));
        else am.set(AlarmManager.RTC, next, tickIntent(c));
    }

    static void fillClock(Context c, RemoteViews v) {
        boolean h24 = DateFormat.is24HourFormat(c);
        DateTimeFormatter hm = DateTimeFormatter.ofPattern(h24 ? "HH:mm" : "hh:mm");
        ZonedDateTime here = ZonedDateTime.now(), mum = ZonedDateTime.now(MUMBAI);
        v.setImageViewBitmap(R.id.clock, text(here.format(hm), Color.WHITE, 20));
        v.setViewVisibility(R.id.ampm, h24 ? View.GONE : View.VISIBLE);
        if (!h24) v.setImageViewBitmap(R.id.ampm, text(fmt(here, "a"), DOT_GREY, 5));
        v.setImageViewBitmap(R.id.date, text(fmt(here, "EEE dd MMM yyyy"), DOT_GREY, 5));
        v.setImageViewBitmap(R.id.mumbaiLabel, text("MUMBAI", RED, 4));
        v.setImageViewBitmap(R.id.mumbaiClock, text(mum.format(hm), Color.WHITE, 9));
        v.setImageViewBitmap(R.id.mumbaiDate, text(fmt(mum, h24 ? "EEE dd" : "a EEE dd"), DOT_GREY, 4));
    }

    static String fmt(ZonedDateTime t, String pattern) {
        return t.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)).toUpperCase(Locale.ROOT);
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        scheduleTick(c);
        PendingResult r = goAsync();
        new Thread(() -> {
            try { update(c, m, ids); } finally { r.finish(); }
        }).start();
    }

    static void update(Context c, AppWidgetManager m, int[] ids) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        Intent tap = new Intent(c, WeatherWidget.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        PendingIntent refresh = PendingIntent.getBroadcast(c, 0, tap,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.root, refresh);
        fillClock(c, v);
        try {
            fillWeather(v);
        } catch (Exception e) {
            // keep the last good forecast on screen, just flag the failure
            v = new RemoteViews(c.getPackageName(), R.layout.widget);
            v.setOnClickPendingIntent(R.id.root, refresh);
            v.setTextViewText(R.id.updated, "Offline " + now() + " · tap to retry");
            m.partiallyUpdateAppWidget(ids, v);
            return;
        }
        try {
            fillWarnings(v);
        } catch (Exception e) {
            v.setTextViewText(R.id.warning, "COULDN'T CHECK MET ÉIREANN WARNINGS");
            v.setTextColor(R.id.warning, GREY);
        }
        v.setTextViewText(R.id.updated, "Updated " + now() + " · tap to refresh");
        m.updateAppWidget(ids, v);
    }

    static void fillWeather(RemoteViews v) throws Exception {
        JSONObject j = new JSONObject(get(WEATHER_URL));
        JSONObject cur = j.getJSONObject("current"), d = j.getJSONObject("daily"), h = j.getJSONObject("hourly");
        int code = cur.getInt("weather_code");

        v.setImageViewBitmap(R.id.icon, matrix(12, Color.WHITE, icon(code)));
        v.setImageViewBitmap(R.id.temp, text(round(cur.getDouble("temperature_2m")) + "°", Color.WHITE, 14));
        v.setTextViewText(R.id.cond, describe(code));
        v.setTextViewText(R.id.feels, "Feels " + round(cur.getDouble("apparent_temperature")) + "°");
        v.setTextViewText(R.id.hilo, "H " + round(d.getJSONArray("temperature_2m_max").getDouble(0)) + "°  L "
                + round(d.getJSONArray("temperature_2m_min").getDouble(0)) + "°");

        v.setImageViewBitmap(R.id.feelsIcon, matrix(4, GREY, THERMO));
        v.setImageViewBitmap(R.id.feelsRange, text(round(d.getJSONArray("apparent_temperature_min").getDouble(0)) + "-"
                + round(d.getJSONArray("apparent_temperature_max").getDouble(0)) + "°", Color.WHITE, 8));
        double uv = d.getJSONArray("uv_index_max").getDouble(0);
        int uvColor = uv >= 6 ? RED : Color.WHITE; // red only once protection is needed
        v.setImageViewBitmap(R.id.uvIcon, matrix(4, uv >= 6 ? RED : GREY, UV_SUN));
        v.setImageViewBitmap(R.id.uv, text(String.valueOf(round(uv)), uvColor, 8));
        v.setTextViewText(R.id.uvLabel, "UV · " + uvLabel(uv).toUpperCase(Locale.ROOT));
        v.setImageViewBitmap(R.id.rainIcon, matrix(4, GREY, DROP));
        v.setImageViewBitmap(R.id.rainTotal, text(
                String.format(Locale.ROOT, "%.1f", d.getJSONArray("precipitation_sum").getDouble(0)), Color.WHITE, 8));

        JSONArray times = h.getJSONArray("time"), prob = h.getJSONArray("precipitation_probability"), mm = h.getJSONArray("precipitation");
        String hourNow = cur.getString("time").substring(0, 13);
        int start = 0;
        while (start < times.length() - 1 && !times.getString(start).startsWith(hourNow)) start++;
        int n = Math.min(HOURS, times.length() - start);
        String[] labels = new String[n];
        int[] p = new int[n];
        double[] amount = new double[n];
        for (int i = 0; i < n; i++) {
            int hr = Integer.parseInt(times.getString(start + i).substring(11, 13));
            labels[i] = i == 0 ? "NOW" : (hr % 12 == 0 ? 12 : hr % 12) + (hr < 12 ? "A" : "P");
            p[i] = prob.optInt(start + i);
            amount[i] = mm.optDouble(start + i, 0);
        }
        v.setImageViewBitmap(R.id.rain, chart(labels, p, amount));
    }

    static void fillWarnings(RemoteViews v) throws Exception {
        JSONArray a = new JSONArray(get(WARNINGS_URL));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE h:mma", Locale.ENGLISH);
        OffsetDateTime now = OffsetDateTime.now();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String worst = null;
        for (int i = 0; i < a.length(); i++) {
            JSONObject w = a.getJSONObject(i);
            OffsetDateTime onset = OffsetDateTime.parse(w.getString("onset")), expiry = OffsetDateTime.parse(w.getString("expiry"));
            if (expiry.isBefore(now)) continue;
            String level = w.getString("level");
            if (rank(level) > rank(worst)) worst = level;
            if (sb.length() > 0) sb.append('\n');
            int dot = sb.length();
            sb.append("▲ ");
            sb.setSpan(new ForegroundColorSpan(levelColor(level)), dot, dot + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String line = level + " " + w.getString("type") + " warning";
            if (onset.isAfter(now)) line += " from " + onset.atZoneSameInstant(ZoneId.systemDefault()).format(f);
            line += " until " + expiry.atZoneSameInstant(ZoneId.systemDefault()).format(f);
            sb.append(line.toUpperCase(Locale.ROOT));
        }
        if (sb.length() == 0) {
            v.setTextViewText(R.id.warning, "○ NO WEATHER WARNINGS");
            v.setTextColor(R.id.warning, GREY);
        } else {
            v.setTextViewText(R.id.warning, sb);
            v.setTextColor(R.id.warning, Color.WHITE);
        }
        v.setColorStateList(R.id.warning, "setBackgroundTintList",
                ColorStateList.valueOf(worst == null ? OUTLINE : levelColor(worst)));
    }

    // widgets ignore app fonts, so numbers and icons are drawn as dot grids
    static Bitmap matrix(int pitch, int color, String[]... glyphs) {
        int rows = glyphs[0].length, cols = -1;
        for (String[] g : glyphs) cols += g[0].length() + 1;
        Bitmap b = Bitmap.createBitmap(Math.max(cols, 1) * pitch, rows * pitch, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        int x0 = 0;
        for (String[] g : glyphs) {
            for (int r = 0; r < rows; r++)
                for (int k = 0; k < g[r].length(); k++)
                    if (g[r].charAt(k) == '#') c.drawCircle(x0 + k * pitch + pitch / 2f, r * pitch + pitch / 2f, pitch * 0.4f, p);
            x0 += (g[0].length() + 1) * pitch;
        }
        return b;
    }

    static Bitmap text(String s, int color, int pitch) {
        List<String[]> g = new ArrayList<>();
        for (char ch : s.toCharArray()) if (GLYPHS.containsKey(ch)) g.add(GLYPHS.get(ch));
        return matrix(pitch, color, g.toArray(new String[0][]));
    }

    // each hour is a 3-dot-wide column; lit dots = chance of rain
    static Bitmap chart(String[] labels, int[] p, double[] mm) {
        int w = 1150, h = 250, rows = 8, cols = 3, top = 34, gridBottom = 206;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(24);

        float colW = (float) w / labels.length, pitch = (float) (gridBottom - top) / rows, r = pitch * 0.32f;
        for (int i = 0; i < labels.length; i++) {
            float cx = i * colW + colW / 2;
            int lit = p[i] == 0 ? 0 : Math.max(1, Math.round(p[i] / 100f * rows));
            int on = i == 0 ? RED : mm[i] > 0 ? Color.WHITE : 0xFF9A9A9A; // grey = chance but no measurable rain
            for (int row = 0; row < rows; row++) {
                dot.setColor(row < lit ? on : 0xFF1F1F1F);
                float y = gridBottom - pitch / 2 - row * pitch;
                for (int k = 0; k < cols; k++) c.drawCircle(cx + (k - 1) * pitch, y, r, dot);
            }
            if (p[i] >= 20) {
                text.setColor(i == 0 ? RED : Color.WHITE);
                c.drawText(p[i] + "", cx, top - 8, text);
            }
            text.setColor(i == 0 ? RED : GREY);
            c.drawText(labels[i], cx, h - 6, text);
        }
        return b;
    }

    static String[] concat(String[] a, String... b) {
        String[] out = new String[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static String get(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);
        try (InputStream in = con.getInputStream()) {
            return new String(in.readAllBytes());
        } finally {
            con.disconnect();
        }
    }

    static int rank(String level) {
        return level == null ? 0 : switch (level) { case "red" -> 3; case "orange" -> 2; case "yellow" -> 1; default -> 0; };
    }

    static int levelColor(String level) {
        return switch (level) { case "red" -> RED; case "orange" -> 0xFFFF8A00; default -> 0xFFFFD000; };
    }

    static String uvLabel(double uv) {
        return uv < 3 ? "Low" : uv < 6 ? "Moderate" : uv < 8 ? "High" : uv < 11 ? "Very high" : "Extreme";
    }

    // WMO weather codes used by Open-Meteo
    static String describe(int code) {
        if (code == 0) return "Clear";
        if (code == 1) return "Mainly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code <= 48) return "Fog";
        if (code <= 57) return "Drizzle";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Showers";
        if (code <= 86) return "Snow showers";
        return "Thunderstorm";
    }

    static String[] icon(int code) {
        if (code <= 1) return SUN;
        if (code == 2) return PARTLY;
        if (code == 3) return CLOUD;
        if (code <= 48) return FOG;
        if (code <= 67 || (code >= 80 && code <= 82)) return RAIN;
        if (code <= 86) return SNOW;
        return THUNDER;
    }

    static long round(double d) { return Math.round(d); }

    static String now() { return LocalTime.now().format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)); }
}
