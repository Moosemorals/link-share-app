package com.moosemorals.linkshare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public final class WebLinksActivity extends Activity {
    private static final String TAG = "WebLinksActivity";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView.setWebContentsDebuggingEnabled(true);

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(
                new LinkStore(this),
                "LinkStoreJava"
        );

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);


        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);
    }


    private WebView getWebView() {
        return webView;
    }

    private void loadLinks(Consumer<JSONArray> done) {
        getHttpClient().get("links", in -> {
            // Off main thread
            try {
                JSONObject json = LinkShareApplication.readStream(new InputStreamReader(in));
                if (json.has("success")) {
                    return json.getJSONArray("success");
                }
            } catch (JSONException | IOException e) {
                Log.w(TAG, "Problem fetching links", e);
            }
            return new JSONArray();
        }, done);
    }

    private void loadUsers(Consumer<JSONArray> done) {
        getHttpClient().get("users", in -> {
            // Off main thread
            try {
                JSONObject json = LinkShareApplication.readStream(new InputStreamReader(in));
                if (json.has("success")) {
                    return json.getJSONArray("success");
                }
            } catch (JSONException | IOException e) {
                Log.w(TAG, "Problem fetching links", e);
            }
            return new JSONArray();
        }, done);

    }

    private Backend getHttpClient() {
        return ((LinkShareApplication) getApplication()).getHttpClient();
    }

    private String getUser() {
        return LinkShareApplication.getSharedPreferences(this).getString(LinkShareApplication.USERNAME_KEY, null);
    }

    private static class LinkStore {

        private final WebLinksActivity parent;
        private final Handler handler;

        private JSONArray links;
        private JSONArray users;

        LinkStore(WebLinksActivity parent) {
            this.parent = parent;
            this.handler = new Handler(Looper.getMainLooper());
        }

        @JavascriptInterface
        public void logout() {
            Log.d(TAG, "Not implemented yet: Logout");
        }

        @JavascriptInterface
        public void visit(String id) {
            Log.d(TAG, "Not implemented yet: Visit id " + id);
        }

        @JavascriptInterface
        public void createLink(String id, String json) {
            Log.d(TAG, "Not implemented yet: Create link");
        }

        @JavascriptInterface
        public void getCredentials(String callback, String id) {
            JSONObject creds = new JSONObject();
            try {
                creds.put("user", parent.getUser());
            } catch (JSONException e) {
                throw new RuntimeException("Can't build JSON!", e);
            }

            sendToScript(callback, id, creds.toString());
        }

        @JavascriptInterface
        public void getUsers(String callback, String id) {
            if (users == null) {
                parent.loadUsers(json -> {
                            users = json;
                            sendToScript(callback, id, json.toString());
                        }
                );
            } else {
                sendToScript(callback, id, users.toString());
            }
        }

        @JavascriptInterface
        public void getLinks(String callback, String id) {
            if (links == null) {
                parent.loadLinks(json -> {
                            links = json;
                            sendToScript(callback, id, json.toString());
                        }
                );
            } else {
                sendToScript(callback, id, links.toString());
            }
        }

        private void sendToScript(String callback, String ...args) {

            StringBuilder javaScriptBuilder = new StringBuilder(callback);
            javaScriptBuilder.append("(");
            for (int i = 0; i < args.length; i += 1) {
                if (i != 0) {
                    javaScriptBuilder.append(",");
                }
                javaScriptBuilder.append("'");
                javaScriptBuilder.append(args[i]);
                javaScriptBuilder.append("'");
            }
            javaScriptBuilder.append(")");

            String javaScript = javaScriptBuilder.toString();


            Log.d(TAG, "Sending " + javaScript);

            handler.post(() -> parent
                    .getWebView()
                    .evaluateJavascript(
                            javaScript,
                            null
                    )
            );
        }
    }

}
