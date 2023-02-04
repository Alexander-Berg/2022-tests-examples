package com.yandex.maps.testapp.map;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.yandex.mapkit.ConflictResolutionMode;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.GeoObjectSelectionMetadata;
import com.yandex.mapkit.map.LayerIds;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.Sublayer;
import com.yandex.mapkit.map.SublayerFeatureFilter;
import com.yandex.mapkit.map.SublayerFeatureFilterType;
import com.yandex.mapkit.map.SublayerFeatureType;
import com.yandex.mapkit.map.SublayerManager;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.List;

public class MapSublayersActivity extends MapBaseActivity
        implements GeoObjectTapListener {
    private static final String[] SUBLAYER_PRESETS = new String[] {
            "Normal", "Buildings on top", "Jams on buildings", "Rainbow on ground",
            "Circle on top", "Hide rainbow", "POI on ground",
            "POI conflict resolution (ignore)", "POI conflict resolution (minor)",
            "POI conflict resolution (equal)", "POI conflict resolution (major)"
    };

    private static final String CUSTOM_MO_LAYER_NAME = "my_map_objects";

    private static class SublayerDesc {
        String layerId;
        SublayerFeatureType featureType;
        ConflictResolutionMode conflictResolutionMode;

        SublayerDesc(
                String layerId, SublayerFeatureType featureType,
                ConflictResolutionMode conflictResolutionMode) {
            this.layerId = layerId;
            this.featureType = featureType;
            this.conflictResolutionMode = conflictResolutionMode;
        }
    };

    private SublayerManager sublayerManager;
    private List<SublayerDesc> defaultSublayers;

    private final MapObjectTapListener placemarkTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject mapObject, Point point) {
            PlacemarkMapObject placemarkMapObject = (PlacemarkMapObject) mapObject;
            placemarkMapObject.setOpacity(1.5f - placemarkMapObject.getOpacity());
            return true;
        }
    };

    private final MapObjectTapListener circleTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject mapObject, Point point) {
            CircleMapObject circleMapObject = (CircleMapObject) mapObject;
            circleMapObject.setFillColor(
                    circleMapObject.getFillColor() == Color.WHITE ? Color.BLACK : Color.WHITE);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_sublayers);
        super.onCreate(savedInstanceState);

        Spinner spinner = findViewById(R.id.preset_spinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, SUBLAYER_PRESETS);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectPreset(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        Map map = mapview.getMap();
        map.addTapListener(this);
        sublayerManager = map.getSublayerManager();

        Point cameraTarget = map.getCameraPosition().getTarget();
        PlacemarkMapObject placemarkMapObject = map.getMapObjects().addPlacemark(
                new Point(cameraTarget.getLatitude() - 0.005, cameraTarget.getLongitude()),
                ImageProvider.fromResource(this, R.drawable.rainbow4));
        placemarkMapObject.setDraggable(true);
        placemarkMapObject.addTapListener(placemarkTapListener);

        MapObjectCollection mapObjectCollection = map.addMapObjectLayer(CUSTOM_MO_LAYER_NAME);
        Circle circle = new Circle(
                new Point(cameraTarget.getLatitude() - 0.005, cameraTarget.getLongitude() - 0.0025),
                100.f);
        mapObjectCollection.addCircle(circle, Color.RED, 2.f, Color.WHITE);
        mapObjectCollection.addTapListener(circleTapListener);

        jamsSwitchBt.setChecked(true);

        // should go after all 'add' operations
        defaultSublayers = snapshot();
    }

    private void clearSublayers() {
        while (sublayerManager.size() > 0) {
            sublayerManager.remove(0);
        }
    }

    private List<SublayerDesc> snapshot() {
        List<SublayerDesc> sublayers = new ArrayList<>();
        for (int i = 0; i < sublayerManager.size(); i++) {
            Sublayer sublayer = sublayerManager.get(i);
            sublayers.add(new SublayerDesc(
                sublayer.getLayerId(), sublayer.getFeatureType(),
                sublayer.getConflictResolutionMode()));
        }
        return sublayers;
    }

    private void applySnapshot(List<SublayerDesc> snapshot) {
        clearSublayers();
        for (SublayerDesc desc : snapshot) {
            Sublayer sublayer = sublayerManager.appendSublayer(desc.layerId, desc.featureType);
            sublayer.setConflictResolutionMode(desc.conflictResolutionMode);
        }
    }

    private void extractPoiSublayerWithConflictResolutionMode(ConflictResolutionMode mode)
    {
        List<String> tags = new ArrayList<>();
        tags.add("poi");

        Integer placemarksAndLabelsIndex = sublayerManager.findFirstOf(
                LayerIds.getMapLayerId(), SublayerFeatureType.PLACEMARKS_AND_LABELS);

        Sublayer placemarksAndLabelsSublayer = sublayerManager.get(placemarksAndLabelsIndex);
        {
            SublayerFeatureFilter filter = placemarksAndLabelsSublayer.getFilter();
            filter.setType(SublayerFeatureFilterType.EXCLUDE);
            filter.setTags(tags);
        }

        Sublayer poiSublayer;
        if (mode == ConflictResolutionMode.MINOR) {
            poiSublayer = sublayerManager.insertSublayerBefore(
                    placemarksAndLabelsIndex, LayerIds.getMapLayerId(), SublayerFeatureType.PLACEMARKS_AND_LABELS);
        } else {
            poiSublayer = sublayerManager.insertSublayerAfter(
                    placemarksAndLabelsIndex, LayerIds.getMapLayerId(), SublayerFeatureType.PLACEMARKS_AND_LABELS);
        }
        {
            SublayerFeatureFilter filter = poiSublayer.getFilter();
            filter.setType(SublayerFeatureFilterType.INCLUDE);
            filter.setTags(tags);
        }
        poiSublayer.setConflictResolutionMode(mode);
    }

    private void selectPreset(int index) {
        applySnapshot(defaultSublayers);
        switch (index) {
            case 0:
                break;

            case 1: {
                Integer buildingsIndex = sublayerManager.findFirstOf(
                        LayerIds.getBuildingsLayerId());
                sublayerManager.moveToEnd(buildingsIndex);
                break;
            }

            case 2: {
                Integer buildingsIndex = sublayerManager.findFirstOf(
                        LayerIds.getBuildingsLayerId());
                Integer jamsIndex = sublayerManager.findFirstOf(
                        LayerIds.getJamsLayerId(), SublayerFeatureType.GROUND);
                sublayerManager.moveAfter(jamsIndex, buildingsIndex);
                break;
            }

            case 3: {
                Integer groundIndex = sublayerManager.findFirstOf(
                        LayerIds.getMapLayerId(), SublayerFeatureType.GROUND);
                Integer mapObjectPlacemarksIndex = sublayerManager.findFirstOf(
                        LayerIds.getMapObjectsLayerId(), SublayerFeatureType.PLACEMARKS_AND_LABELS);
                sublayerManager.moveAfter(mapObjectPlacemarksIndex, groundIndex);
                break;
            }

            case 4: {
                Integer customMapObjectsIndex = sublayerManager.findFirstOf(
                        CUSTOM_MO_LAYER_NAME, SublayerFeatureType.GROUND);
                sublayerManager.moveToEnd(customMapObjectsIndex);
                break;
            }

            case 5: {
                for (;;) {
                    Integer mapObjectsIndex = sublayerManager.findFirstOf(
                            LayerIds.getMapObjectsLayerId());
                    if (mapObjectsIndex == null) {
                        break;
                    }
                    sublayerManager.remove(mapObjectsIndex);
                }
                break;
            }

            case 6: {
                List<String> tags = new ArrayList<>();
                tags.add("poi");

                Integer placemarksAndLabelsIndex = sublayerManager.findFirstOf(
                        LayerIds.getMapLayerId(), SublayerFeatureType.PLACEMARKS_AND_LABELS);

                Sublayer placemarksAndLabelsSublayer = sublayerManager.get(placemarksAndLabelsIndex);
                {
                    SublayerFeatureFilter filter = placemarksAndLabelsSublayer.getFilter();
                    filter.setType(SublayerFeatureFilterType.EXCLUDE);
                    filter.setTags(tags);
                }

                Integer groundIndex = sublayerManager.findFirstOf(
                        LayerIds.getMapLayerId(), SublayerFeatureType.GROUND);
                Sublayer poiSublayer = sublayerManager.insertSublayerAfter(
                        groundIndex, LayerIds.getMapLayerId(), SublayerFeatureType.PLACEMARKS_AND_LABELS);
                {
                    SublayerFeatureFilter filter = poiSublayer.getFilter();
                    filter.setType(SublayerFeatureFilterType.INCLUDE);
                    filter.setTags(tags);
                }
                poiSublayer.setConflictResolutionMode(ConflictResolutionMode.EQUAL);

                break;
            }

            case 7: {
                extractPoiSublayerWithConflictResolutionMode(ConflictResolutionMode.IGNORE);
                break;
            }

            case 8: {
                extractPoiSublayerWithConflictResolutionMode(ConflictResolutionMode.MINOR);
                break;
            }

            case 9: {
                extractPoiSublayerWithConflictResolutionMode(ConflictResolutionMode.EQUAL);
                break;
            }

            case 10: {
                extractPoiSublayerWithConflictResolutionMode(ConflictResolutionMode.MAJOR);
                break;
            }
        }
    }

    @Override
    public boolean onObjectTap(GeoObjectTapEvent event) {
        GeoObjectSelectionMetadata metadata = event.getGeoObject().getMetadataContainer().getItem(
                GeoObjectSelectionMetadata.class);
        if (metadata != null) {
            mapview.getMap().selectGeoObject(metadata.getId(), metadata.getLayerId());
            return true;
        }
        return false;
    }
}
