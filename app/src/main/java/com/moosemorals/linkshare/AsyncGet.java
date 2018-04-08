package com.moosemorals.linkshare;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

class AsyncGet extends AsyncTask<String, Void, AsyncResult<JSONObject>> {
    private static final String TAG = "AsyncGet";
    private final String target;
    private final WeakReference<Consumer<AsyncResult<JSONObject>>> callback;
    private final WeakReference<Context> context;

    AsyncGet(Context context, String target, Consumer<AsyncResult<JSONObject>> callback) {
        this.target = target;
        this.callback = new WeakReference<>(callback);
        this.context = new WeakReference<>(context);
    }

   @Override
    protected AsyncResult<JSONObject> doInBackground(String... param) {

        try {
            URL url;
            if (param != null && param.length > 0) {
                url = new URL(LinkShareApplication.BASEURL + target + "?" + LinkShareApplication.paramEncode(param));
            } else {
                url = new URL(LinkShareApplication.BASEURL + target);
            }

            Log.d(TAG, "Fetching " + url.toExternalForm());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setSSLSocketFactory(LinkShareApplication.getSSLSocketFactory(context.get()));

            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
            conn.setFixedLengthStreamingMode(0);
            try {
                Log.d(TAG, "Connecting");
                conn.connect();
                Log.d(TAG, "Connected, trying to read result");
                try (InputStreamReader in = new InputStreamReader(new BufferedInputStream(conn.getInputStream()), "UTF-8")) {
                    return new AsyncResult<>(LinkShareApplication.readStream(in));
                } catch (IOException ex) {
                    try (InputStreamReader in = new InputStreamReader(new BufferedInputStream(conn.getErrorStream()), "UTF-8")) {
                        return new AsyncResult<>(LinkShareApplication.readStream(in));
                    }
                }

            } finally {
                conn.disconnect();
            }

        } catch (Exception ex) {
            return new AsyncResult<>(ex);
        }
    }

    @Override
    protected void onPostExecute(AsyncResult<JSONObject> result) {
        Consumer<AsyncResult<JSONObject>> fn = callback.get();
        if (fn != null) {
            Log.d(TAG, "Passing result back to main thread");
            fn.accept(result);
        }
    }
}
