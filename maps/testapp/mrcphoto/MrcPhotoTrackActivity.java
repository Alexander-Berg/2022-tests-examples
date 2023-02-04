package com.yandex.maps.testapp.mrcphoto;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoPhoto;
import com.yandex.mapkit.geometry.Direction;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.mrc.MrcPhotoTrack;
import com.yandex.mapkit.places.mrc.MrcPhotoTrackPlayer;
import com.yandex.mapkit.places.mrc.MrcPhotoTrackService;
import com.yandex.mapkit.places.mrc.MrcPhotoTrackView;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.common.internal.Serialization;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.util.logging.Logger;

public class MrcPhotoTrackActivity extends Activity
                                   implements MrcPhotoTrackPlayer.MrcPhotoTrackPlayerListener,
                                              MrcPhotoTrackService.MrcPhotoTrackListener,
                                              MapObjectTapListener {
    public static final String PARAM_TRACK_TYPE = "TRACK_TYPE";
    public static final String PARAM_TRACK_POLYLINE = "TRACK_POLYLINE";
    public static final String PARAM_POSITION = "TRACK_POSITION";
    private static final float ZINDEX = 30;

    private static Logger LOGGER = Logger.getLogger("com.yandex.maps.testapp.mrcphoto.MrcPhotoTrackActivity");

    private PolylinePosition startPosition = null;
    private MrcPhotoTrackService mrcPhotoTrackService = null;

    private MrcPhotoTrackView mrcPhotoTrackView;
    private MrcPhotoTrackPlayer mrcPhotoTrackPlayer;

    private RoutePolyline routePolyline = null;
    private MapView mapView = null;
    private Map map = null;

    private PlacemarkMapObject placemark = null;
    private Button playbackButton = null;
    private TextView infoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGGER.info("onCreate");

        setContentView(R.layout.mrc_photo_track);
        mrcPhotoTrackView = findViewById(R.id.mrc_photo_track_view);
        infoView = findViewById(R.id.photo_info_text);
        CheckBox showConnectionsCheckbox = findViewById(R.id.show_track_connections_checkbox);
        playbackButton = findViewById(R.id.mrc_track_playback_button);
        Spinner playbackSpeedSpinner = findViewById(R.id.mrc_track_playback_speed_spinner);
        mapView = findViewById(R.id.track_mapview);

        mrcPhotoTrackService = PlacesFactory.getInstance().createMrcPhotoTrackService();
        mrcPhotoTrackPlayer = mrcPhotoTrackView.getPlayer();
        routePolyline = new RoutePolyline(mapView);

        map = mapView.getMap();
        placemark = map.getMapObjects().addPlacemark(new Point(0.0f, 0.0f));
        placemark.setIcon(
                ImageProvider.fromResource(this, R.drawable.mrc_photo_marker),
                new IconStyle().setRotationType(RotationType.ROTATE));
        placemark.setVisible(false);

        map.move(
                new CameraPosition(new Point(59.945933, 30.320045), 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

        showConnectionsCheckbox = findViewById(R.id.show_track_connections_checkbox);
        showConnectionsCheckbox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                mrcPhotoTrackPlayer.enableMove();
            } else {
                mrcPhotoTrackPlayer.disableMove();
            }
        });

        playbackSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                final String[] speeds = getResources().getStringArray(R.array.mrc_photo_track_playback_speed);
                switch(speeds[position]) {
                    case "X1":
                        mrcPhotoTrackPlayer.setPlaybackSpeed(MrcPhotoTrackPlayer.PlaybackSpeed.X1);
                        break;
                    case "X2":
                        mrcPhotoTrackPlayer.setPlaybackSpeed(MrcPhotoTrackPlayer.PlaybackSpeed.X2);
                        break;
                    case "X4":
                        mrcPhotoTrackPlayer.setPlaybackSpeed(MrcPhotoTrackPlayer.PlaybackSpeed.X4);
                        break;
                    case "X8":
                        mrcPhotoTrackPlayer.setPlaybackSpeed(MrcPhotoTrackPlayer.PlaybackSpeed.X8);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        MrcPhotoTrack.TrackType trackType = (MrcPhotoTrack.TrackType)getIntent().getSerializableExtra(PARAM_TRACK_TYPE);

        Polyline trackPolyline = null;
        if (getIntent().getByteArrayExtra(PARAM_TRACK_POLYLINE) != null) {
            trackPolyline = Serialization.deserialize(
                    getIntent().getByteArrayExtra(PARAM_TRACK_POLYLINE), Polyline.class);
        }

        if(getIntent().getByteArrayExtra(PARAM_POSITION) != null) {
            startPosition = Serialization.deserialize(
                    getIntent().getByteArrayExtra(PARAM_POSITION), PolylinePosition.class);
        }


        if (trackType == null || trackPolyline == null || startPosition == null) {
            LOGGER.severe("Either MRC track type, or track polyline, or track position hasn't been provided in an intent extra");
            finish();
            return;
        }

        mrcPhotoTrackService.getMrcPhotoTrack(trackType, trackPolyline, this);
    }

    @Override
    public void onPhotoTrackResult(@NonNull MrcPhotoTrack mrcPhotoTrack) {
        routePolyline.setGeometry(mrcPhotoTrack.getTrackPolyline());
        routePolyline.subscribeForTaps(MrcPhotoTrackActivity.this);
        routePolyline.setVisible(true);
        routePolyline.focusOnRoute();

        mrcPhotoTrackPlayer.setPhotoTrack(mrcPhotoTrack);
        LOGGER.info("Set MRC photo track to the MRC photo track player");
        mrcPhotoTrackPlayer.openPhotoAt(startPosition);
        mrcPhotoTrackPlayer.enableMove();
    }

    @Override
    public void onPhotoTrackError(@NonNull Error error) {
        Toast.makeText(this, "Unable to get MRC photo track", Toast.LENGTH_LONG).show();
    }

    public void onTrackPlaybackTap(View view) {
        if(mrcPhotoTrackPlayer.isIsPlaying()) {
            mrcPhotoTrackPlayer.stop();
        } else {
            mrcPhotoTrackPlayer.play();
        }
    }

    @Override
    public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point point) {
        PolylinePosition position = mrcPhotoTrackPlayer.getPhotoTrack().snapToCoverage(point);
        if (position == null) {
            Toast.makeText(this, "There is no MRC photo at this point", Toast.LENGTH_SHORT).show();
            return false;
        }
        mrcPhotoTrackPlayer.openPhotoAt(position);
        return true;
    }

    @Override
    public void onUpdate(@NonNull MrcPhotoTrackPlayer player) {
        String photoId = player.getPhotoId();
        infoView.setText(photoId != null ? photoId : "");

        playbackButton.setText(player.isIsPlaying() ? R.string.stop : R.string.play);

        GeoPhoto.ShootingPoint shootingPoint = player.getShootingPoint();
        if (shootingPoint != null) {
            placemark.setGeometry(shootingPoint.getPoint().getPoint());

            Direction direction = shootingPoint.getDirection();
            placemark.setDirection(direction != null ? (float)direction.getAzimuth() : 0f);
            placemark.setVisible(true);

            CameraPosition cp = mapView.getMap().getCameraPosition();
            mapView.getMap().move(
                    new CameraPosition(
                            shootingPoint.getPoint().getPoint(),
                            cp.getZoom(),
                            cp.getAzimuth(),
                            cp.getTilt()),
                    new Animation(Animation.Type.SMOOTH, 0.0f),
                    null);
        } else {
            placemark.setVisible(false);
        }
    }

    @Override
    public void onError(@NonNull MrcPhotoTrackPlayer mrcPhotoTrackPlayer, @NonNull Error error) {
        final String text = "Got MRC Photo Track Player error: " + error.toString();
        LOGGER.severe(text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        LOGGER.info("onStop");
        routePolyline.unsubsribeFromTaps(this);
        mrcPhotoTrackPlayer.removeListener(this);
        mapView.onStop();
        mrcPhotoTrackView.onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LOGGER.info("onStart");
        mapView.onStart();
        mrcPhotoTrackView.onStart();
        mrcPhotoTrackPlayer.addListener(this);
        routePolyline.subscribeForTaps(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        routePolyline.onDestroy();
    }
}
