package com.yandex.maps.testapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import com.yandex.runtime.Runtime;

public abstract class TestAppActivity extends FragmentActivity {
    @Override
    protected void onStart() {
        super.onStart();
        MapkitAdapter.onStart(this);
        onStartImpl();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MapkitAdapter.onResume(this);
    }

    @Override
    protected void onPause() {
        MapkitAdapter.onPause(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        onStopImpl();
        MapkitAdapter.onStop(this);
        super.onStop();
    }

    protected TestAppActivity() {
        MainApplication.initialize();
    }

    protected abstract void onStartImpl();
    protected abstract void onStopImpl();
}
