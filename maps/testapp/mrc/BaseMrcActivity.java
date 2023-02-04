package com.yandex.maps.testapp.mrc;


import android.os.Bundle;

import com.yandex.maps.testapp.MainApplication;
import com.yandex.maps.testapp.TestAppActivity;

public class BaseMrcActivity extends TestAppActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MrcAdapter.onResume(this);
    }

    @Override
    protected void onPause() {
        MrcAdapter.onPause(this);
        super.onPause();
    }

    @Override
    protected void onStartImpl() {

    }

    @Override
    protected void onStopImpl() {

    }
}
