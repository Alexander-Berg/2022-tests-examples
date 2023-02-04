package com.yandex.maps.testapp.masstransit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.GeoObjectSession;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.MasstransitInfoService;
import com.yandex.mapkit.transport.masstransit.Stop;
import com.yandex.mapkit.transport.masstransit.StopAlert;
import com.yandex.mapkit.transport.masstransit.StopMetadata;
import com.yandex.mapkit.transport.masstransit.StopScheduleMetadata;
import com.yandex.mapkit.uri.UriObjectMetadata;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.maps.testapp.masstransit.stops.brief.MasstransitBriefStopActivity;
import com.yandex.maps.testapp.masstransit.stops.schedule.MasstransitStopScheduleActivity;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class MasstransitActivity extends MapBaseActivity implements GeoObjectTapListener, InputListener {
    private static Logger LOGGER = Logger.getLogger("yandex.maps");
    private EditText uriInput;
    private MasstransitInfoService mtInfoService;
    private GeoObjectSession.GeoObjectListener listener;

    private class StopCard {
        private LinearLayout briefCardLayout;
        private TextView nameView;
        private TextView uriView;
        private TextView alertView;
        private Button openBriefStopCardButton;
        private Button openScheduleButton;
        private EditText timestampInput;
        private String resolvedUri;

        public StopCard(Context context) {
            briefCardLayout = findViewById(R.id.masstransit_stop_card);
            nameView = findViewById(R.id.masstransit_stop_card_name);
            uriView = findViewById(R.id.masstransit_stop_card_uri);
            alertView = findViewById(R.id.masstransit_stop_card_alert);
            openBriefStopCardButton = findViewById(R.id.masstransit_open_brief_card);
            openBriefStopCardButton.setOnClickListener(v -> {
                Intent intent = MasstransitBriefStopActivity.createIntent(
                    context,
                    resolvedUri
                );
                startActivity(intent);
            });
            timestampInput = findViewById(R.id.input_timestamp);
            openScheduleButton = findViewById(R.id.masstransit_open_schedule);
            openScheduleButton.setOnClickListener(v -> {
                Long timestamp = parseTimestampString(
                    MasstransitActivity.this,
                    timestampInput.getText().toString()
                );
                Intent intent = MasstransitStopScheduleActivity.createIntent(
                    MasstransitActivity.this,
                    resolvedUri,
                    timestamp
                );
                startActivity(intent);
            });
        }

        public void hide() {
            briefCardLayout.setVisibility(android.view.View.GONE);
        }

        public void show(String name, String uri, @Nullable String alert) {
            uriView.setText("URI: " + uri);
            nameView.setText("Name: " + name);
            if (alert != null) {
                alertView.setText("Alert: " + alert);
                alertView.setVisibility(android.view.View.VISIBLE);
            } else {
                alertView.setVisibility(android.view.View.GONE);
            }
            resolvedUri = uri;
            boolean isTransitStopUri = uri != null && uri.startsWith("ymapsbm1://transit/stop");
            if (isTransitStopUri) {
                openBriefStopCardButton.setVisibility(android.view.View.VISIBLE);
                openScheduleButton.setVisibility(android.view.View.VISIBLE);
                timestampInput.setVisibility(android.view.View.VISIBLE);
            } else {
                openBriefStopCardButton.setVisibility(android.view.View.INVISIBLE);
                openScheduleButton.setVisibility(android.view.View.INVISIBLE);
                timestampInput.setVisibility(android.view.View.INVISIBLE);
            }
            briefCardLayout.setVisibility(android.view.View.VISIBLE);
        }
    }

    private StopCard stopCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.hideKeyboard(this);
        setContentView(R.layout.map_masstransit);
        super.onCreate(savedInstanceState);
        stopCard = new StopCard(this);
        mapview.getMap().addTapListener(this);
        mapview.getMap().addInputListener(this);
        uriInput = findViewById(R.id.input_uri);
        mtInfoService = TransportFactory.getInstance().createMasstransitInfoService();
    }

    @Override
    public boolean onObjectTap(GeoObjectTapEvent event) {
        Utils.hideKeyboard(this);
        GeoObject stopObj = event.getGeoObject();
        String name = stopObj.getName();
        String uri = null;
        UriObjectMetadata uriMetadata = stopObj.getMetadataContainer().getItem(UriObjectMetadata.class);
        if (uriMetadata != null && uriMetadata.getUris().size() >= 1) {
            uri = uriMetadata.getUris().get(0).getValue();
        }

        LOGGER.info("On masstransit tap: name = '" + name + "', uri = '" + uri + "'");
        stopCard.show(name, uri, null);
        return true;
    }

    @Override
    public void onMapLongTap(Map map, Point point) {
        Utils.hideKeyboard(this);
    }

    @Override
    public void onMapTap(Map map, Point point) {
        Utils.hideKeyboard(this);
        mapview.getMap().deselectGeoObject();
        stopCard.hide();
    }

    public void onResolveUriForStop(View view) {
        Utils.hideKeyboard(this);
        final String uri = extractUri();
        listener = new GeoObjectSession.GeoObjectListener() {

            @Override
            public void onGeoObjectError(com.yandex.runtime.Error error) {
                LOGGER.info("Cannot resolve uri: " + error.toString());
                Utils.showError(getBaseContext(), error);
            }

            @Override
            public void onGeoObjectResult(GeoObject geoObject) {
                StopMetadata stopMetadata = geoObject.getMetadataContainer().getItem(StopMetadata.class);
                showResolvedInfo(
                    geoObject.getGeometry(),
                    stopMetadata.getStop(),
                    stopMetadata.getAlert(),
                    uri
                );
            }
        };
        mtInfoService.resolveStopUri(uri, listener);
    }

    public void onResolveUriForSchedule(View view) {
        Utils.hideKeyboard(this);
        final String uri = extractUri();
        listener = new GeoObjectSession.GeoObjectListener() {
            @Override
            public void onGeoObjectError(com.yandex.runtime.Error error) {
                LOGGER.info("Cannot resolve uri: " + error.toString());
                Utils.showError(getBaseContext(), error);
            }

            @Override
            public void onGeoObjectResult(GeoObject geoObject) {
                StopScheduleMetadata stopMetadata = geoObject.getMetadataContainer().getItem(StopScheduleMetadata.class);
                showResolvedInfo(
                    geoObject.getGeometry(),
                    stopMetadata.getStop(),
                    stopMetadata.getAlert(),
                    uri
                );
            }
        };
        mtInfoService.scheduleByStopUri(uri, null, listener);
    }

    private void showResolvedInfo(
        List<Geometry> geometries,
        Stop stop,
        StopAlert alert,
        String uri
    ) {
        String alertText = "";
        if (alert != null && alert.getEffect() != null) {
            StopAlert.Effect effect = alert.getEffect();
            switch (effect) {
                case NO_SERVICE:
                    alertText = "Stop is closed (Effect: " + effect.name() + ")";
                    break;
                default:
                    alertText = "Effect: " + effect.name();
            }
        }
        stopCard.show(stop.getName(), uri, alertText);
        Point point = geometries.get(0).getPoint();
        CameraPosition cp = mapview.getMap().getCameraPosition();
        CameraPosition newCameraPosition = new CameraPosition(
            point,
            /*zoom=*/17,
            cp.getAzimuth(),
            cp.getTilt()
        );
        mapview.getMap().move(
            newCameraPosition,
            new Animation(Animation.Type.SMOOTH, 0.5f),
            /* cameraCallback */ null
        );
    }

    private String extractUri() {
        final String uri = uriInput.getText().toString();
        if (uri.isEmpty()) {
            Utils.showMessage(this, "Input uri to resolve");
        }
        LOGGER.info("Resolving uri: " + uri);
        return uri;
    }


    private static Long parseTimestampString(Activity activity, String input) {
        if (!input.isEmpty()) {
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                Toast.makeText(
                    activity,
                    "Could'n parse not empty timestamp",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
        return null;
    }
}
