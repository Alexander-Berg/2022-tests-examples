package com.yandex.maps.testapp.map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.yandex.mapkit.geometry.Point;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.map.MapActivity;
import com.yandex.runtime.bindings.Serialization;

public class MapCoordinatesActivity extends TestAppActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_coordinates);
        super.onCreate(savedInstanceState);
    }

    public void onMoveClick(View view) {
        try {
            float latitude = Float.parseFloat(((TextView) findViewById(R.id.latitude)).getText().toString());
            float longitude = Float.parseFloat(((TextView) findViewById(R.id.longitude)).getText().toString());

            Point point = new Point(latitude, longitude);
            Intent result = new Intent();
            result.putExtra(
                MapActivity.CAMERA_TARGET_EXTRA,
                Serialization.serializeToBytes(point));

            setResult(RESULT_OK, result);
            finish();
        } catch (NumberFormatException e) {
            Utils.showMessage(this, "Number format error");
        }
    }

    @Override
    protected void onStopImpl(){}

    @Override
    protected void onStartImpl(){}
}
