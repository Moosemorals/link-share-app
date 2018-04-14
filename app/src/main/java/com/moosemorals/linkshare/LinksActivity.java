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

        conversationView = findViewById(R.id.links_convo);
        TabBar tabBar = findViewById(R.id.links_tabs);

        tabBar.addTab("Osric");
        tabBar.addTab("Moose");
        tabBar.addTab("Shinju");
        tabBar.addTab("Search");
        tabBar.addTab("Settings");

        tabBar.addTabChangedListener(newTabName -> conversationView.setSharedWith(newTabName));

        conversationView.setSharedWith("Osric");

        conversationView.setHttpClient(((LinkShareApplication)getApplication()).getHttpClient());
        conversationView.setFavIconCache(((LinkShareApplication)getApplication()).getFavIconCache());
    }

    @Override
    protected void onResume() {
        super.onResume();
        conversationView.loadLinks();
    }
}
