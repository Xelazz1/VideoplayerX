package com.xelazz1.videoplayerx;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the user's video library (id, content:// uri, display name,
 * duration, last playback position) to a small JSON file in the app's
 * private internal storage, so the list survives closing and reopening
 * the app. All reads/writes are synchronized since JS-interface calls
 * can arrive on a background thread.
 */
class VideoLibrary {

    static class Entry {
        String id;
        String uri;
        String name;
        double duration;
        double lastTime;
        String subtitle;

        Entry(String id, String uri, String name, double duration, double lastTime) {
            this(id, uri, name, duration, lastTime, null);
        }

        Entry(String id, String uri, String name, double duration, double lastTime, String subtitle) {
            this.id = id;
            this.uri = uri;
            this.name = name;
            this.duration = duration;
            this.lastTime = lastTime;
            this.subtitle = subtitle;
        }

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("name", name);
            o.put("duration", duration);
            o.put("lastTime", lastTime);
            if (subtitle != null) o.put("subtitle", subtitle);
            return o;
        }
    }

    private final File file;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    VideoLibrary(Context context) {
        file = new File(context.getFilesDir(), "library.json");
        load();
    }

    private synchronized void load() {
        entries.clear();
        if (!file.exists()) return;
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            return;
        }
        try {
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Entry e = new Entry(
                        o.getString("id"),
                        o.getString("uri"),
                        o.getString("name"),
                        o.optDouble("duration", 0),
                        o.optDouble("lastTime", 0),
                        o.optString("subtitle", null)
                );
                entries.put(e.id, e);
            }
        } catch (JSONException e) {
            // Corrupt file: start fresh rather than crashing the app
            entries.clear();
        }
    }

    private synchronized void persist() {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : entries.values()) {
                arr.put(e.toJson());
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString().getBytes("UTF-8"));
            }
        } catch (JSONException | IOException e) {
            // Best-effort persistence; a failed write just means this change
            // won't survive a restart, but the app keeps working.
        }
    }

    synchronized List<Entry> getAll() {
        return new ArrayList<>(entries.values());
    }

    synchronized Entry get(String id) {
        return entries.get(id);
    }

    synchronized void add(Entry e) {
        entries.put(e.id, e);
        persist();
    }

    synchronized void rename(String id, String name) {
        Entry e = entries.get(id);
        if (e != null) {
            e.name = name;
            persist();
        }
    }

    synchronized void updateProgress(String id, double lastTime, double duration) {
        Entry e = entries.get(id);
        if (e != null) {
            e.lastTime = lastTime;
            if (duration > 0) e.duration = duration;
            persist();
        }
    }

    synchronized void saveSubtitle(String id, String subtitleContent) {
        Entry e = entries.get(id);
        if (e != null) {
            e.subtitle = subtitleContent;
            persist();
        }
    }

    synchronized void remove(String id) {
        entries.remove(id);
        persist();
    }

    synchronized String toJsonArrayString(List<Entry> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : list) arr.put(e.toJson());
        } catch (JSONException ignored) {}
        return arr.toString();
    }
}
