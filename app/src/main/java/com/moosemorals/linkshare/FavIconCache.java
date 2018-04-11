package com.moosemorals.linkshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
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

final class FavIconCache {
    private static final String TAG = "FavIconCache";
    private final Map<String, WeakReference<Bitmap>> cache = new HashMap<>();
    private final LinkedList<QueueItem> queue = new LinkedList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
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

    private void handleNext(QueueItem next) {
        Bitmap bm = getBitmap(next.url);
        if (bm != null) {
            doOnUiThread(() -> next.callback.accept(bm));
        } else {
            new IconFetcher().doFetch(next.url, bitmap -> {
                storeBitmap(next.url, bitmap);
                doOnUiThread(() -> next.callback.accept(bitmap));
            });
        }
    }

    private void doOnUiThread(Runnable r) {
        handler.post(r);
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

    private static class IconFetcher {

        private void doFetch(String target, Consumer<Bitmap> callback) {
            try {
                URL url = new URL(target);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setDoOutput(false);
                conn.setFixedLengthStreamingMode(0);
                try {
                    Log.d(TAG, "Connecting");
                    conn.connect();
                    Log.d(TAG, "Connected, trying to read result");

                    try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream())) {
                        callback.accept(BitmapFactory.decodeStream(in));
                    }
                } finally {
                    conn.disconnect();
                }

            } catch (IOException e) {
                Log.w(TAG, "Can't get FavIcon: " + target, e);
                callback.accept(null);
            }
        }

    }
}
