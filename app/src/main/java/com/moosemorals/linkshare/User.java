package com.moosemorals.linkshare;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public final class User {

    private final String name;
    private final List<String> devices;

    User(JSONObject json) throws JSONException {
        name = json.getString("name");

        devices = new LinkedList<>();
        JSONArray array = json.getJSONArray("devices");
        for (int index = 0; index < array.length(); index += 1) {
            devices.add(array.getString(index));
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getDevices() {
        return devices;
    }
}
