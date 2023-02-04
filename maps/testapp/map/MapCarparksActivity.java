package com.yandex.maps.testapp.map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.carparks.CarparksCarparkTapInfo;
import com.yandex.mapkit.directions.carparks.CarparksLayer;
import com.yandex.mapkit.directions.carparks.CarparksNearbyLayer;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.GeoObjectSelectionMetadata;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.maps.testapp.R;

import java.util.HashMap;
import java.util.logging.Logger;


public class MapCarparksActivity extends MapBaseActivity implements
        CameraListener, InputListener,
        MapCustomizationDialog.StyleHandler<MapBaseActivity.StyleTarget> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_carparks);
        super.onCreate(savedInstanceState);

        zoomLabel = findViewById(R.id.zoom_label);

        map = mapview.getMap();
        map.setTiltGesturesEnabled(true);

        carparksLayer = DirectionsFactory.getInstance().createCarparksLayer(mapview.getMapWindow());
        carparksNearbyLayer = DirectionsFactory.getInstance().createCarparksNearbyLayer(mapview.getMapWindow());

        carparksLayer.setVisible(true);
        map.addCameraListener(this);
        map.addInputListener(this);
        map.addTapListener(geoObjectTapListener_);

        map.move(new CameraPosition(
                         new Point(55.7522f, 37.6235f), 15.0f, 0.0f, 0.0f),
                 new Animation(Animation.Type.SMOOTH, 0),
                 null);


        nearbyAreas = map.getMapObjects().addCollection();

        customizeStyleButton = findViewById(R.id.customize_style_button);
        customizationDialog = new MapCustomizationDialog(
            this,
            this /* handler */,
            new HashMap<StyleTarget, Integer>(){{
                put(
                    StyleTarget.CARPARKS,
                    R.menu.carparks_customization_templates);
        }});
    }

    @Override
    public void applyStyle(MapBaseActivity.StyleTarget unused, final String style) {
        postponedStyle = style;
        customizeStyle();
    }

    @Override
    public void saveStyle(MapBaseActivity.StyleTarget unused, final String style) {
        customizeStyleButton.setVisibility(View.VISIBLE);
        postponedStyle = style;
    }

    private void customizeStyle() {
        customizeStyleButton.setVisibility(View.GONE);
        if (postponedStyle == null) {
            return;
        }
        boolean ok = carparksLayer.setCarparksStyle(postponedStyle);
        if (!ok) {
            LOGGER.warning("Failed to set carparks lots layer style");
        }
        ok = carparksNearbyLayer.setCarparksStyle(postponedStyle);
        if (!ok) {
            LOGGER.warning("Failed to set carparks nearby layer style");
        }
        postponedStyle = null;
    }

    public void onBackClick(View view) {
        finish();
    }

    public void onStyleClick(View view) {
        customizationDialog.show(StyleTarget.CARPARKS);
    }

    public void onCustomizeStyleClick(View view) {
        customizeStyle();
    }

    public void onWhereIsMyCarButtonPressed(View view) {
        if (lastParkPlace != null) {
            map.move(
                new CameraPosition(lastParkPlace, 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);
        } else {
            Toast.makeText(
                MapCarparksActivity.this,
                R.string.map_my_car_location_unavailable,
                Toast.LENGTH_LONG).show();
        }
    }

    public void onCarparksCheckboxClick(View view) {
        carparksLayer.setVisible(((CheckBox) view).isChecked());
    }

    public void onHideNearbyClick(View view) {
        carparksNearbyLayer.hide();
        nearbyAreas.clear();
    }

    public void onNightModeCheckboxClick(View view) {
        map.setNightModeEnabled(((CheckBox) view).isChecked());
    }

    @Override
    public void onCameraPositionChanged(
            Map map,
            CameraPosition cameraPosition,
            CameraUpdateReason cameraUpdateReason,
            boolean finished) {
        updateZoomLabel();
    }

    @SuppressLint("DefaultLocale")
    private void updateZoomLabel() {
        zoomLabel.setText(
                String.format("Z: %.2f", map.getCameraPosition().getZoom()));
    }

    private void showNotification(String title, String message) {
        dialog = new AlertDialog.Builder(MapCarparksActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) { dialog.cancel(); }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mapview.getMap().deselectGeoObject();
                        dialog = null;
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private final GeoObjectTapListener geoObjectTapListener_ =
            new GeoObjectTapListener() {
                @SuppressLint("DefaultLocale")
                @Override
                public boolean onObjectTap(GeoObjectTapEvent event) {
                    if (dialog != null) {
                        // Android has a delay between AlertDialog.show() call
                        // and an actual appearance of the dialog.
                        // Thus user has a chance to tap several times.
                        return true;
                    }

                    final CarparksCarparkTapInfo carparkTapInfo = event.getGeoObject()
                            .getMetadataContainer()
                            .getItem(CarparksCarparkTapInfo.class);
                    final GeoObjectSelectionMetadata metadata =
                            event.getGeoObject()
                                    .getMetadataContainer()
                                    .getItem(GeoObjectSelectionMetadata.class);

                    if (carparkTapInfo != null && metadata != null) {
                        mapview.getMap().selectGeoObject(
                                metadata.getId(),
                                metadata.getLayerId());
                        showNotification(
                                "Carpark",
                                String.format(
                                        "id: %s\n" +
                                        "type: %s\n" +
                                        "uri: %s\n" +
                                        "group: %s\n" +
                                        "price: %s",
                                        carparkTapInfo.getId(),
                                        carparkTapInfo.getType(),
                                        carparkTapInfo.getUri(),
                                        carparkTapInfo.getGroup(),
                                        carparkTapInfo.getPrice()));
                    }
                    return true;
                }
            };

    @Override
    public void onMapTap(Map map, Point point) {
        if (dialog == null) {
            mapview.getMap().deselectGeoObject();
        }
    }

    @Override
    public void onMapLongTap(Map map, Point point) {
        EditText distanceEdit = findViewById(R.id.carparks_nearby_distance);
        try {
            float distance = Float.valueOf(distanceEdit.getText().toString());
            Circle circle = new Circle(point, distance);
            nearbyAreas.clear();
            nearbyAreas.addCircle(circle, 0xee0000ff, 1.5f, 0);
            CheckBox carparksCheckBox = findViewById(R.id.carparks_checkbox);
            carparksCheckBox.setChecked(false);
            carparksLayer.setVisible(false);
            carparksNearbyLayer.show(point, distance);
        } catch (NumberFormatException e) {
            showNotification(
                "Carpark",
                "Only numbers are allowed as nearby distance");
        } catch (Exception ignored) {
        }
    }

    private Map map;
    private TextView zoomLabel;
    private CircleMapObject lastParkCircle;
    private MapObjectCollection nearbyAreas;
    private AlertDialog dialog;
    CarparksLayer carparksLayer;
    CarparksNearbyLayer carparksNearbyLayer;

    private Point lastParkPlace;

    private MapCustomizationDialog<StyleTarget> customizationDialog;
    private String postponedStyle = null;
    private Button customizeStyleButton = null;
    private static final Logger LOGGER = Logger.getLogger("yandex.maps");
}
