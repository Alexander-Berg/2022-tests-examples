package com.yandex.naviprovider.naviprovider_app;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.yandex.mapkit.LocalizedValue;
import com.yandex.mapkit.directions.guidance.DisplayedAnnotations;

import com.yandex.maps.naviprovider.AppType;
import com.yandex.maps.naviprovider.Bookmark;
import com.yandex.maps.naviprovider.BuildRouteError;
import com.yandex.maps.naviprovider.NaviProviderFactory;
import com.yandex.maps.naviprovider.NaviProvider;
import com.yandex.maps.naviprovider.NaviClient;
import com.yandex.maps.naviprovider.Place;
import com.yandex.maps.naviprovider.RegionStatus;
import com.yandex.maps.naviprovider.RegionStatusV2;
import com.yandex.maps.naviprovider.SoundScheme;
import com.yandex.maps.naviprovider.SoundSchemes;

import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.directions.guidance.FasterAlternative;
import com.yandex.mapkit.MapKitFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NaviClient {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapKitFactory.setApiKey("eed83508-0ddc-4f23-b426-d2f90b46cad2");
        NaviProviderFactory.initialize(this);

        route_ = (TextView)findViewById(R.id.route);
        routePosition_ = (TextView)findViewById(R.id.route_position);
        speedLimit_ = (TextView)findViewById(R.id.speed_limit);
        speedLimitExceeded_ = (TextView)findViewById(R.id.speed_limit_exceeded);
        annotations_ = (TextView)findViewById(R.id.annotations);
        placesDatabaseStatus_ = (TextView)findViewById(R.id.places_db);
        home_ = (TextView)findViewById(R.id.home);
        work_ = (TextView)findViewById(R.id.work);
        bookmarks_ = (TextView)findViewById(R.id.bookmarks);
        version_ = (TextView)findViewById(R.id.version);
        minorVersion_ = (TextView)findViewById(R.id.minor_version);
        prevPathLength_ = (TextView)findViewById(R.id.prevPathLength);
        roadName_ = (TextView)findViewById (R.id.road_name);
        confirmationStatus_ = (TextView)findViewById(R.id.confirmation_status);
        fasterAlternative_ = (TextView)findViewById(R.id.faster_alternative);
        soundSchemes_ = (TextView)findViewById(R.id.sound_schemes);
        intent_ = (EditText)findViewById(R.id.intent);
        connectionStatus_ = (TextView)findViewById(R.id.connection_status);
        regionUpdates_ = (TextView)findViewById(R.id.region_updates);
        regionUpdatesV2_ = (TextView)findViewById(R.id.region_updates_v2);
        clearOfflineCache_ = (TextView)findViewById(R.id.clear_offline_cache);

        version_.setText("Client version: " +
            NaviProvider.naviClientManager().getClientVersion());
        minorVersion_.setText("Client minor version: " +
                NaviProvider.naviClientManager().getClientMinorVersion());

        final Button connectButton = (Button)findViewById(R.id.connect_button);
        connectButton.setEnabled(!NaviProvider.naviClientManager().isConnected());

        final RadioButton storeButton = (RadioButton)findViewById(R.id.radio_button_store);
        final RadioButton sandboxButton = (RadioButton)findViewById(R.id.radio_button_sandbox);
        final RadioButton inhouseButton = (RadioButton)findViewById(R.id.radio_button_inhouse);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectButton.setEnabled(false);

                AppType appType;
                if (storeButton.isChecked()) {
                    appType = AppType.STORE;
                } else if (sandboxButton.isChecked()) {
                    appType = AppType.SANDBOX;
                } else {
                    appType = AppType.INHOUSE;
                }
                boolean ok = NaviProvider.naviClientManager().connect(appType);
                if (!ok) {
                    route_.setText("Service is not found");
                    connectButton.setEnabled(true);
                } else {
                    NaviProvider.naviClientManager().addListener(MainActivity.this);
                }
            }
        });

        final Button clearRouteButton = (Button)findViewById(R.id.clear_route_button);
        clearRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NaviProvider.naviClientManager().sendIntent(
                    "yandexnavi://set_route?route_bytes=");
            }
        });

        Button sendIntentButton = (Button)findViewById(R.id.send_intent_button);
        sendIntentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NaviProvider.naviClientManager().sendIntent(intent_.getText().toString());
            }
        });

        CheckBox needBackgroundGuidance = (CheckBox)findViewById(R.id.need_background_guidance_checkbox);
        needBackgroundGuidance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                NaviProvider.naviClientManager().setNeedBackgroundGuidance(checked);
            }
        });
    }

    @Override
    public void onRouteChanged(DrivingRoute route) {
        route_.setText("Route " + getRouteDescription(route));
    }

    @Override
    public void onRoutePositionUpdated(PolylinePosition routePosition) {
        if (routePosition != null) {
            routePosition_.setText(
                    "Route position: " + routePosition.getSegmentIndex() + " "
                            + routePosition.getSegmentPosition());
        } else {
            routePosition_.setText("No route position");
        }
    }

    @Override
    public void onAnnotationsUpdated(DisplayedAnnotations annotations) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < annotations.getAnnotations().size(); i++) {
            s.append(annotations.getAnnotations().get(i).getAnnotation().getDescriptionText());
            s.append(" ");
            s.append(annotations.getAnnotations().get(i).getDistance().getText());
            s.append(" ");
        }
        s.append(annotations.getNextRoadName());
        annotations_.setText(s.toString());
    }

    @Override
    public void onSpeedLimitUpdated(LocalizedValue speedLimit) {
        if (speedLimit != null) {
            speedLimit_.setText("Speed limit: " + speedLimit.getText());
        } else {
            speedLimit_.setText("No speed limit");
        }
    }

    @Override
    public void onSpeedLimitExceeded(boolean speedLimitExceeded) {
        speedLimitExceeded_.setText("Speed limit is " + (speedLimitExceeded ? "" : "not ") + "exceeded");
    }

    @Override
    public void onHomeUpdated(Place home) {
        home_.setText("Home: " + getPlaceDescription(home));
    }

    @Override
    public void onWorkUpdated(Place work) {
        work_.setText("Work: " + getPlaceDescription(work));
    }

    @Override
    public void onBookmarksUpdated(List<Bookmark> bookmarks) {
        StringBuilder s = new StringBuilder();
        s.append("Bookmarks:\n");
        if(bookmarks != null) {
            for (Bookmark bookmark : bookmarks) {
                s.append(bookmark.getTitle() + "\n");
                s.append(bookmark.getDescriptionText() + "\n");
                s.append(bookmark.getUri()+ "\n\n");
            }
        }
        bookmarks_.setText(s.toString());
    }

    @Override
    public void onPlacesDatabaseReadyUpdated(boolean ready) {
        placesDatabaseStatus_.setText(
            "Places database is " + (ready ? "" : "not ") + "ready");
    }

    @Override
    public void onServerVersionReceived(int version) {
        version_.setText("Client version: " +
            NaviProvider.naviClientManager().getClientVersion() +
            "\nServer version: " + version);
    }

    @Override
    public void onServerMinorVersionReceived(int version) {
        minorVersion_.setText("Client minor version: " +
                NaviProvider.naviClientManager().getClientMinorVersion() +
                "\nServer minor version: " + version);
    }

    @Override
    public void onPrevPathLengthChanged(double prevPathLength) {
        prevPathLength_.setText("PrevPathLength: " + prevPathLength);
    }

    @Override
    public void onRoadNameUpdated(String roadName) {
        roadName_.setText("Road name: " + roadName);
    }

    @Override
    public void onConfirmationStatusUpdated(boolean needConfirmation) {
        confirmationStatus_.setText("Need confirmation: " + needConfirmation);
    }

    @Override
    public void onFasterAlternativeUpdated(FasterAlternative fasterAlternative) {
        if (fasterAlternative == null) {
            fasterAlternative_.setText("No faster alternative");
            return;
        }
        fasterAlternative_.setText("Faster alternative: route " +
            getRouteDescription(fasterAlternative.getRoute()) +
            " -" + fasterAlternative.getTimeDifference().getText());
    }

    @Override
    public void onSoundSchemesUpdated(SoundSchemes soundSchemes) {
        StringBuilder s = new StringBuilder();
        s.append("Sound schemes:\n");
        if (soundSchemes != null) {
            for (SoundScheme scheme : soundSchemes.getSchemes()) {
                s.append(scheme.getName() + "\n");
                s.append(scheme.getCommentary() + "\n");
                s.append(scheme.getInternalName()+ "\n\n");
            }
            s.append("Selected scheme: " + soundSchemes.getSelected());
        }
        soundSchemes_.setText(s.toString());
    }

    @Override
    public void onRequestedRoutesReceived(List<DrivingRoute> routes) {
        StringBuilder s = new StringBuilder("Received routes:\n");
        for (DrivingRoute route : routes) {
            if (route != null) {
                s.append(getRouteDescription(route) + "\n");
            } else {
                s.append("Can't deserialize the route\n");
            }
        }

        showMessageBox(s.toString());
    }

    public void onBuildRouteError(BuildRouteError error) {
        showMessageBox(error.toString());
    }

    public void onRegionStatusUpdated(RegionStatus regionStatus)
    {
        regionUpdates_.setText(
                "RegionId: " + regionStatus.getRegionId() +
                " state: " + regionStatus.getState() +
                " progress: " + regionStatus.getProgress() +
                " error: " + regionStatus.getError());
    }

    public void onRegionStatusUpdatedV2(RegionStatusV2 regionStatus)
    {
        regionUpdatesV2_.setText(
            "RegionId: " + regionStatus.getRegionId() +
            " state: " + regionStatus.getState() +
            " progress: " + regionStatus.getProgress() +
            " error: " + regionStatus.getError() +
            " outdated: " + regionStatus.getOutdated());
    }

    public void onClearOfflineCacheFinished()
    {
        clearOfflineCache_.setText("Offline cache is cleared");
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        connectionStatus_.setText("Connection status: " + connected);
    }

    private void showMessageBox(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.show();
    }

    private String getRouteDescription(DrivingRoute route) {
        if (route != null) {
            return "length is " +
                route.getMetadata().getWeight().getDistance().getText();
        }
        return "is null";
    }

    private String getPlaceDescription(Place place) {
        StringBuilder s = new StringBuilder();
        if (place != null) {
            s.append(place.getPosition().getLatitude() +
                " " + place.getPosition().getLongitude());
            if (place.getAddress() != null) {
                s.append(" " + place.getAddress());
            }
            if (place.getShortAddress() != null) {
                s.append(" " + place.getShortAddress());
            }
        }
        return s.toString();
    }

    private TextView route_;
    private TextView routePosition_;
    private TextView speedLimit_;
    private TextView speedLimitExceeded_;
    private TextView annotations_;
    private TextView placesDatabaseStatus_;
    private TextView home_;
    private TextView work_;
    private TextView bookmarks_;
    private TextView version_;
    private TextView minorVersion_;
    private TextView prevPathLength_;
    private TextView roadName_;
    private TextView confirmationStatus_;
    private TextView fasterAlternative_;
    private TextView soundSchemes_;
    private EditText intent_;
    private TextView connectionStatus_;
    private TextView regionUpdates_;
    private TextView regionUpdatesV2_;
    private TextView clearOfflineCache_;
}
