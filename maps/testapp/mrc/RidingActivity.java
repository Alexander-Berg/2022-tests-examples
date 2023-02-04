package com.yandex.maps.testapp.mrc;

import android.Manifest;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.mapkit.LocalizedValue;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.mapview.MapView;

import com.yandex.maps.testapp.R;

import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.mrc.camera.CameraManager;
import com.yandex.maps.testapp.mrc.camera.CaptureController;
import com.yandex.mrc.CaptureMode;
import com.yandex.mrc.CreateDrivingSessionListener;
import com.yandex.mrc.DeviceSpaceInfo;
import com.yandex.mrc.FreeDrivingListener;
import com.yandex.mrc.FreeDrivingSession;
import com.yandex.mrc.LocalRide;
import com.yandex.mrc.LocalRideListener;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.SensorsManager;
import com.yandex.mrc.WatchDeviceSpaceSession;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.Error;
import com.yandex.runtime.i18n.I18nManager;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RidingActivity extends BaseMrcActivity {
    private static final long CHECK_DEVICE_SPACE_PERIOD_MSEC = 5000;
    private static final long AVAILABLE_SPACE_LIMIT_BYTES = 128L * 1024 * 1024;
    private static final long AVAILABLE_SPACE_WARNING_BYTES = 256L * 1024 * 1024;

    private enum State { CAPTURE_OFF, CAPTURE_ON }

    private State state = State.CAPTURE_OFF;
    private boolean haveFreeSpace = true;

    private ImageView recIndicator;
    private Button startStopButton;
    private MapView mapview;
    private TextView photosCountTextView;
    private TextView rideDurationTextView;
    private TextView trackDistanceTextView;

    private LocationManager locationManager;
    private SensorsManager sensorsManager;
    private Guide guide;
    private LocationFollower locationFollower;

    private RideManager rideManager = null;
    private String rideId = null;
    private LocalRide localRide = null;
    private FreeDrivingSession drivingSession = null;
    private CaptureController captureController = null;
    private WatchDeviceSpaceSession watchDeviceSpaceSession;

    private PermissionsManager permissionsManager;

    private byte[] fakeMrcImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("RidingActivity.onCreate");
        setContentView(R.layout.activity_mrc_riding);

        startStopButton = findViewById(R.id.start_stop_button);
        startStopButton.setEnabled(false);

        mapview = findViewById(R.id.mapview);
        mapview.setNoninteractive(true);
        photosCountTextView = findViewById(R.id.photos_count);
        rideDurationTextView = findViewById(R.id.ride_duration);
        trackDistanceTextView = findViewById(R.id.track_distance);
        resetRideInfo();

        recIndicator = findViewById(R.id.fake_preview);
        recIndicator.bringToFront();

        permissionsManager = new PermissionsManager(this, permissionListener);

        rideManager = MRCFactory.getInstance().getRideManager();
        rideManager.newDrivingSession(createDrivingSessionListener);
        watchDeviceSpace();

        captureController = new CaptureController(cameraManager);
        locationManager = MapKitFactory.getInstance().createLocationManager();
        MapKitFactory.getInstance().setLocationManager(locationManager);

        sensorsManager = MRCFactory.getInstance().createSensorsManager();

        guide = DirectionsFactory.getInstance().createGuide();
        guide.setReroutingEnabled(false);

        locationFollower = new LocationFollower(this, guide, mapview.getMap());
        guide.subscribe(locationFollower);

        fakeMrcImage = ImageUtils.readFakeImageFromAssets(this);
    }

    @Override
    protected void onDestroy() {
        Logger.info("RidingActivity.onDestroy");
        if (guide != null) {
            guide.unsubscribe(locationFollower);
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.info("RidingActivity.onStart");

        if (AuthUtil.getCurrentAccount() == null) {
            showUserMessage(R.string.sign_into_account);
            finish();
        }
        mapview.onStart();
        showCaptureOff();
        startWithPermissions();
    }

    @Override
    protected void onStop() {
        Logger.info("RidingActivity.onStop");

        if (!isChangingConfigurations()) {
            stopWatchingDeviceSpace();
            suspendDrivingSession();

            captureController.finish();

            if (drivingSession != null && drivingSession.isValid()) {
                drivingSession.unsubscribe(freeDrivingListener);
            }
            unsubscribeFromRideUpdates();
        }

        guide.suspend();
        locationManager.suspend();
        mapview.onStop();

        super.onStop();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(
            int permsRequestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults){
        permissionsManager.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);
    }


    private void startWithPermissions() {
        if (!permissionsManager.haveCameraPermission()) {
            permissionsManager.requestCameraPermission();
            return;
        }

        if (!permissionsManager.haveLocationPermission()) {
            permissionsManager.requestLocationPermission();
            return;
        }

        locationManager.resume();
        guide.resume();

        captureController.start();
    }

    private void resumeDrivingSession() {
        showCaptureOn();
        state = State.CAPTURE_ON;

        if (drivingSession != null && drivingSession.isValid()) {
            drivingSession.resume();
            CaptureMode captureMode = drivingSession.getCaptureMode();
            Logger.info("capture mode: {" + captureMode.getIsOn() + ", " + captureMode.getPeriod() + "}");
            recIndicator.setVisibility(captureMode.getIsOn() ? View.VISIBLE : View.GONE);
            captureController.setCaptureMode(captureMode.getIsOn(), captureMode.getPeriod());
        }
    }

    private void suspendDrivingSession() {
        if (state == State.CAPTURE_ON) {
            showCaptureOff();
            state = State.CAPTURE_OFF;
        }
        captureController.setCaptureMode(false, 0);

        if (drivingSession != null && drivingSession.isValid()) {
            drivingSession.suspend();
        }
    }

    public void onControlClick(View view) {
        switch (state) {
            case CAPTURE_OFF:
                resumeDrivingSession();
                break;
            case CAPTURE_ON:
                suspendDrivingSession();
                break;
        }
    }

    private void showCaptureOff() {
        startStopButton.setText(R.string.mrc_start_capture);
        recIndicator.setVisibility(View.GONE);

        if (!haveFreeSpace) {
            disableControlButton();
        }
    }

    private void showCaptureOn() {
        startStopButton.setText(R.string.mrc_stop_capture);
    }

    private void disableControlButton() {
        startStopButton.setEnabled(false);
    }

    private void enableControlButton() {
        startStopButton.setEnabled(true);
    }

    protected void showUserMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showUserMessage(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show();
    }

    private void onFreeSpaceIsEnough(long bytesLeft) {
        if (state == State.CAPTURE_OFF && !haveFreeSpace) {
            enableControlButton();
        }
        haveFreeSpace = true;
    }

    private void onFreeSpaceIsLow(long bytesLeft) {
        final I18nManager i18nManager = I18nManagerFactory.getI18nManagerInstance();
        final String message = getString(R.string.mrc_free_space_low, i18nManager.localizeDataSize(bytesLeft));
        showUserMessage(message);
        haveFreeSpace = true;
    }

    private void onFreeSpaceIsOver() {
        if (state == State.CAPTURE_ON) {
            suspendDrivingSession();
        }
        disableControlButton();
        showUserMessage(getString(R.string.mrc_free_space_over));
        haveFreeSpace = false;
    }

    private void watchDeviceSpace() {
        watchDeviceSpaceSession = MRCFactory.getInstance().getStorageManager().watchDeviceSpace(
                new WatchDeviceSpaceSession.WatchDeviceSpaceSessionListener() {
                    @Override
                    public void onError(@NonNull Error error) {
                        Logger.error("Device space watching failed: " + error);
                        showUserMessage("Device space watching failed");
                        finish();
                    }

                    @Override
                    public void onDeviceSpaceUpdated(@NonNull DeviceSpaceInfo info) {
                        final long availableBytes = info.getAvailableBytes();

                        if (availableBytes <= AVAILABLE_SPACE_LIMIT_BYTES) {
                            onFreeSpaceIsOver();
                        } else if (availableBytes <= AVAILABLE_SPACE_WARNING_BYTES) {
                            onFreeSpaceIsLow(availableBytes - AVAILABLE_SPACE_LIMIT_BYTES);
                        } else {
                            onFreeSpaceIsEnough(availableBytes - AVAILABLE_SPACE_LIMIT_BYTES);
                        }
                    }
                },
                CHECK_DEVICE_SPACE_PERIOD_MSEC);
    }

    private void stopWatchingDeviceSpace() {
        if (watchDeviceSpaceSession != null) {
            watchDeviceSpaceSession.cancel();
            watchDeviceSpaceSession = null;
        }
    }

    private LocalRide getLocalRideById(String rideId) {
        for (LocalRide ride: rideManager.getLocalRides()) {
            if (ride.isValid() && ride.getBriefRideInfo().getId().equals(rideId)) {
                return ride;
            }
        }
        return null;
    }

    private void unsubscribeFromRideUpdates() {
        if (drivingSession != null && drivingSession.isValid()) {
            drivingSession.unsubscribe(freeDrivingListener);
        }
        if (localRide != null && localRide.isValid()) {
            localRide.unsubscribe(localRideListener);
        }
    }

    private void resetRideInfo() {
        photosCountTextView.setText(getString(R.string.mrc_photos_count, 0));
        rideDurationTextView.setText(getString(R.string.mrc_ride_duration, "0"));
        trackDistanceTextView.setText(getString(R.string.mrc_ride_track_distance, "0"));
    }

    //
    // LISTENERS
    //

    private final FreeDrivingListener freeDrivingListener = new FreeDrivingListener() {
        @Override
        public void onCaptureModeUpdated() {
            CaptureMode captureMode = drivingSession.getCaptureMode();
            Logger.info("capture mode: {" + captureMode.getIsOn() + ", " + captureMode.getPeriod() + "}");
            recIndicator.setVisibility(captureMode.getIsOn() ? View.VISIBLE : View.GONE);
            captureController.setCaptureMode(captureMode.getIsOn(), captureMode.getPeriod());
        }

        @Override
        public void onError(@NonNull Error error) {
            Logger.error("Driving session error: " + error);
            showUserMessage("Driving session error");
        }
    };

    private final CreateDrivingSessionListener createDrivingSessionListener
            = new CreateDrivingSessionListener() {
        @Override
        public void onDrivingSessionCreated(@NonNull FreeDrivingSession session) {
            Logger.info("New driving session created: " + session.getRideId());
            drivingSession = session;
            drivingSession.setLocationManager(locationManager);
            drivingSession.setSensorsManager(sensorsManager);
            drivingSession.subscribe(freeDrivingListener);

            rideId = drivingSession.getRideId();
            localRide = getLocalRideById(rideId);
            if (localRide != null) {
                localRide.subscribe(localRideListener);
            }

            enableControlButton();
        }

        @Override
        public void onError(@NonNull Error error) {
            Logger.error("Failed to create driving session: " + error);
            showUserMessage("Failed to create driving session");
        }
    };

    private final CameraManager cameraManager = new CameraManager() {
        @Override
        public void takePicture() {
            Logger.info("Saving new picture");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (drivingSession != null && drivingSession.isValid()) {
                        drivingSession.savePhoto(fakeMrcImage, 0L);
                    }
                }
            });
        }
    };

    private final LocalRideListener localRideListener = new LocalRideListener() {
        @Override
        public void onRideChanged(@NonNull LocalRide ride) {
            if (localRide.getBriefRideInfo().getId().equals(ride.getBriefRideInfo().getId())) {
                long photosCount = ride.getLocalPhotosCount();
                photosCountTextView.setText(getString(R.string.mrc_photos_count, photosCount));
                LocalizedValue duration = ride.getBriefRideInfo().getDuration();
                if (duration != null) {
                    rideDurationTextView.setText(getString(R.string.mrc_ride_duration, duration.getText()));
                }
                LocalizedValue distance = ride.getBriefRideInfo().getTrackDistance();
                if (distance != null) {
                    trackDistanceTextView.setText(getString(R.string.mrc_ride_track_distance, distance.getText()));
                }
            }
        }

        @Override
        public void onRideError(@NonNull LocalRide ride, @NonNull Error error) {
            showUserMessage("Local ride error: " + error.toString());
        }
    };

    private final PermissionsManager.Listener permissionListener = new PermissionsManager.Listener() {
        @Override
        public void onPermissionGranted(String permission) {
            Logger.info("Permission granted: " + permission);
            startWithPermissions();
        }

        @Override
        public void onPermissionDenied(String permission) {
            Logger.warn("Permission denied: " + permission);
            String message = null;
            if (permission.equals(Manifest.permission.CAMERA)) {
                message = "Camera permission not granted";
            } else if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                message = "Location permission not granted";
            }

            if (message != null) {
                showUserMessage(message);
                finish();
            }
        }
    };
}
