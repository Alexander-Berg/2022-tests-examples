package com.yandex.launcher;

import android.content.Context;

import com.yandex.launcher.common.app.CommonAppState;

public class TestCommonAppState extends CommonAppState {

    private static TestCommonAppState instance;

    private TestCommonAppState(Context appContext) {
        super(appContext);
    }

    public static void init(Context context) {
        instance = new TestCommonAppState(context);
        instance.init();
    }
}
