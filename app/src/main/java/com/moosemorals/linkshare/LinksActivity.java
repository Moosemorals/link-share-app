package com.moosemorals.linkshare;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public final class LinksActivity extends Activity {
    private static final String TAG = "LinksActivity";

    private ConversationView conversationView;
    private TabBar tabBar;
    private String username;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.links);

        username = LinkShareApplication.getSharedPreferences(this).getString(LinkShareApplication.USERNAME_KEY, null);

        if (username == null) {
            throw new RuntimeException("Shouldn't get here with a null username");
        }

        conversationView = findViewById(R.id.links_convo);
        tabBar = findViewById(R.id.links_tabs);

        tabBar.addTab("Search");
        tabBar.addTab("Settings");

        tabBar.addTabChangedListener(newTabName -> conversationView.setSharedWith(newTabName));
        conversationView.setFavIconCache(((LinkShareApplication) getApplication()).getFavIconCache());
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUsers();
        loadLinks();
    }

    private void loadUsers() {
        Backend client = getHttpClient();

        client.get("users", in -> {            // Off main thread
            try {
                JSONObject json = LinkShareApplication.readStream(new InputStreamReader(in));
                if (json.has("success")) {
                    return parseUsers(json.getJSONArray("success"));
                } else {
                    Log.e(TAG, "Server problem getting links: " + json.getString("error"));
                }
            } catch (JSONException | IOException e) {
                Log.w(TAG, "Problem fetching links", e);
            }
            return null;
        }, users -> {
            for (User u : users) {
                String name = u.getName();
                tabBar.addFirstTab(name);
            }
            tabBar.setActiveTab(username);
        });

    }

    private Backend getHttpClient() {
        return ((LinkShareApplication) getApplication()).getHttpClient();
    }

    private List<User> parseUsers(JSONArray json) throws JSONException {
        List<User> result = new LinkedList<>();

        for (int i = 0; i < json.length(); i += 1) {
            result.add(new User(json.getJSONObject(i)));
        }

        return result;
    }

    private void loadLinks() {
        getHttpClient().get("links", in -> {
            // Off main thread
            try {
                JSONObject json = LinkShareApplication.readStream(new InputStreamReader(in));
                if (json.has("success")) {
                    List<Link> parsed = parseLinks(json.getJSONArray("success"));

                    parsed.sort(Comparator.comparingLong(Link::getCreated));

                    return parsed;
                } else {
                    Log.e(TAG, "Server problem getting links: " + json.getString("error"));
                }
            } catch (JSONException | IOException e) {
                Log.w(TAG, "Problem fetching links", e);
            }
            return null;
        }, links -> conversationView.setLinks(links) );
    }

    private List<Link> parseLinks(JSONArray json) throws JSONException {
        List<Link> result = new LinkedList<>();
        for (int i = 0; i < json.length(); i += 1) {
            result.add(new Link(json.getJSONObject(i)));
        }
        return result;
    }

}
