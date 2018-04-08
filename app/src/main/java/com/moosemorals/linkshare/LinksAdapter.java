package com.moosemorals.linkshare;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class LinksAdapter implements ListAdapter {

    private final Context parent;
    private final Set<DataSetObserver> observers = new HashSet<>();
    private final List<Link> links = new ArrayList<>();

    LinksAdapter(Context parent) {
        this.parent = parent;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        synchronized (observers) {
            observers.add(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        synchronized (observers) {
            observers.add(observer);
        }
    }

    private void notifyObservers() {
        synchronized (observers) {
            for (DataSetObserver o : observers) {
                o.onChanged();
            }
        }
    }

    @Override
    public int getCount() {
        synchronized (links) {
            return links.size();
        }
    }

    @Override
    public Object getItem(int position) {
        return getLink(position);
    }

    Link getLink(int position) {
        synchronized (links) {
            return links.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup p) {
        Link link;
        synchronized (links) {
            link = links.get(position);
        }

        LayoutInflater inflater = (LayoutInflater) parent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v;
        if (convertView == null) {
            v = inflater.inflate(R.layout.links_row, null);
        } else {
            v = convertView;
        }

        TextView text = v.findViewById(R.id.links_row_title);

        String title = link.getTitle();
        if (title == null) {
            title = link.getUrl();
        }

        text.setText(title);

        return v;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return links.isEmpty();
    }

    void updateLinks() {

        String token = LinkShareApplication.getToken(parent);

        if (token != null) {

            new AsyncGet(parent, "links", result -> {
                if (result.isSuccess()) {
                    try {
                        JSONObject json = result.getResult();
                        if (json.has("success")) {
                            parseLinks(json.getJSONArray("success"));

                        } else {
                            Log.e(TAG, "Server problem getting links: " + json.getString("error") );
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON error getting links", e);
                    }
                } else {
                    Log.e(TAG, "network error getting links", result.getError());
                }
            }).execute("_t", token);
        } else {
            Log.w(TAG, "Can't get links, not logged in");
        }
    }

    private void parseLinks(JSONArray json) throws JSONException {
        synchronized (links) {
            links.clear();

            for (int i = 0; i < json.length(); i += 1) {
                links.add(new Link(json.getJSONObject(i)));
            }

            Log.d(TAG, "Now we've got " + links.size() + " links");
        }
        notifyObservers();
    }

}
