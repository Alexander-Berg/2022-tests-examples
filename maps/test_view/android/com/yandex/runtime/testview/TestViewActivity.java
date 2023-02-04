package com.yandex.runtime.testview;

import com.yandex.runtime.view.PlatformView;
import com.yandex.runtime.view.PlatformViewFactory;
import com.yandex.runtime.NativeObject;
import com.yandex.runtime.Runtime;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;

/** @exclude */
public class TestViewActivity extends Activity {
    private static final String LOG_TAG = "yandex.maps";
    private static final String DELEGATE_PARAMS_EXTRA = "DELEGATE_PARAMS_EXTRA";

    private PlatformView platformView;

    public static void start(NativeObject nativeDelegateParams) {
        Context context = Runtime.getApplicationContext();
        Intent intent = new Intent(context, TestViewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(DELEGATE_PARAMS_EXTRA, nativeDelegateParams.release());
        context.startActivity(intent);
    }

    public void stop() throws Throwable {
        // Finalize calling for checking destructions of C++ objects in unit tests.
        Log.d(LOG_TAG, "TestViewActivity:stop");
        ViewGroup vg = (ViewGroup)(platformView.getView().getParent());
        vg.removeView(platformView.getView());
        platformView.getNativePlatformView().finalize();
        platformView = null;
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "TestViewActivity:onCreate");
        NativeObject nativeDelegateParams = new NativeObject(getIntent().getLongExtra(DELEGATE_PARAMS_EXTRA, 0), false);
        platformView = PlatformViewFactory.getPlatformView(this, null);
        startTestView(platformView.getNativePlatformView(), nativeDelegateParams, this);
        setContentView(platformView.getView());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (platformView != null) {
            platformView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (platformView != null) {
            platformView.resume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (platformView != null) {
            platformView.stop();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (platformView != null) {
            platformView.start();
        }
    }

    private native void startTestView(
            NativeObject platformView,
            NativeObject nativeDelegateParams,
            TestViewActivity activity);
}
