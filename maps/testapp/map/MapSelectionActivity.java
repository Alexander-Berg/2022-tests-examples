package com.yandex.maps.testapp.map;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.yandex.mapkit.ConflictResolutionMode;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.GeoObjectSelectionMetadata;
import com.yandex.mapkit.map.GeoObjectTags;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.Sublayer;
import com.yandex.mapkit.map.SublayerFeatureType;
import com.yandex.mapkit.map.SublayerManager;
import com.yandex.mapkit.uri.UriObjectMetadata;
import com.yandex.mapkit.uri.Uri;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

public class MapSelectionActivity extends MapBaseActivity implements GeoObjectTapListener, InputListener {
    private static Logger LOGGER = Logger.getLogger("yandex.maps");
    private PlacemarkMapObject placemark;
    private Stack<Selection> selectionStack;
    private MapObjectCollection longTapPlacemarks;

    private class Selection
    {
        public GeoObjectSelectionMetadata metadata;
        public CameraPosition position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_selection);
        super.onCreate(savedInstanceState);
        mapview.getMap().addTapListener(this);
        mapview.getMap().addInputListener(this);
        mapview.getMap().setFastTapEnabled(true);

        longTapPlacemarks = mapview.getMap().addMapObjectLayer("long_tap_placemarks");
        SublayerManager sublayerManager = mapview.getMap().getSublayerManager();
        Sublayer sublayer = sublayerManager.get(sublayerManager.findFirstOf(
                "long_tap_placemarks", SublayerFeatureType.PLACEMARKS_AND_LABELS));
        sublayer.setConflictResolutionMode(ConflictResolutionMode.MAJOR);

        placemark = mapview.getMap().getMapObjects().addPlacemark(mapview.getMap().getCameraPosition().getTarget());
        selectionStack = new Stack<Selection>();
    }

    @Override
    public boolean onObjectTap(GeoObjectTapEvent event) {
        // event.setSelected(true); // deprecated
        if (event.getGeoObject().getGeometry().size() != 0) {
            placemark.setGeometry(event.getGeoObject().getGeometry().get(0).getPoint());
        }

        String description = "";

        if (event.getGeoObject().getName() != null && !event.getGeoObject().getName().isEmpty()) {
            description += "name: " + event.getGeoObject().getName() + "; ";
        }

        final GeoObjectTags geoObjectTags = event.getGeoObject()
                .getMetadataContainer()
                .getItem(GeoObjectTags.class);
        if (geoObjectTags != null) {
            description += "tags: " + geoObjectTags.getTags().toString() + "; ";
        }

        final GeoObjectSelectionMetadata selectionMetadata = event.getGeoObject()
                .getMetadataContainer()
                .getItem(GeoObjectSelectionMetadata.class);

        if (selectionMetadata != null) {
            description += "id: " + selectionMetadata.getId() + "; layer id: " + selectionMetadata.getLayerId() + "; ";
            selectionStack.push(
                    new Selection(){{
                        metadata = selectionMetadata;
                        position = mapview.getMap().getCameraPosition();
                    }});
            mapview.getMap().selectGeoObject(selectionMetadata.getId(), selectionMetadata.getLayerId());
        }

        final UriObjectMetadata uriObjectMetadata = event.getGeoObject()
                .getMetadataContainer()
                .getItem(UriObjectMetadata.class);

        if (uriObjectMetadata != null) {
            List<String> uris = new ArrayList<>();
            for (Uri uri : uriObjectMetadata.getUris())
                uris.add(uri.getValue());

            description += "uris: " + uris + "; ";
        }

        TextView descriptionTextView = findViewById(R.id.description);
        descriptionTextView.setText(description);
        return selectionMetadata != null;
    }

    public void onBackButtonClick(View view) {
        if (selectionStack.size() > 0) {
            Selection selection = selectionStack.pop();
            mapview.getMap().move(selection.position);
            mapview.getMap().selectGeoObject(selection.metadata.getId(), selection.metadata.getLayerId());
        } else {
            mapview.getMap().deselectGeoObject();
        }
    }

    @Override
    public void onMapTap(Map map, Point point) {
        mapview.getMap().deselectGeoObject();
        selectionStack.clear();
        TextView descriptionTextView = findViewById(R.id.description);
        descriptionTextView.setText("description");
    }

    @Override
    public void onMapLongTap(Map map, Point point) {
        longTapPlacemarks.addPlacemark(point);
    }
}
