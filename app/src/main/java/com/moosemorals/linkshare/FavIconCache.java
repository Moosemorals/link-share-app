package com.moosemorals.linkshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class FavIconCache {
    private static final String TAG = "FavIconCache";
    private final Map<String, CacheItem> cache = new HashMap<>();
    private final LinkedList<String> queue = new LinkedList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable backgroundRunnable = () -> {
        while (!Thread.interrupted()) {
            try {
                String next;
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

    private void handleNext(String next) {
        CacheItem cacheItem;
        synchronized (cache) {
            cacheItem = cache.get(next);
        }

        if (cacheItem.fetched) {
            doOnUiThread(() -> {
                for (Iterator<BitmapConsumer> iterator = cacheItem.consumers.iterator(); iterator.hasNext(); ) {
                    BitmapConsumer consumer = iterator.next();
                    consumer.accept(cacheItem.bitmap);
                    iterator.remove();
                }
            });

        } else {
            cacheItem.bitmap = doFetch(next);
            synchronized (cache) {
                cacheItem.fetched = true;
            }
            doOnUiThread(() -> {
                for (Iterator<BitmapConsumer> iterator = cacheItem.consumers.iterator(); iterator.hasNext(); ) {
                    BitmapConsumer consumer = iterator.next();
                    consumer.accept(cacheItem.bitmap);
                    iterator.remove();
                }
            });
        }
    }

    private void doOnUiThread(Runnable r) {
        handler.post(r);
    }

    void start() {
        Thread backgroundThread = new Thread(backgroundRunnable, "FavIconCache");
        backgroundThread.start();
    }

    void loadIcon(String url, BitmapConsumer callback) {
        if (url == null || url.equals("undefined")) {
            doOnUiThread(() -> callback.accept(null));
            return;
        }

        synchronized (cache) {
            CacheItem item = cache.get(url);
            if (item == null) {
                synchronized (queue) {
                    queue.add(url);
                    queue.notifyAll();
                }
                cache.put(url, new CacheItem());
            } else if (item.fetched) {
                doOnUiThread(() -> callback.accept(item.bitmap));
            } else {
                item.consumers.add(callback);
            }
        }
    }

    private Bitmap doFetch(String target) {
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
                    return BitmapFactory.decodeStream(in);
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            Log.w(TAG, "Can't get FavIcon: " + target, e);
        }
        return null;
    }

    interface BitmapConsumer extends Consumer<Bitmap> {
    }

    private static class CacheItem {
        volatile boolean fetched = false;
        Bitmap bitmap;
        List<BitmapConsumer> consumers = new LinkedList<>();
    }
}
