package com.moosemorals.linkshare;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.net.ssl.HttpsURLConnection;

public final class ShareActivity extends Activity {

    private static final String TAG = "ShareActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.links);

        Intent intent = getIntent();

        String action = intent.getAction();
        if (action == null) {
            action = "";
        }

        try {
            XMLReader parser = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
        } catch (SAXException e) {
            Log.e(TAG, "Problem with the parser");
        }


        switch (action) {
            case Intent.ACTION_SEND:
                String rawLink = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (rawLink != null && Patterns.WEB_URL.matcher(rawLink).matches()) {
                    Uri uri = Uri.parse(rawLink);
                    Log.d(TAG, "Got a link: " + uri.toString());
                }
                break;
        }
    }

    private void handleLink(Uri uri) {




    }

}
