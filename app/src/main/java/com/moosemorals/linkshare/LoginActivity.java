package com.moosemorals.linkshare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.Consumer;

public final class LoginActivity extends Activity implements Consumer<AsyncResult<JSONObject>> {

    private static final String TAG = "LoginActivity";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        if (LinkShareApplication.getSharedPreferences(this).contains(LinkShareApplication.TOKEN_KEY)) {
            startListActivity();
        }

    }

    public void doLogin(View v) {
        String username = getValue(R.id.login_username);
        String password = getValue(R.id.login_password);
        String device = getValue(R.id.login_device);

        setEnabled(false);

        new AsyncPost(this,"login", this)
                .execute("username", username,
                        "password", password,
                        "device", device);
    }

    @Override
    public void accept(AsyncResult<JSONObject> result) {
        if (result.isSuccess()) {
            JSONObject json = result.getResult();

            String username, token;
            try {
                if (json.has("success")) {
                    JSONObject success = json.getJSONObject("success");
                    username = success.getString("user");
                    token = success.getString("token");
                    onSuccess(username, token);
               } else {
                    Log.w(TAG, "Login failed: " + json.getString("error"));
                }

            } catch (JSONException e) {
                Log.e(TAG, "JSON problem", e);
            }
        }
        setEnabled(true);
    }

    private void onSuccess(String username, String token) {
        LinkShareApplication.getSharedPreferences(this)
                .edit()
                .putString(LinkShareApplication.USERNAME_KEY, username)
                .putString(LinkShareApplication.TOKEN_KEY, token)
                .apply();
        startListActivity();
    }

    private void startListActivity() {
        Intent intent = new Intent(this, LinksActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        this.finish();
    }

    private void setEnabled(boolean enabled) {
        LinearLayout layout = findViewById(R.id.login_layout);
        for (int i = 0; i < layout.getChildCount(); i += 1) {
            View v = layout.getChildAt(i);
            v.setEnabled(enabled);
        }
    }

    private String getValue(int id) {
        TextView text = findViewById(id);
        return text.getText().toString();
    }
}
