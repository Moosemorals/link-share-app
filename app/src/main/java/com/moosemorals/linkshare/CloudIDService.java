package com.moosemorals.linkshare;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public final class CloudIDService extends FirebaseInstanceIdService {

    private static final String TAG = "CloudIDService";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String refreshedToken) {
        new AsyncPost(this, "firebaseId",
                result -> {
                    if (result.isSuccess()) {
                        Log.d(TAG, "Got json result " + result.getResult().toString());
                    } else {
                        Log.w(TAG, "Got an error", result.getError());
                    }
                }
        ).execute("token", refreshedToken);
    }
}
