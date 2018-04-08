package com.moosemorals.linkshare;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class LinkView extends LinearLayout {

    private View rootView;
    private TextView titleView;
    private ImageView favIconView;
    private Link link;

    public LinkView(Context context) {
        super(context);
    }

    public LinkView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LinkView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LinkView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        rootView = inflate(context, R.layout.link_view, this);

        titleView = findViewById(R.id.link_view_title);
        favIconView = findViewById(R.id.link_view_favicon);
    }

    void setLink(Link l) {
        this.link = l;
        updateViews();
    }

    Link getLink() {
        return link;
    }

    private void updateViews() {
        String title = link.getTitle();
        if (title == null) {
            title = link.getUrl();
        }

        titleView.setText(title);

        LinkShareApplication.loadFavIcon(link.getUrl(), bitmap -> {
            favIconView.setImageBitmap(bitmap);
        });
    }
}
