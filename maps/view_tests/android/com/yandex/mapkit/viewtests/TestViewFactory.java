package com.yandex.mapkit.viewtests;

import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.MapKitFactory;
import android.app.Activity;


/** @exclude */
public class TestViewFactory {
    public static MapView createMapVew(Activity testActivity) {
        MapKitFactory.initialize(testActivity);
        MapKitFactory.getInstance().initialize("0", "0");
        MapView mapView = new MapView(testActivity);
        testActivity.setContentView(mapView);
        return mapView;
    }
}

