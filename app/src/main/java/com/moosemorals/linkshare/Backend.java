package com.moosemorals.linkshare;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

final class Backend {
    private static final String TAG = "Backend";
    private final Map<String, WeakReference<Bitmap>> cache = new HashMap<>();
    private final LinkedList<QueueItem> queue = new LinkedList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinkShareApplication parent;
    private final SSLSocketFactory sslSocketFactory;
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
        String token = parent.getToken();
        return new URL(LinkShareApplication.BASEURL + target + "?_t=" + token);
    }

    Backend(LinkShareApplication parent) throws IOException {
        this.parent = parent;
        sslSocketFactory = parent.getSSLSocketFactory();
    }

    void start() {
        Thread backgroundThread = new Thread(backgroundRunnable, "HttpCache");
        backgroundThread.start();
    }

    <T> void get(String url, Function<InputStream, T> process, Consumer<T> callback) {
        synchronized (queue) {
            queue.add(new QueueItem<>("GET", url, null, process, callback));
            queue.notifyAll();
        }
    }

     <T> void get(String url, Function<InputStream, T> process) {
        synchronized (queue) {
            queue.add(new QueueItem<>("GET", url, null, process, null));
            queue.notifyAll();
        }
    }

    <T> void post(String url, String body, Function<InputStream, T> process, Consumer<T> callback) {
        synchronized (queue) {
            queue.add(new QueueItem<>("POST", url, body, process, callback));
            queue.notifyAll();
        }
    }

    private <T> void handleNext(QueueItem<T> next) {
        T stash = null;

        boolean sendingBody = next.method.equals("POST") && next.body != null;
        try {
            URL url = buildUrl( next.url) ;

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslSocketFactory);
            conn.setRequestMethod(next.method);

            if (sendingBody) {
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(next.body.length());
            } else {
                conn.setDoOutput(false);
                conn.setFixedLengthStreamingMode(0);
            }
            try {
                conn.connect();

                if (sendingBody) {
                    try (BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream())) {
                        out.write(next.body.getBytes("UTF-8"));
                        out.flush();
                    }
                }

                try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream())) {
                    stash = next.process.apply(in);
                } catch (IOException ex) {
                    // Sigh. This is how the system signifies a non-200 response.
                    try (BufferedInputStream in = new BufferedInputStream(conn.getErrorStream())) {
                        stash = next.process.apply(in);
                    }
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
                next.callback.accept(result);
            });
        }
    }

    private static class QueueItem<T> {
        final String url;
        final String method;
        final Function<InputStream, T> process;
        final Consumer<T> callback;
        final String body;

        QueueItem(String method, String target, String body, Function<InputStream, T> process, Consumer<T> callback) {
            this.method = method;
            this.url = target;
            this.body = body;
            this.process = process;
            this.callback = callback;
        }
    }
}
