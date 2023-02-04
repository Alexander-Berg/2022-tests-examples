package com.yandex.maps.testapp.logs;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

public class VerboseInfoActivity extends TestAppActivity {
    public final static String EXTRA_SCOPE = "com.yandex.maps.testapp.logs.SCOPE";
    public final static String EXTRA_INFO = "com.yandex.maps.testapp.logs.INFO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verbose_log_view);
        ((TextView)findViewById(R.id.log_text)).setMovementMethod(new LinkMovementMethod());

        Intent intent = getIntent();
        ((TextView)findViewById(R.id.log_info)).setText(
            intent.getStringExtra(EXTRA_SCOPE));
        ((TextView)findViewById(R.id.log_text)).setText(
            intent.getStringExtra(EXTRA_INFO));
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}
}
