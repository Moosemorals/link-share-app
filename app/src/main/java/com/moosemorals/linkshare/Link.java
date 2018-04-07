package com.moosemorals.linkshare;

import org.json.JSONException;
import org.json.JSONObject;

final class Link {

    private final String url;
    private final String title;

    Link(JSONObject json) throws JSONException {
        this.url = json.getString("url");
        this.title = json.getString("title");
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }
}
