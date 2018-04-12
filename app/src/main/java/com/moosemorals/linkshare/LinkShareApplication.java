package com.moosemorals.linkshare;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public final class LinkShareApplication extends Application {

    private static final String TAG = "LinkShareApplication";

    static final String BASEURL = "https://moosemorals.com/link-share/";
    static final String USERNAME_KEY = "com.moosemorals.linkshare.username";
    static final String TOKEN_KEY = "com.moosemorals.linkshare.token";

    private static final String PREFS_NAME = LinkShareApplication.class.getName();

    private static FavIconCache favIconCache;
    private HttpClient httpClient;

    @Override
    public void onCreate() {
        super.onCreate();
        favIconCache = new FavIconCache();
        favIconCache.start();

        httpClient = new HttpClient(this);
        httpClient.start();
    }

    HttpClient getHttpClient() {
        return httpClient;
    }

    static void loadFavIcon(String url, Consumer<Bitmap> callback) {
        favIconCache.loadIcon(url, callback);
    }

    static String paramEncode(String... param) {
        if (param.length % 2 != 0) {
            throw new IllegalArgumentException("Parameter key/values must come in pairs");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < param.length; i += 2) {
            String key = param[i];
            String value = param[i + 1];

            if (i != 0) {
                result.append("&");
            }

            try {
                result.append(URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Java doesn't know UTF-8");
            }
        }
        return result.toString();
    }

    static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    static String getToken(Context context) {
        return getSharedPreferences(context).getString(TOKEN_KEY, null);
    }

    static String getUserName(Context context) {
        return getSharedPreferences(context).getString(USERNAME_KEY, null);
    }

    // Adapted from https://stackoverflow.com/q/44483431/195833
    static SSLSocketFactory getSSLSocketFactory(Context context) throws IOException {
        if (context == null) {
            throw new IOException("No context");
        }

        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
            Certificate ca;
            try (InputStream caInput = context.getResources().openRawResource(R.raw.letsencrypt_x3)) {
                ca = cf.generateCertificate(caInput);
                Log.d(TAG, "ca=" + ((X509Certificate) ca).getSubjectDN());
            }

            // Create a KeyStore containing our trusted CAs
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return sslContext.getSocketFactory();

        } catch (CertificateException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new IOException(e);
        }
    }

    static JSONObject readStream(Reader in) throws JSONException, IOException {
        char[] buf = new char[4096];
        int read;
        StringBuilder raw = new StringBuilder();
        while ((read = in.read(buf)) != -1) {
            raw.append(buf, 0, read);
        }

        Log.d(TAG,"Read: " + raw.toString());
        return new JSONObject(raw.toString());
    }
}
