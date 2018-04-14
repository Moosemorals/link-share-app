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

public final class TabBar extends View {
    private static final String TAG = "TabBar";
    private static final int TAB_HEIGHT = 80;
    private final List<TabData> tabs = new LinkedList<>();
    private final Set<TabChangedListener> tabChangedListeners = new HashSet<>();

    private RectF tabRectUpper, tabRectLower;
    private Paint unselectedPaint, selectedPaint;
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

        setBackgroundColor(0x42000000);

        unselectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unselectedPaint.setColor(0xffe0e0e0);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setColor(0xffffffff);

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

            int tabWidth = getWidth() / tabs.size();

            for (int i = 0; i < tabs.size(); i++) {
                TabData tab = tabs.get(i);
                if (clickX > tabWidth * i && clickX < tabWidth * (i + 1)) {
                    selectTab(tab.name);
                    invalidate();
                }
            }
        }

        return result;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Paint tabPaint;
        int tabWidth = getWidth() / tabs.size();
        tabRectUpper.right = tabWidth;
        tabRectLower.right = tabWidth;

        for (TabData tab : tabs) {

            if (tab.name.equals(selectedTab)) {
                tabPaint = selectedPaint;
            } else {
                tabPaint = unselectedPaint;
            }

            canvas.drawRoundRect(tabRectUpper, 32, 32, tabPaint);
            canvas.drawRect(tabRectLower, tabPaint);

            StaticLayout text = StaticLayout.Builder
                    .obtain(tab.name, 0, tab.name.length(), textPaint, tabWidth)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();

            canvas.save();
            canvas.translate(tabWidth / 2 - text.getLineWidth(0) / 2, TAB_HEIGHT / 2 - text.getHeight() / 2);

            text.draw(canvas);
            canvas.restore();

            canvas.translate(tabWidth, 0);
        }
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = TAB_HEIGHT;

        Log.d(TAG, "Setting size to " + width + "x" + height);

        setMeasuredDimension(width, height);
    }

    void addTab(String name) {
        tabs.add(new TabData(name));
        if (selectedTab == null) {
            selectedTab = name;
        }
    }

    void selectTab(String name) {
        selectedTab = name;
        notifyListeners(name);
    }

    interface TabChangedListener {
        void onTabChanged(String newTabName);
    }

    private static class TabData {
        String name;

        TabData(String name) {
            this.name = name;
        }
    }
}
