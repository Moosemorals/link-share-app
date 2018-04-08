package com.moosemorals.linkshare;

import android.content.Context;
import android.net.Uri;
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

class AsyncGetFavicon extends AsyncTask<String, Void, AsyncResult<String>> {
    private static final String TAG = "AsyncGetFavicon";
    private final Uri target;
    private final WeakReference<Consumer<AsyncResult<String>>> callback;
    private final WeakReference<Context> context;

    AsyncGetFavicon(Context context, Uri target, Consumer<AsyncResult<String>> callback) {
        this.target = target;
        this.callback = new WeakReference<>(callback);
        this.context = new WeakReference<>(context);
    }

   @Override
    protected AsyncResult<String> doInBackground(String... param) {

        try {

            Log.d(TAG, "Fetching " + target.toString());

            HttpsURLConnection conn = (HttpsURLConnection) new URL(target.toString()).openConnection();

            conn.setSSLSocketFactory(LinkShareApplication.getSSLSocketFactory(context.get()));

            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
            conn.setFixedLengthStreamingMode(0);
            try {
                conn.connect();

                conn.getContentType();

                try (InputStreamReader in = new InputStreamReader(new BufferedInputStream(conn.getInputStream()),));


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
