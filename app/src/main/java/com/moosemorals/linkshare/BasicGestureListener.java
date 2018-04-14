package com.moosemorals.linkshare;

import android.view.GestureDetector;
import android.view.MotionEvent;

class BasicGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return true;
    }
}
