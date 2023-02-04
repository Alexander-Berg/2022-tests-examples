package com.yandex.mobile.verticalapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import com.yandex.mobile.verticalapp.dynamicscreen.DynamicScreenActivity;
import com.yandex.mobile.verticalwidget.activity.BaseCoreActivity;

/**
 * @author ironbcc on 22.04.2015.
 */
public class TestActivity extends BaseCoreActivity implements View.OnClickListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_widgets);
        ((TestApplication) getApplication()).getAppComponent().inject(this);
        findViewById(R.id.dynamic_screen_btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dynamic_screen_btn:
                startActivity(new Intent(this, DynamicScreenActivity.class));
                break;
        }
    }
}
