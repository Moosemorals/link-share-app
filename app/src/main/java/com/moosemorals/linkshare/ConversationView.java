package com.moosemorals.linkshare;

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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConversationView extends View {
    private static final String TAG = "ConversationView";

    private static final int ICON_SIZE = 64;
    private static final float BUBBLE_MARGIN = ICON_SIZE / 4.0f;
    private static final int TEXT_SIZE = 48;
    private final List<Link> links = new ArrayList<>();
    private final Map<String, BitmapData> favIcons = new HashMap<>();
    private HttpClient httpClient;
    private TextPaint tp;
    private Paint iconPaint;
    private String user;
    private Rect iconSrc, iconDest;
    private RectF bubbleRect;

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

    void setHttpClient(HttpClient client) {
        this.httpClient = client;
    }

    private void init() {
        user = LinkShareApplication.getUserName(getContext());
        bubbleRect = new RectF();
        iconDest = new Rect(0, 0, ICON_SIZE, ICON_SIZE);
        iconSrc = new Rect(0, 0, 0, 0);
        tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xff000000);
        tp.setTextSize(TEXT_SIZE);
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(0x88e0e0e0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw");
        final int width = canvas.getWidth();
        int offset = ICON_SIZE;
        int lastOffset = 0;
        float textWidth = 0;
        boolean mine, oldMine = false;
        StaticLayout text;

        bubbleRect.top = ICON_SIZE / 2;

        synchronized (links) {
            for (int i = 0, linksSize = links.size(); i < linksSize; i++) {
                Link link = links.get(i);

                mine = user.equals(link.getFrom());

                // Need to do text first because we need the width for the
                // bubble
                text = layoutText(link, mine, width - (ICON_SIZE * 3));

                if (text.getLineWidth(0) > textWidth) {
                    textWidth = text.getLineWidth(0);
                }

                // Bubble
                if (i > 0 && mine != oldMine) {
                    canvas.save();
                    if (oldMine) {
                        canvas.translate(width - (text.getWidth() + 2f * ICON_SIZE), lastOffset + 2 * BUBBLE_MARGIN);
                    } else {
                        canvas.translate(2 * BUBBLE_MARGIN, lastOffset + 2 * BUBBLE_MARGIN);
                    }
                    bubbleRect.top = 0;
                    bubbleRect.left = 0;
                    bubbleRect.right = textWidth + (ICON_SIZE * 2f);
                    bubbleRect.bottom = (offset - lastOffset) - BUBBLE_MARGIN;

                    canvas.drawRoundRect(bubbleRect, ICON_SIZE / 4f, ICON_SIZE / 4f, iconPaint);

                    canvas.restore();
                    lastOffset = offset;
                    offset += ICON_SIZE;
                    textWidth = 0;
                }

                // FavIcon
                canvas.save();
                if (mine) {
                    canvas.translate(width - (ICON_SIZE * 2), offset);
                } else {
                    canvas.translate(ICON_SIZE, offset);
                }

                BitmapData icon = getIcon(link);
                canvas.drawRect(iconDest, iconPaint);

                if (icon != null && icon.bitmap != null) {
                    iconSrc.right = icon.bitmap.getWidth();
                    iconSrc.bottom = icon.bitmap.getHeight();
                    canvas.drawBitmap(icon.bitmap, iconSrc, iconDest, iconPaint);
                }
                canvas.restore();

                // Text
                canvas.save();

                if (mine) {
                    canvas.translate(ICON_SIZE, offset);
                } else {
                    canvas.translate(ICON_SIZE * 2, offset);
                }

                text.draw(canvas);
                canvas.restore();

                // Setup for next loop
                oldMine = mine;
                offset += text.getHeight();
            }
        }
        Log.d(TAG, "Done with draw");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        Log.d(TAG, "Setting size to " + width + "x" + height);

        setMeasuredDimension(width, height);
    }

    private BitmapData getIcon(Link link) {
        BitmapData icon;
        final String favIconUrl = link.getFavIconUrl();
        synchronized (favIcons) {
            icon = favIcons.get(favIconUrl);
        }

        Log.d(TAG, "Checking for icon");
        if (icon == null || (icon.bitmap == null && !icon.tried)) {
            LinkShareApplication.loadFavIcon(favIconUrl, bm -> {
                synchronized (favIcons) {
                    favIcons.put(favIconUrl, new BitmapData(bm, true));
                    Log.d(TAG, "Got an icon");
                    invalidate();
                }
            });
        }
        return icon;
    }

    private StaticLayout layoutText(Link link, boolean mine, int width) {
        String title = link.getDisplayText();
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(
                title,
                0,
                title.length(),
                tp,
                width);
        builder.setAlignment(mine ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL);
        builder.setMaxLines(1);
        builder.setEllipsize(TextUtils.TruncateAt.END);

        return builder.build();
    }

    void loadLinks() {
        httpClient.get("links", in -> {
            try {
                JSONObject json = LinkShareApplication.readStream(new InputStreamReader(in));
                if (json.has("success")) {
                    Log.d(TAG, "Got some links");
                    return parseLinks(json.getJSONArray("success"));
                } else {
                    Log.e(TAG, "Server problem getting links: " + json.getString("error"));
                }
            } catch (JSONException | IOException e) {
                Log.w(TAG, "Problem fetching links", e);
            }
            return null;
        }, l -> {
            synchronized (links) {
                links.clear();
                links.addAll(l);
                invalidate();
            }
        });

    }

    private List<Link> parseLinks(JSONArray json) throws JSONException {
        List<Link> result = new ArrayList<>();
        for (int i = 0; i < json.length(); i += 1) {
            result.add(new Link(json.getJSONObject(i)));
        }
        return result;
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
