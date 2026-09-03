package com.xelazz1.videoplayerx;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private VideoLibrary library;

    // Legacy <input type="file"> chooser (used only if the page is ever opened
    // in a plain browser context instead of through this native picker flow)
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 5173;

    // Native "+" button picker, using the Storage Access Framework so the
    // granted access to each chosen video persists across app restarts
    private static final int PICK_VIDEOS_REQUEST_CODE = 5174;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        library = new VideoLibrary(this);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        // The page itself is loaded from file:///android_asset/, and it needs to be
        // able to load content:// video URIs as <video> sources. Modern WebView
        // restricts cross-scheme access from file:// pages by default, so this is
        // explicitly opened up here. Safe in this app because we only ever load
        // our own bundled local HTML - never remote/untrusted pages.
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                              FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("video/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                try {
                    startActivityForResult(Intent.createChooser(intent, "Video seç"),
                            FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // The player never needs camera/mic/protected-media access - deny by
                // default instead of blanket-granting whatever the page asks for.
                request.deny();
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl("file:///android_asset/video_player.html");
    }

    // ---------- JS <-> native bridge ----------
    private class WebAppInterface {

        @JavascriptInterface
        public String getLibrary() {
            return library.toJsonArrayString(library.getAll());
        }

        @JavascriptInterface
        public void pickVideos() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("video/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(intent, PICK_VIDEOS_REQUEST_CODE);
                } catch (Exception ignored) {
                    // No document picker available on this device; nothing to do
                }
            });
        }

        @JavascriptInterface
        public void renameVideo(String id, String name) {
            library.rename(id, name);
        }

        @JavascriptInterface
        public void deleteVideo(String id) {
            VideoLibrary.Entry e = library.get(id);
            if (e != null) {
                try {
                    getContentResolver().releasePersistableUriPermission(
                            Uri.parse(e.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                    // Permission may already be gone; not a problem
                }
            }
            library.remove(id);
        }

        @JavascriptInterface
        public void saveProgress(String id, double lastTime, double duration) {
            library.updateProgress(id, lastTime, duration);
        }

        @JavascriptInterface
        public void saveSubtitle(String id, String subtitleContent) {
            library.saveSubtitle(id, subtitleContent);
        }

        @JavascriptInterface
        public String getSettings() {
            android.content.SharedPreferences prefs =
                    getSharedPreferences("vpx_settings", MODE_PRIVATE);
            JSONObject o = new JSONObject();
            try {
                o.put("theme", prefs.getString("theme", "dark"));
                o.put("lang", prefs.getString("lang", "tr"));
            } catch (JSONException ignored) {}
            return o.toString();
        }

        @JavascriptInterface
        public void saveSettings(String theme, String lang) {
            getSharedPreferences("vpx_settings", MODE_PRIVATE)
                    .edit()
                    .putString("theme", theme)
                    .putString("lang", lang)
                    .apply();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_VIDEOS_REQUEST_CODE) {
            handleVideosPicked(resultCode, data);
            return;
        }
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleVideosPicked(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) return;

        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;

        JSONArray addedJson = new JSONArray();
        ContentResolver resolver = getContentResolver();

        for (Uri uri : uris) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Some providers don't support persistable permissions; the video will
                // still work for this session, it just may not survive an app restart.
            }

            String displayName = queryDisplayName(resolver, uri);
            String id = UUID.randomUUID().toString();

            VideoLibrary.Entry entry = new VideoLibrary.Entry(id, uri.toString(), displayName, 0, 0);
            library.add(entry);

            try {
                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("uri", uri.toString());
                o.put("name", displayName);
                addedJson.put(o);
            } catch (JSONException ignored) {}
        }

        String js = "window.onVideosAdded && window.onVideosAdded("
                + JSONObject.quote(addedJson.toString()) + ")";
        // JSONObject.quote wraps the JSON array string safely as a JS string literal;
        // the page parses it back into an array with JSON.parse.
        webView.evaluateJavascript(js, null);
    }

    private String queryDisplayName(ContentResolver resolver, Uri uri) {
        String name = null;
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}

        if (name == null || name.trim().isEmpty()) {
            name = "Video";
        } else {
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
        }
        return name;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
