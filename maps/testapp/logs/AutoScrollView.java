package com.yandex.maps.testapp.logs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class AutoScrollView extends ScrollView {
    public AutoScrollView(Context context) {
        super(context);
    }

    public AutoScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        fullScroll(ScrollView.FOCUS_DOWN);
    }
}
