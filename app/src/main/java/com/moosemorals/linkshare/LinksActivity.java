package com.moosemorals.linkshare;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;


public class LinksActivity extends Activity {
    private static final String TAG = "LinksActivity";
    private LinksAdapter linksAdapter;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.links);


        String username = LinkShareApplication.getSharedPrefrences(this).getString(LinkShareApplication.USERNAME_KEY, null);

        if (username == null) {
            throw new RuntimeException("Shouldn't get here with a null username");
        }

        TextView text = findViewById(R.id.links_hello);

        text.setText("Hello " + username);


        ListView linksList = findViewById(R.id.links_list);
        linksAdapter = new LinksAdapter(this);

        linksList.setOnItemClickListener((parent, view, position, id) -> {
            Link link = linksAdapter.getLink(position);

            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link.getUrl())));
        });

        linksList.setAdapter(linksAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (linksAdapter != null) {
            linksAdapter.updateLinks();
        }
    }
}
