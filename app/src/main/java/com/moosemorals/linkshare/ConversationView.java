package com.moosemorals.linkshare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ConversationView extends View {
    private static final String TAG = "ConversationView";

    private static final int ICON_SIZE = 64;
    private static final int LINE_SPACING = ICON_SIZE / 4;
    private static final float BUBBLE_MARGIN = ICON_SIZE / 3f;
    private static final float BUBBLE_RADIUS = 2 * ICON_SIZE / 3f;
    private static final int WINDOW_MARGIN = ICON_SIZE / 4;

    private static final int TEXT_SIZE = 48;
    private final List<Link> links = new ArrayList<>();
    private final Map<String, BitmapData> favIcons = new HashMap<>();
    private final List<LinkData> linkData = new LinkedList<>();
    private final List<LinkData> group = new LinkedList<>();
    private HttpClient httpClient;
    private FavIconCache favIconCache;
    private TextPaint tp;
    private Paint iconPaint, bubblePaint;
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

    void setFavIconCache(FavIconCache cache) {
        this.favIconCache = cache;
    }

    private void init() {
        user = LinkShareApplication.getUserName(getContext());
        bubbleRect = new RectF();
        iconDest = new Rect(0, 0, ICON_SIZE, ICON_SIZE);
        iconSrc = new Rect(0, 0, 0, 0);

        tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xff000000);
        tp.setTextSize(TEXT_SIZE);

        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setColor(0xffa0f0a0);

        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(0xffe0e0e0);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw");
        final int width = canvas.getWidth() - (WINDOW_MARGIN * 2);
        int offset = ICON_SIZE;

        linkData.clear();

        synchronized (links) {
            if (links.isEmpty()) {
                Log.i(TAG, "Nothing to display");
                return;
            }

            for (Link link : links) {
                boolean mine = user.equals(link.getFrom());
                linkData.add(new LinkData(
                        link,
                        mine,
                        layoutText(link, mine, width - (ICON_SIZE * 3))
                ));
            }
        }

        canvas.translate(WINDOW_MARGIN, WINDOW_MARGIN);

        Log.d(TAG, "Displaying: " + linkData.size());
        do {
            group.clear();

            LinkData next = linkData.remove(0);

            float groupWidth = next.textLayout.getLineWidth(0);
            int groupHeight = next.textLayout.getHeight();
            boolean mine = next.mine;
            group.add(next);

            while (!linkData.isEmpty() && linkData.get(0).mine == mine) {
                next = linkData.remove(0);
                groupHeight += next.textLayout.getHeight() + LINE_SPACING;
                if (next.textLayout.getLineWidth(0) > groupWidth) {
                    groupWidth = next.textLayout.getLineWidth(0);
                }
                group.add(next);
            }

            float textWidth = groupWidth;
            groupWidth += ICON_SIZE + BUBBLE_MARGIN * 2;

            // Draw bubble
            canvas.save();
            if (mine) {
                canvas.translate(width - groupWidth, offset - BUBBLE_MARGIN);
            } else {
                canvas.translate(0, offset - BUBBLE_MARGIN);
            }

            bubbleRect.set(0, 0, groupWidth, groupHeight + BUBBLE_MARGIN * 2);
            canvas.drawRoundRect(bubbleRect, BUBBLE_RADIUS, BUBBLE_RADIUS, bubblePaint);

            canvas.translate(BUBBLE_MARGIN, BUBBLE_MARGIN);
            for (LinkData d : group) {
                BitmapData bitmapData = getIcon(d.link);
                canvas.save();
                if (mine) {
                    canvas.translate(textWidth, 0);
                }

                if (bitmapData != null && bitmapData.bitmap != null) {
                    Bitmap icon = bitmapData.bitmap;
                    iconSrc.set(0, 0, icon.getWidth(), icon.getHeight());
                    canvas.drawBitmap(icon, iconSrc, iconDest, iconPaint);
                } else {
                    canvas.drawRect(iconDest, iconPaint);
                }

                canvas.restore();
                canvas.save();
                if (!mine) {
                    canvas.translate(ICON_SIZE, 0);
                }
                d.textLayout.draw(canvas);
                canvas.restore();

                canvas.translate(0, d.textLayout.getHeight() + LINE_SPACING);
            }

            offset += groupHeight + BUBBLE_MARGIN * 3;
            canvas.restore();
        } while (!linkData.isEmpty());
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
            favIconCache.loadIcon(favIconUrl, bm -> {
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
        //  builder.setAlignment(mine ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL);
        builder.setMaxLines(1);
        builder.setEllipsize(TextUtils.TruncateAt.END);

        return builder.build();
    }

    void loadLinks() {
        httpClient.get("links", in -> {
            // Off main thread
            try {
                JSONObject json = LinkShareApplication.readStream(new InputStreamReader(in));
                if (json.has("success")) {
                    Log.d(TAG, "Got some links");
                    List<Link> parsed = parseLinks(json.getJSONArray("success"));

                    parsed.sort(Comparator.comparingLong(Link::getCreated));

                    synchronized (links) {
                        links.clear();
                        links.addAll(parsed);
                    }

                    postInvalidate();
                    return null;
                } else {
                    Log.e(TAG, "Server problem getting links: " + json.getString("error"));
                }
            } catch (JSONException | IOException e) {
                Log.w(TAG, "Problem fetching links", e);
            }
            return null;
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

    private static class LinkData {
        Link link;
        StaticLayout textLayout;
        boolean mine;

        LinkData(Link link, boolean mine, StaticLayout textLayout) {
            this.link = link;
            this.mine = mine;
            this.textLayout = textLayout;
        }
    }

}
