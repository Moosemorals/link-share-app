package com.moosemorals.linkshare;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

final class Link {
    private static final String TAG = "Link";
    private final String url;
    private final String title;
    private final String favIconUrl;
    private final String from;
    private final long created;
    private final String to;

    Link(JSONObject json) throws JSONException {
        this.url = json.getString("url");
        this.title = json.getString("title");
        this.from = json.getString("from");
        this.to = json.getString("to");
        if (json.has("favIconURL")) {
            this.favIconUrl = json.getString("favIconURL");
        } else {
            this.favIconUrl = null;
        }
        this.created = json.getLong("created");
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getFavIconUrl() {
        return favIconUrl;
    }

    public String getDisplayText() {
        return title != null ? title : url;
    }

    public boolean isPartOfConversation(String f, String t) {
        return (from.equals(f) && to.equals(t)) || (from.equals(t) && to.equals(f));
    }

    @Override
    public String toString() {
        return "Link{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", favIconUrl='" + favIconUrl + '\'' +
                '}';
    }

    public String getFrom() {
        return from;
    }

    public long getCreated() {
        return created;
    }

    public String getTo() {
        return to;
    }
}
