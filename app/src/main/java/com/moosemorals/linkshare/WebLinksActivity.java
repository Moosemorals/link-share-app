package com.moosemorals.linkshare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public final class WebLinksActivity extends Activity {
    private static final String TAG = "WebLinksActivity";

    private WebView webView;
    private LinkStore linkStore;
    private LinkShareApplication app;

    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (LinkShareApplication)getApplication();

        // We might have arrived here from a SEND intent, so check if we're logged in
        String user = app.getUserName();
        if (user == null) {
            startLoginActivity();
            return;
        }

        linkStore = new LinkStore(this);

        WebView.setWebContentsDebuggingEnabled(true);

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(
                linkStore,
                "LinkStoreJava"
        );

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);

        String url = getGivenUrl();
        if (url != null) {
            new PageFetcher(l -> linkStore.poke(l)).execute(url);
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        this.finish();
    }

    private WebView getWebView() {
        return webView;
    }

    private String getGivenUrl() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_SEND)) {

            String link = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (link != null && Patterns.WEB_URL.matcher(link).matches()) {
                return link;
            }
        }
        return null;
    }

    private void loadLinks(Consumer<JSONArray> done) {
        getHttpClient().get("links", in -> {
            // Off main thread
            try {
                JSONObject json = LinkShareApplication.readJsonFromStream(in);
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
                JSONObject json = LinkShareApplication.readJsonFromStream(in);
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
        return app.getHttpClient();
    }

    private String getUserName() {
        return app.getUserName();
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

        void poke(JSONObject link) {
            sendToScript("LinkStore._doEvent", "poke", link.toString());
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
        public void getGivenLink(String callback, String id) {
            String link = parent.getGivenUrl();
            if (link != null) {
                new PageFetcher(l -> sendToScript(callback, id, l.toString())).execute(link);
            } else {
                sendToScript(callback, id);
            }
        }

        @JavascriptInterface
        public void getCredentials(String callback, String id) {
            JSONObject creds = new JSONObject();
            try {
                creds.put("user", parent.getUserName());
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

        private void sendToScript(String callback, String... args) {

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

    private static final class PageFetcher extends AsyncTask<String, Void, JSONObject> {

        private final Consumer<JSONObject> done;

        PageFetcher(Consumer<JSONObject> done) {
            this.done = done;
        }

        @Override
        protected JSONObject doInBackground(String... strings) {

            String target = strings[0];
            JSONObject result = new JSONObject();
            try {

                result.put("url", target);

                // We're only going to play with http here
                if (!target.startsWith("http")) {
                    return result;
                }

                URL uri = new URL(target);
                HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
                conn.setInstanceFollowRedirects(true);

                Log.d(TAG, "Connecting to " + target);
                conn.connect();
                Log.d(TAG, "Connected");

                int status = conn.getResponseCode();
                result.put("status", status);

                if (status == 200) {
                    InputStream in = new BufferedInputStream(conn.getInputStream());

                    // We want the url after any redirects.
                    String destUrl = conn.getURL().toExternalForm();
                    result.put("url", destUrl);

                    Log.d(TAG, "Starting parse");
                    Document doc = Jsoup.parse(in, null, destUrl);
                    Log.d(TAG, "Parse complete");

                    Elements title = doc.select("title");
                    if (title.size() > 0) {
                        result.put("title", title.get(0).text());
                    }

                    Elements favIconList = doc.select("link[rel=icon],link[rel='icon shortcut'],link[rel='shortcut icon']");
                    String favIconUrl = null;
                    if (favIconList.size() > 0) {
                        Log.d(TAG, "Favcon candidates " + favIconList.toString());
                        for (int i = favIconList.size() - 1; i >= 0; i -= 1) {
                            favIconUrl = favIconList.get(i).attr("abs:href");
                            if (favIconUrl != null) {
                                break;
                            }
                        }
                    }
                    if (favIconUrl != null) {
                        result.put("favIconURL", favIconUrl);
                    }

                }
            } catch (IOException | JSONException ex) {
                Log.w(TAG, "Some kind of problem fetching " + target, ex);
            }

            Log.d(TAG, "Result of fetch is: " + result.toString());

            return result;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            done.accept(json);
        }
    }


}
