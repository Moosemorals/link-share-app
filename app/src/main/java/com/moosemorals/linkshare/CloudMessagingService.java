package com.moosemorals.linkshare;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public final class CloudMessagingService extends FirebaseMessagingService {

    private final static String TAG = "CloudMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
       Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }
    }
}
