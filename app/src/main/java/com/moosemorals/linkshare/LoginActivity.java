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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public final class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";
    private static final Class NEXT_ACTIVITY = WebLinksActivity.class;

    private LinkShareApplication app;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (LinkShareApplication) getApplication();

        if (app.isLoggedIn()) {
            startNextActivity();
        } else {
            setContentView(R.layout.login);
        }

    }

    public void doLogin(View v) {
        String username = getValue(R.id.login_username);
        String password = getValue(R.id.login_password);
        String device = getValue(R.id.login_device);

        setEnabled(false);

        String body = LinkShareApplication.paramEncode("username", username,
                        "password", password,
                        "device", device);

        app.getHttpClient().post("login", body, inputStream -> {
            try {
                JSONObject json = LinkShareApplication.readJsonFromStream(inputStream);

                if (json.has("success")) {
                    app.completeLogin(json.getJSONObject("success"));
                    startNextActivity();
                }
            } catch (JSONException | IOException e) {
                Log.w(TAG, "Login problem", e);
            }
            return null;
        }, x -> setEnabled(true));
    }

    private void startNextActivity() {
        Intent intent = new Intent(this, NEXT_ACTIVITY);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        finish();
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
