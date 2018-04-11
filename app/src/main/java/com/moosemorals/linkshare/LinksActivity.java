package com.moosemorals.linkshare;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;


public class LinksActivity extends Activity {
    private static final String TAG = "LinksActivity";

    private ConversationView conversationView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.links);


        String username = LinkShareApplication.getSharedPreferences(this).getString(LinkShareApplication.USERNAME_KEY, null);

        if (username == null) {
            throw new RuntimeException("Shouldn't get here with a null username");
        }

        TextView text = findViewById(R.id.links_hello);

        text.setText("Hello " + username);

        conversationView = findViewById(R.id.links_convo);


    }

    @Override
    protected void onResume() {
        super.onResume();
        conversationView.loadLinks();
    }
}
