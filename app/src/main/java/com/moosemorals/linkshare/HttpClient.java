package com.moosemorals.linkshare;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;

final class HttpClient {
    private static final String TAG = "HttpClient";
    private final Map<String, WeakReference<Bitmap>> cache = new HashMap<>();
    private final LinkedList<QueueItem> queue = new LinkedList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final Runnable backgroundRunnable = () -> {
        while (!Thread.interrupted()) {
            try {
                QueueItem next;
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        queue.wait();
                    }
                    next = queue.removeFirst();
                }

                handleNext(next);
            } catch (InterruptedException ex) {
                Log.i(TAG, "Closing down");
            }
        }
    };

    private URL buildUrl(String target) throws MalformedURLException {
        String token = LinkShareApplication.getToken(context);
        return new URL(LinkShareApplication.BASEURL + target + "?_t=" + token);
    }

    HttpClient(Context context) {
        this.context = context;
    }

    void start() {
        Thread backgroundThread = new Thread(backgroundRunnable, "HttpCache");
        backgroundThread.start();
    }

    <T> void get(String url, Function<InputStream, T> process, Consumer<T> callback) {
        synchronized (queue) {
            queue.add(new QueueItem<>(url, process, callback));
            queue.notifyAll();
        }
    }

     <T> void get(String url, Function<InputStream, T> process) {
        synchronized (queue) {
            queue.add(new QueueItem<>(url, process, null));
            queue.notifyAll();
        }
    }

    private <T> void handleNext(QueueItem<T> next) {
        T stash = null;
        try {
            URL url = buildUrl( next.url) ;

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setSSLSocketFactory(LinkShareApplication.getSSLSocketFactory(context));

            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
            conn.setFixedLengthStreamingMode(0);
            try {
                Log.d(TAG, "Connecting to " + url.toExternalForm());
                conn.connect();
                Log.d(TAG, "Connected, trying to read result");

                try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream())) {
                    stash = next.process.apply(in);
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            Log.w(TAG, "Problem fetching " + next.url, e);
        }

        if (next.callback != null) {
            T result = stash;
            handler.post(() -> {
                if (result != null) {
                    next.callback.accept(result);
                }
            });
        }
    }

    private static class QueueItem<T> {
        final String url;
        final Function<InputStream, T> process;
        final Consumer<T> callback;

        QueueItem(String target, Function<InputStream, T> process, Consumer<T> callback) {
            this.url = target;
            this.process = process;
            this.callback = callback;
        }
    }
}
