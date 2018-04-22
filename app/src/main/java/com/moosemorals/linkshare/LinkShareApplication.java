package com.moosemorals.linkshare;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public final class LinkShareApplication extends Application {

    static final String BASEURL = "https://moosemorals.com/link-share/";
    static final String USERNAME_KEY = "com.moosemorals.linkshare.username";
    static final String TOKEN_KEY = "com.moosemorals.linkshare.token";
    private static final String TAG = "LinkShareApplication";
    private static final String PREFS_NAME = LinkShareApplication.class.getName();

    private FavIconCache favIconCache;
    private Backend httpClient;
    private SSLSocketFactory sslSocketFactory = null;

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

    static JSONObject readJsonFromStream(InputStream in) throws JSONException, IOException {
        return readJsonFromStream(new InputStreamReader(in, "UTF-8"));
    }

    static JSONObject readJsonFromStream(Reader in) throws JSONException, IOException {
        char[] buf = new char[4096];
        int read;
        StringBuilder raw = new StringBuilder();
        while ((read = in.read(buf)) != -1) {
            raw.append(buf, 0, read);
        }

        Log.d(TAG, "Read: " + raw.toString());
        return new JSONObject(raw.toString());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        favIconCache = new FavIconCache();
        favIconCache.start();

        try {
            httpClient = new Backend(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        httpClient.start();
    }

    Backend getHttpClient() {
        return httpClient;
    }

    FavIconCache getFavIconCache() {
        return favIconCache;
    }

    String getToken() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(TOKEN_KEY, null);
    }

    String getUserName() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(USERNAME_KEY, null);
    }

    void completeLogin(JSONObject json) throws JSONException {

        String username = json.getString("user");
        String token = json.getString("token");

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(LinkShareApplication.USERNAME_KEY, username)
                .putString(LinkShareApplication.TOKEN_KEY, token)
                .apply();

    }

    boolean isLoggedIn() {
        return getUserName() != null;
    }

    // Adapted from https://stackoverflow.com/q/44483431/195833
    SSLSocketFactory getSSLSocketFactory() throws IOException {
        if (sslSocketFactory == null) {

            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                // From https://www.washington.edu/itconnect/security/ca/load-der.crt
                Certificate ca;
                try (InputStream caInput = getResources().openRawResource(R.raw.letsencrypt_x3)) {
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

                sslSocketFactory = sslContext.getSocketFactory();

            } catch (CertificateException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                throw new IOException(e);
            }
        }

        return sslSocketFactory;
    }
}
