package com.moosemorals.linkshare;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

class AsyncPost extends AsyncTask<String, Void, AsyncResult<JSONObject>> {
    private final String TAG = "AsyncPost";
    private final String target;
    private final WeakReference<Consumer<AsyncResult<JSONObject>>> callback;
    private final WeakReference<Context> context;

    AsyncPost(Context context, String target, Consumer<AsyncResult<JSONObject>> callback) {
        this.target = target;
        this.callback = new WeakReference<>(callback);
        this.context = new WeakReference<>(context);
    }

    @Override
    protected AsyncResult<JSONObject> doInBackground(String... param) {

        String body = LinkShareApplication.paramEncode(param);
        try {
            URL url = new URL(LinkShareApplication.BASEURL + target);

            Log.d(TAG, "Connecting to " + url.toExternalForm());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setSSLSocketFactory(LinkShareApplication.getSSLSocketFactory(context.get()));

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length());
            try {
                Log.d(TAG, "connecting");

                conn.connect();

                Log.d(TAG, "Sending body");
                OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(conn.getOutputStream()), "UTF-8");
                out.write(body);
                out.flush();
                out.close();
                Log.d(TAG, "Body sent, waiting for response");

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
            Log.w(TAG, "Got an error, reporting it", ex);
            return new AsyncResult<>(ex);
        }
    }

    @Override
    protected void onPostExecute(AsyncResult<JSONObject> result) {
        Consumer<AsyncResult<JSONObject>> fn = callback.get();
        if (fn != null) {
            Log.d(TAG, "Sending response back to main thread");
            fn.accept(result);
        } else {
            Log.w(TAG, "Main thread got bored of waiting, I guess");
        }
    }

}
