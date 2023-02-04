package com.yandex.maps.testapp.mrc;

import static com.yandex.maps.testapp.mrc.DetailedRideActivity.INTENT_EXTRA_LOCAL_RIDE_ID;
import static com.yandex.maps.testapp.mrc.DetailedRideActivity.INTENT_EXTRA_SERVER_RIDE_ID;
import static com.yandex.maps.testapp.mrc.ImageUtils.getImageSize;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoPhoto;
import com.yandex.mapkit.Image;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.geo.PolylineIndex;
import com.yandex.mapkit.geometry.geo.PolylineUtils;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.mrc.DetailedRide;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ImageSession;
import com.yandex.mrc.LocalRideIdentifier;
import com.yandex.mrc.OpenRideSession;
import com.yandex.mrc.RideEditSession;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.RidePhoto;
import com.yandex.mrc.RidePhotoLoadingSession;
import com.yandex.mrc.ServerRideIdentifier;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.util.List;


public class DetailedRidePhotosActivity extends BaseMrcActivity {
    private static final String TAG = "DetailedRidePhotos";

    private Context context;
    private RideManager rideManager;
    private ImageDownloader imageDownloader;
    private DetailedRide detailedRide = null;

    private OpenRideSession openRideSession = null;
    private RidePhotoLoadingSession photoLoadingSession = null;
    private ImageSession imageLoadingSession = null;

    private LocalRideIdentifier localRideIdentifier;
    private ServerRideIdentifier serverRideIdentifier;
    private RidePhoto currentPhoto = null;
    private Float currentPhotoPos;

    private RidePhoto deleteFromPhoto = null;
    private RidePhoto deleteTillPhoto = null;

    private Map map;
    private MapObjectCollection mapObjectCollection;
    private PlacemarkMapObject currentPositionPin;
    private PolylineMapObject trackPolyline;
    private PolylineIndex polylineIndex;

    private MapView mapview;
    private ImageView ridePhoto;
    private SeekBar seekBar;
    private TextView deleteFromPhotoIdView;
    private TextView deleteTillPhotoIdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_mrc_detailed_ride_photos);

        context = this;
        rideManager = MRCFactory.getInstance().getRideManager();
        imageDownloader = MRCFactory.getInstance().getImageDownloader();

        mapview = findViewById(R.id.mapview);
        ridePhoto = findViewById(R.id.current_ride_photo);
        seekBar = findViewById(R.id.photo_seek_bar);
        seekBar.setOnSeekBarChangeListener(seekBarListener);

        deleteFromPhotoIdView = findViewById(R.id.delete_from_photo_id);
        deleteTillPhotoIdView = findViewById(R.id.delete_till_photo_id);

        map = mapview.getMap();
        mapObjectCollection = map.getMapObjects().addCollection();

        Intent intent = getIntent();
        if (intent.hasExtra(INTENT_EXTRA_LOCAL_RIDE_ID)) {
            localRideIdentifier = rideManager.deserializeLocalRideId(
                intent.getStringExtra(INTENT_EXTRA_LOCAL_RIDE_ID));
            openRideSession = rideManager.openRide(localRideIdentifier, openRideListener);
        } else if (intent.hasExtra(INTENT_EXTRA_SERVER_RIDE_ID)) {
            serverRideIdentifier = rideManager.deserializeServerRideId(
                    intent.getStringExtra(INTENT_EXTRA_SERVER_RIDE_ID));
            openRideSession = rideManager.openRide(serverRideIdentifier, openRideListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

        if (AuthUtil.getCurrentAccount() == null) {
            showUserMessage(R.string.sign_into_account);
            finish();
        }
        mapview.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        mapview.onStop();
        super.onStop();
    }

    void setCurrentRide(@NonNull DetailedRide ride) {
        detailedRide = ride;

        Polyline track = detailedRide.getTrack();
        if (trackPolyline == null) {
            trackPolyline = mapObjectCollection.addPolyline(track);
        } else {
            trackPolyline.setGeometry(track);
        }

        trackPolyline.setStrokeColor(Color.argb(255, 0, 0, 255));
        focusOnTrack(track);
        polylineIndex = PolylineUtils.createPolylineIndex(track);

        currentPhoto = null;
        openFirstPhoto();

        deleteFromPhoto = null;
        deleteTillPhoto = null;
        deleteFromPhotoIdView.setText("");
        deleteTillPhotoIdView.setText("");
    }

    OpenRideSession.OpenRideListener openRideListener = new OpenRideSession.OpenRideListener() {
        @Override
        public void onRideOpened(@NonNull DetailedRide detailedRide) {
            setCurrentRide(detailedRide);
        }

        @Override
        public void onOpenRideError(@NonNull Error error) {
            Log.e(TAG, "Failed to open detailed ride: " + error);
            showUserMessage("Failed to open detailed ride");
            finish();
        }
    };

    SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                jumpToPosition((float)progress / 100f);
            }
        }

        @Override public void onStartTrackingTouch(SeekBar seekBar) { }
        @Override public void onStopTrackingTouch(SeekBar seekBar) { }
    };

    public void onNextPhotoClick(View view) {
        if (currentPhoto == null) {
            return;
        }
        loadNextPhoto();
    }

    public void onPrevPhotoClick(View view) {
        if (currentPhoto == null) {
            return;
        }
        loadPrevPhoto();
    }

    public void onSetDeleteFrom(View view) {
        if (currentPhoto == null) {
            return;
        }
        if (deleteTillPhoto != null && currentPhoto.getImageStreamPosition() > deleteTillPhoto.getImageStreamPosition()) {
            showUserMessage("FROM photo must precede TILL photo");
            return;
        }
        deleteFromPhoto = currentPhoto;
        deleteFromPhotoIdView.setText(deleteFromPhoto.getId());
    }

    public void onClearDeleteFrom(View view) {
        deleteFromPhoto = null;
        deleteFromPhotoIdView.setText("");
    }

    public void onSetDeleteTill(View view) {
        if (currentPhoto == null) {
            return;
        }
        if (deleteFromPhoto != null && currentPhoto.getImageStreamPosition() < deleteFromPhoto.getImageStreamPosition()) {
            showUserMessage("FROM photo must precede TILL photo");
            return;
        }
        deleteTillPhoto = currentPhoto;
        deleteTillPhotoIdView.setText(deleteTillPhoto.getId());
    }

    public void onClearDeleteTill(View view) {
        deleteTillPhoto = null;
        deleteTillPhotoIdView.setText("");
    }

    public void onDeletePhotos(View view) {
        if (deleteFromPhoto == null || deleteTillPhoto == null) {
            return;
        }

        detailedRide.deletePhotos(
                deleteFromPhoto.getId(),
                deleteTillPhoto.getId(),
                new RideEditSession.RideEditListener() {
                    @Override
                    public void onRideUpdated(@NonNull DetailedRide detailedRide) {
                        setCurrentRide(detailedRide);
                    }

                    @Override
                    public void onRideUpdateError(@NonNull Error error) {
                        Log.e(TAG, "Failed to delete ride photos: " + error);
                        showUserMessage("Failed to delete ride photos");
                    }
                });
    }


    private void jumpToPosition(float position) {
        loadPhotoAtPosition(position);
    }

    private void openFirstPhoto() {
        photoLoadingSession = detailedRide.loadPhotos(null, 0, 1, new RidePhotoLoadingSession.RidePhotoListener() {
            @Override
            public void onPhotosLoaded(@NonNull List<RidePhoto> photos) {
                if (photos.isEmpty()) {
                    return;
                }
                RidePhoto photo = photos.get(0);
                applyPhoto(photo);
            }

            @Override
            public void onPhotoLoadingError(@NonNull Error error) {
                showUserMessage("Failed to load first photo");
            }
        });
    }

    private void loadNextPhoto() {
        photoLoadingSession = detailedRide.loadPhotos(currentPhoto.getId(), 0, 1, new RidePhotoLoadingSession.RidePhotoListener() {
            @Override
            public void onPhotosLoaded(@NonNull List<RidePhoto> photos) {
                if (photos.size() < 2) {
                    return;
                }
                RidePhoto photo = photos.get(1);
                applyPhoto(photo);
            }

            @Override
            public void onPhotoLoadingError(@NonNull Error error) {
                showUserMessage("Failed to load photo after id " + currentPhoto.getId());
            }
        });
    }

    private void loadPrevPhoto() {
        photoLoadingSession = detailedRide.loadPhotos(currentPhoto.getId(), 1, 0, new RidePhotoLoadingSession.RidePhotoListener() {
            @Override
            public void onPhotosLoaded(@NonNull List<RidePhoto> photos) {
                if (photos.size() < 2) {
                    return;
                }
                RidePhoto photo = photos.get(0);
                applyPhoto(photo);
            }

            @Override
            public void onPhotoLoadingError(@NonNull Error error) {
                showUserMessage("Failed to load photo before id " + currentPhoto.getId());
            }
        });
    }

    private void loadPhotoAtPosition(float position) {
        photoLoadingSession = detailedRide.seekPhoto(position, new RidePhotoLoadingSession.RidePhotoListener() {
            @Override
            public void onPhotosLoaded(@NonNull List<RidePhoto> photos) {
                if (photos.isEmpty()) {
                    ridePhoto.setImageBitmap(null);
                    return;
                }
                RidePhoto photo = photos.get(0);
                applyPhoto(photo);
            }

            @Override
            public void onPhotoLoadingError(@NonNull Error error) {
                showUserMessage("Failed to load photo at position " + position);
            }
        });
    }

    private void applyPhoto(RidePhoto photo) {
        currentPhoto = photo;
        currentPhotoPos = photo.getImageStreamPosition();
        seekBar.setProgress((int)(currentPhotoPos * 100));
        updateShootingPosition(photo);
        loadPhotoImage(photo.getImage());
    }

    private void loadPhotoImage(@NonNull Image image) {
        if (imageLoadingSession != null) {
            imageLoadingSession.cancel();
        }

        Image.ImageSize imageSize = getImageSize(image, "original");
        if (imageSize == null) {
            Log.e(TAG, "Missing image sizes for image " + image.getUrlTemplate());
            return;
        }

        imageLoadingSession = imageDownloader.loadImageBitmap(
                image.getUrlTemplate(),
                imageSize,
                new ImageSession.ImageListener() {
                    @Override
                    public void onImageLoaded(@NonNull Bitmap bitmap) {
                        ridePhoto.setImageBitmap(bitmap);
                        ridePhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }

                    @Override
                    public void onImageLoadingError(@NonNull Error error) {
                        Log.e(TAG, "Failed to load image: " + image.getUrlTemplate());
                    }
                });
    }

    void updateShootingPosition(RidePhoto ridePhoto) {
        GeoPhoto.ShootingPoint shootingPoint = ridePhoto.getShootingPoint();
        if (shootingPoint == null || shootingPoint.getDirection() == null) {
            if (currentPositionPin != null) {
                mapObjectCollection.remove(currentPositionPin);
                currentPositionPin = null;
            }
            return;
        }

        Point point = shootingPoint.getPoint().getPoint();
        float heading = (float)shootingPoint.getDirection().getAzimuth();

        if (currentPositionPin == null) {
            currentPositionPin = mapObjectCollection.addPlacemark(point);
            currentPositionPin.setZIndex(20f);
            Bitmap arrowIcon = ImageProvider.fromResource(context, R.drawable.navigation_icon).getImage();
            currentPositionPin.setIcon(
                    ImageProvider.fromBitmap(arrowIcon),
                    new IconStyle().setFlat(true).setRotationType(RotationType.ROTATE));
        } else {
            currentPositionPin.setGeometry(point);
        }
        currentPositionPin.setDirection(heading);
    }

    private void focusOnTrack(@NonNull Polyline track) {
        if (track.getPoints().isEmpty()) {
            return;
        }

        CameraPosition cameraPosition = map.cameraPosition(BoundingBoxHelper.getBounds(track));
        map.move(cameraPosition, new Animation(Animation.Type.SMOOTH, 0.5f), null);
    }

    private void showUserMessage(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show();
    }

    private void showUserMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
