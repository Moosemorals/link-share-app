package com.moosemorals.linkshare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConversationView extends View {
    private static final String TAG = "ConversationView";

    private static final int ICON_SIZE = 64;
    private static final int TEXT_SIZE = 48;

    private TextPaint tp;
    private Paint iconPaint;
    private String user;
    private Rect iconSrc, iconDest;
    private RectF bubbleRect;
    private final List<Link> links = new ArrayList<>();
    private final Map<String, BitmapData> favIcons = new HashMap<>();

    public ConversationView(Context context) {
        super(context);
        init();
    }

    public ConversationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConversationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ConversationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        user = LinkShareApplication.getUserName(getContext());
        bubbleRect = new RectF();
        iconDest = new Rect(0, 0, ICON_SIZE, ICON_SIZE);
        iconSrc = new Rect(0,0,0,0);
        tp = new TextPaint();
        tp.setColor(0xff000000);
        tp.setTextSize(TEXT_SIZE);
        iconPaint = new Paint();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        Log.d(TAG, "Setting size to " + width + "x" + height);

        setMeasuredDimension(width, height);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw");
        int width = canvas.getWidth();
        int offset = 0;
        boolean mine;
        StaticLayout text;
        synchronized (links) {
            for (Link link : links) {
                BitmapData icon;
                String favIconUrl = link.getFavIconUrl();
                synchronized (favIcons) {
                    icon = favIcons.get(favIconUrl);
                }

                Log.d(TAG, "Checking for icon");
                if (icon == null || ( icon.bitmap == null && !icon.tried)) {
                    LinkShareApplication.loadFavIcon(favIconUrl, bm -> {
                        synchronized (favIcons) {
                            favIcons.put(favIconUrl, new BitmapData(bm, true));
                            Log.d(TAG, "Got an icon");
                            invalidate();
                        }
                    });
                }

                mine = user.equals(link.getFrom());
                canvas.save();
                if (mine) {
                    canvas.translate(-ICON_SIZE, offset);
                } else {
                    canvas.translate(ICON_SIZE, offset);
                }

                text = layoutText(link, mine,width - ICON_SIZE);
                text.draw(canvas);
                canvas.restore();

                if (icon != null && icon.bitmap != null) {
                    canvas.save();
                    canvas.translate(0, offset);
                    iconSrc.right = icon.bitmap.getWidth();
                    iconSrc.bottom = icon.bitmap.getHeight();
                    canvas.drawBitmap(icon.bitmap, iconSrc, iconDest, iconPaint);
                }

                offset += text.getHeight();
            }
        }
        Log.d(TAG, "Done with draw");
    }

    private StaticLayout layoutText(Link link, boolean mine, int width) {
        String title = link.getDisplayText();
        return StaticLayout.Builder
                .obtain(
                        title,
                        0,
                        title.length(),
                        tp,
                        width)
                .setAlignment(mine ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL)
                .build();
    }

    void loadLinks() {

        String token = LinkShareApplication.getToken(getContext());

        if (token != null) {

            new AsyncGet(getContext(), "links", result -> {
                if (result.isSuccess()) {
                    try {
                        JSONObject json = result.getResult();
                        if (json.has("success")) {
                            Log.d(TAG, "Got some links");
                            parseLinks(json.getJSONArray("success"));
                            invalidate();
                        } else {
                            Log.e(TAG, "Server problem getting links: " + json.getString("error"));
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
    }

    private static class BitmapData {
        Bitmap bitmap;
        boolean tried;
        BitmapData(Bitmap bitmap, boolean tried) {
            this.bitmap = bitmap;
            this.tried = tried;
        }
   }

}
