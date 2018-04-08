package com.moosemorals.linkshare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

final class FavIconCache {
    private static final String TAG = "FavIconCache";
    private final Context context;
    private final Map<String, WeakReference<Bitmap>> cache = new HashMap<>();
    private final LinkedList<QueueItem> queue = new LinkedList<>();
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

    FavIconCache(Context context) {
        this.context = context;
    }

    private void handleNext(QueueItem next) {
        Bitmap bm = getBitmap(next.url);
        if (bm != null) {
            next.callback.accept(bm);
        } else {
            new IconFetcher(context, bitmap -> {
                storeBitmap(next.url, bitmap);
                next.callback.accept(bitmap);
            }).execute(next.url);
        }
    }

    private void storeBitmap(String url, Bitmap bitmap) {
        synchronized (cache) {
            cache.put(url, new WeakReference<>(bitmap));
        }
    }

    private Bitmap getBitmap(String url) {
        synchronized (cache) {
            WeakReference<Bitmap> bm = cache.get(url);
            if (bm != null) {
                Bitmap result = bm.get();
                if (result == null) {
                    cache.remove(url);
                }
                return result;
            }
        }
        return null;
    }

    void start() {
        Thread backgroundThread = new Thread(backgroundRunnable, "FavIconCache");
        backgroundThread.start();
    }

    void loadIcon(String url, Consumer<Bitmap> callback) {
        synchronized (queue) {
            queue.add(new QueueItem(url, callback));
            queue.notifyAll();
        }
    }

    private static class QueueItem {
        final String url;
        final Consumer<Bitmap> callback;

        QueueItem(String url, Consumer<Bitmap> callback) {
            this.url = url;
            this.callback = callback;
        }
    }

    private static class IconFetcher extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<Context> context;
        private final Consumer<Bitmap> callback;

        IconFetcher(Context context, Consumer<Bitmap> callback) {
            this.context = new WeakReference<>(context);
            this.callback = callback;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL target = new URL(strings[0]);

                HttpURLConnection conn = (HttpURLConnection) target.openConnection();

       //         conn.setSSLSocketFactory(LinkShareApplication.getSSLSocketFactory(context.get()));

                conn.setRequestMethod("GET");
                conn.setDoOutput(false);
                conn.setFixedLengthStreamingMode(0);
                try {
                    Log.d(TAG, "Connecting");
                    conn.connect();
                    Log.d(TAG, "Connected, trying to read result");
                    try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream())) {
                        return BitmapFactory.decodeStream(in);
                    }
                } finally {
                    conn.disconnect();
                }

            } catch (IOException e) {
                Log.w(TAG, "Can't get FavIcon: " + strings[0], e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            callback.accept(bitmap);
        }
    }
}
