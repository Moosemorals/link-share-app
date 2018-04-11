package com.moosemorals.linkshare;

import org.json.JSONException;
import org.json.JSONObject;

final class Link {

    private final String url;
    private final String title;
    private final String favIconUrl;
    private final String from;

    Link(JSONObject json) throws JSONException {
        this.url = json.getString("url");
        this.title = json.getString("title");
        this.from = json.getString("from");
        if (json.has("favIconURL")) {
            this.favIconUrl = json.getString("favIconURL");
        } else {
            this.favIconUrl = null;
        }
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
}
