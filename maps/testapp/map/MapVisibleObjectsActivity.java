package com.yandex.maps.testapp.map;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.coverage.Coverage;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.LinearRing;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polygon;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.GeoObjectTags;
import com.yandex.mapkit.map.GeoObjectInspectionMetadata;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.LayerIds;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapLoadedListener;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.bindings.Serialization;
import com.yandex.mapkit.logo.Alignment;
import com.yandex.mapkit.logo.HorizontalAlignment;
import com.yandex.mapkit.logo.VerticalAlignment;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class MapVisibleObjectsActivity extends MapBaseActivity {
    private static final Logger LOGGER = Logger.getLogger("yandex.maps");
    private static final Point CAMERA_TARGET = new Point(59.945933, 30.320045);

    private Point point(int x, int y) {
        double START_LATITUDE = CAMERA_TARGET.getLatitude();
        double START_LONGITUDE = CAMERA_TARGET.getLongitude();

        double STEP_LATITUDE = 0.01;
        double STEP_LONGITUDE = 0.01;

        return new Point(START_LATITUDE + x * STEP_LATITUDE, START_LONGITUDE + y * STEP_LONGITUDE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_visible_objects);
        super.onCreate(savedInstanceState);

        MapObjectCollection mapObjects = mapview.getMap().getMapObjects();

        ImageProvider imageProvider = ImageProvider.fromResource(this, R.drawable.a0);
        mapObjects.addPlacemark(point(0, 0), imageProvider);
        mapObjects.addPlacemark(point(1, 0), imageProvider);
        mapObjects.addPlacemark(point(2, 0), imageProvider);
        mapObjects.addPlacemark(point(3, 0), imageProvider);

        mapObjects.addPolyline(new Polyline(Arrays.asList(point(0, 1), point(1, 1))));
        mapObjects.addPolyline(new Polyline(Arrays.asList(point(2, 1), point(3, 1))));

        List<Point> outerRing = Arrays.asList(point(0, 2), point(1, 2), point(1, 3), point(0, 3));
        mapObjects.addPolygon(new Polygon(new LinearRing(outerRing), new ArrayList<>()));

        mapObjects.addCircle(new Circle(point(3, 3), 100.f), 0xff0000ff, 5.f, 0x0000ffff);
    }

    public void onVisibleObjectsClick(View view) {
        String text = "";

        List<GeoObject> objects = mapview.getMap().visibleObjects(null, null);
        int groundObjects = 0;
        int points = 0;
        int polylines = 0;
        int polygons = 0;
        int circles = 0;
        int pois = 0;
        int structures = 0;

        int mapObjects = 0;
        int mapObjectPoints = 0;
        int mapObjectPolylines = 0;
        int mapObjectPolygons = 0;
        int mapObjectCircles = 0;

        int noMetadata = 0;

        for (GeoObject object : objects) {
            GeoObjectInspectionMetadata metadata = object.getMetadataContainer()
                .getItem(GeoObjectInspectionMetadata.class);

            if (metadata != null) {
                if (metadata.getLayerId().equals(LayerIds.getMapLayerId())) {
                    ++groundObjects;

                    switch (metadata.getObjectType()) {
                        case POINT:
                            ++points;
                            break;
                        case POLYLINE:
                            ++polylines;
                            break;
                        case POLYGON:
                            ++polygons;
                            break;
                        case CIRCLE:
                            ++circles;
                            break;
                    }

                    GeoObjectTags tags = object.getMetadataContainer().getItem(GeoObjectTags.class);
                    if (tags != null) {
                        if (tags.getTags().contains("poi"))
                            ++pois;
                        if (tags.getTags().contains("structure"))
                            ++structures;
                    }
                } else if (metadata.getLayerId().equals(LayerIds.getMapObjectsLayerId())) {
                    ++mapObjects;

                    switch (metadata.getObjectType()) {
                        case POINT:
                            ++mapObjectPoints;
                            break;
                        case POLYLINE:
                            ++mapObjectPolylines;
                            break;
                        case POLYGON:
                            ++mapObjectPolygons;
                            break;
                        case CIRCLE:
                            ++mapObjectCircles;
                            break;
                    }
                }
            } else {
                ++noMetadata;
            }
        }
        text += groundObjects + " ground layer objects (" + points + " points, " + polylines + " polylines, "
                + polygons + " polygons, " + circles + " circles; " + pois + " pois, " + structures + " structures); ";
        text += mapObjects + " map objects (" + mapObjectPoints + " points, " + mapObjectPolylines + " polylines, "
                + mapObjectPolygons + " polygons, " + mapObjectCircles + " circles); ";
        text += noMetadata + " objects without GeoObjectInspectionMetadata";

        showMessage(text, "Visible objects result");
    }
}
