package com.yandex.maps.testapp.map;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.yandex.mapkit.map.MapLoadStatistics;
import com.yandex.mapkit.map.MapLoadedListener;

import java.util.logging.Logger;

class MapLoadStopwatch implements MapLoadedListener {
    private static final Logger LOGGER = Logger.getLogger("yandex.maps");
    private MapBaseActivity mapActivity;
    private TextView textView;

    MapLoadStopwatch(MapBaseActivity mapActivity, TextView textView) {
        this.mapActivity = mapActivity;
        this.textView = textView;
    }

    @Override
    public void onMapLoaded(@NonNull MapLoadStatistics statistics) {
        textView.setText(Float.toString(statistics.getFullyAppeared() / 1000.f));
        final String message = MapBaseActivity.toString(statistics);
        LOGGER.info(message);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapActivity.showMessage(message, "Map Load Status");
            }
        });
    }
}
