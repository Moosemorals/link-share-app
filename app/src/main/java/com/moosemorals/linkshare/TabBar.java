package com.moosemorals.linkshare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.moosemorals.linkshare.ConversationView.ICON_SIZE;
import static com.moosemorals.linkshare.ConversationView.SHADOW_COLOUR;
import static com.moosemorals.linkshare.ConversationView.SHADOW_OFFSET;
import static com.moosemorals.linkshare.ConversationView.SHADOW_RADIUS;

public final class TabBar extends View {
    private static final String TAG = "TabBar";
    private static final int TAB_HEIGHT = 80;
    private static final int TAB_SELECTED_COLOR = 0xffffffff;
    private static final int TAB_UNSELECTED_COLOR = 0xffe0e0e0;
    private final List<TabData> tabs = new LinkedList<>();
    private final Set<TabChangedListener> tabChangedListeners = new HashSet<>();

    private RectF tabRectUpper, tabRectLower;
    private Paint tabBackgroundPaint;
    private TextPaint textPaint;
    private String selectedTab;
    private GestureDetector gestureDetector;

    public TabBar(Context context) {
        super(context);
        init();
    }

    public TabBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TabBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TabBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        tabRectUpper = new RectF(0, 0, 0, TAB_HEIGHT);
        tabRectLower = new RectF(0, TAB_HEIGHT / 2, 0, TAB_HEIGHT);

        setBackgroundColor(getResources().getColor(R.color.background));

        tabBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xff000000);
        textPaint.setTextSize(32);
        gestureDetector = new GestureDetector(getContext(), new BasicGestureListener());

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = gestureDetector.onTouchEvent(event);
        if (result) {
            float clickX = event.getX();
            float clickY = event.getY();


            for (int i = 0; i < tabs.size(); i++) {
                TabData tab = tabs.get(i);
                if (tab.bounds.contains(clickX, clickY)) {
                    setActiveTab(tab.name);
                    invalidate();
                    performClick();
                }
            }
        }

        return result;
    }

    @Override
    public void onDraw(Canvas canvas) {
        float currentLeft = 0;
        float maxWidth = (getWidth() - (ConversationView.WINDOW_MARGIN * 2)) / tabs.size();

        canvas.translate(ConversationView.WINDOW_MARGIN, SHADOW_RADIUS);

        for (TabData tab : tabs) {
            StaticLayout text = StaticLayout.Builder
                    .obtain(tab.name, 0, tab.name.length(), textPaint, (int) maxWidth)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();

            float tabWidth = text.getLineWidth(0) + ICON_SIZE;


            tab.bounds.left = currentLeft + SHADOW_RADIUS;
            tab.bounds.top = 0;
            tab.bounds.right = currentLeft + tabWidth - SHADOW_RADIUS;
            tab.bounds.bottom = TAB_HEIGHT;

            tabRectUpper.right = tabWidth;
            tabRectLower.right = tabWidth;

            if (tab.name.equals(selectedTab)) {
                tabBackgroundPaint.setColor(TAB_SELECTED_COLOR);
            } else {
                tabBackgroundPaint.setColor(TAB_UNSELECTED_COLOR);
            }

            tabBackgroundPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_OFFSET, SHADOW_OFFSET, SHADOW_COLOUR);
            canvas.drawRoundRect(tabRectUpper, 32, 32, tabBackgroundPaint);
            tabBackgroundPaint.clearShadowLayer();
            canvas.drawRect(tabRectLower, tabBackgroundPaint);

            canvas.save();
            canvas.translate(tabWidth / 2 - text.getLineWidth(0) / 2, TAB_HEIGHT / 2 - text.getHeight() / 2);

            text.draw(canvas);
            canvas.restore();

            canvas.translate(tabWidth, 0);
            currentLeft += tabWidth;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = TAB_HEIGHT + SHADOW_RADIUS;

        Log.d(TAG, "Setting size to " + width + "x" + height);

        setMeasuredDimension(width, height);
    }

    void addTabChangedListener(TabChangedListener l) {
        synchronized (tabChangedListeners) {
            tabChangedListeners.add(l);
        }
    }

    void removeTabChangedListner(TabChangedListener l) {
        synchronized (tabChangedListeners) {
            tabChangedListeners.remove(l);
        }
    }

    private void notifyListeners(String newTab) {
        synchronized (tabChangedListeners) {
            for (TabChangedListener l : tabChangedListeners) {
                l.onTabChanged(newTab);
            }
        }
    }

    void addFirstTab(String name) {
        addTab(name, 0);
    }

    void addTab(String name) {
        addTab(name, tabs.size());
    }

    void addTab(String name, int index) {
        tabs.add(index, new TabData(name));
        if (selectedTab == null) {
            selectedTab = name;
        }
        postInvalidate();
    }

    void setActiveTab(String name) {
        selectedTab = name;
        notifyListeners(name);
    }

    interface TabChangedListener {
        void onTabChanged(String newTabName);
    }

    private static class TabData {
        String name;
        RectF bounds = new RectF(0,0,0,0);

        TabData(String name) {
            this.name = name;
        }
    }
}
