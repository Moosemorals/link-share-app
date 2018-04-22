package com.moosemorals.linkshare;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public final class CloudIDService extends FirebaseInstanceIdService {

    private static final String TAG = "CloudIDService";

    private LinkShareApplication app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = (LinkShareApplication)getApplication();
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.

        if (app.isLoggedIn()) {

            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "Refreshed token: " + refreshedToken);

            // If you want to send messages to this application instance or
            // manage this apps subscriptions on the server side, send the
            // Instance ID token to your app server.
            sendRegistrationToServer(refreshedToken);
        }
    }

    private void sendRegistrationToServer(String refreshedToken) {
        String body = LinkShareApplication.paramEncode("token", refreshedToken);

        app.getHttpClient().post("google", body, inputStream -> {
            try {
                JSONObject result = LinkShareApplication.readJsonFromStream(inputStream);
                if (result.has("success")) {
                    Log.d(TAG, "Registered token");
                } else {
                    Log.w(TAG, "Could not register token: " + result.toString());
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Could not register token", e);
            }
            return  null;
        }, null);
    }
}
