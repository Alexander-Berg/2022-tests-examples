package com.yandex.maps.testapp.mrc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoPhoto;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Direction;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.mrc.walklist.PreviewImagesAdapter;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.mrc.walk.CreatePlacemarkSession;
import com.yandex.mrc.walk.EditLocalPlacemarkSession;
import com.yandex.mrc.walk.LocalPlacemark;
import com.yandex.mrc.walk.LocalPlacemarkIdentifier;
import com.yandex.mrc.walk.PlacemarkData;
import com.yandex.mrc.walk.PlacemarkImage;
import com.yandex.mrc.walk.WalkManager;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlacemarkEditActivity extends BaseMrcActivity {
    public static final String INTENT_EXTRA_PLACEMARK_ID = "placemark_id";

    private Context context = this;
    private LocationManager locationManager;
    private Location lastKnownLocation;
    private WalkManager walkManager;
    private ImageDownloader imageDownloader;

    private MapView mapView;
    private Map map;

    private LocalPlacemarkIdentifier existingPlacemarkId;
    private LocalPlacemark existingPlacemark;
    private List<PlacemarkImage> addedImages = new ArrayList<>();
    private ArrayList<String> removedImageIds = new ArrayList<>();

    private RecyclerView previewImagesView;
    private PreviewImagesAdapter previewImagesAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private Spinner feedbackTypeSpinner;
    private EditText commentView;

    private PlacemarkMapObject objectPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mrc_placemark_edit);

        walkManager = MRCFactory.getInstance().getWalkManager();
        imageDownloader = MRCFactory.getInstance().getImageDownloader();
        locationManager = MapKitFactory.getInstance().createLocationManager();
        locationManager.subscribeForLocationUpdates(0.0, 100, 0, false, FilteringMode.ON, locationListener);

        mapView = findViewById(R.id.mapview);
        map = mapView.getMap();
        feedbackTypeSpinner = findViewById(R.id.feedback_type_spinner);
        commentView = findViewById(R.id.comment_input);

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        previewImagesView = findViewById(R.id.pedestrian_photos_list);
        previewImagesView.setLayoutManager(layoutManager);

        Intent intent = getIntent();
        if (intent.hasExtra(INTENT_EXTRA_PLACEMARK_ID)) {
            existingPlacemarkId = walkManager.deserializeLocalPlacemarkId(
                    intent.getStringExtra(INTENT_EXTRA_PLACEMARK_ID));
            existingPlacemark = getPlacemarkById(existingPlacemarkId);
            setExistingPlacemark();
        } else {
            createPreviewImagesAdapter(Collections.emptyList());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationManager.resume();
        map.addInputListener(inputListener);
        map.addCameraListener(cameraListener);

        if (objectPin == null) {
            Point point = existingPlacemark != null
                ? existingPlacemark.getData().getGeometry().getPoint() : null;

            objectPin = map.getMapObjects().addPlacemark(
                    point != null ? point : map.getCameraPosition().getTarget());
            objectPin.setIcon(
                    ImageProvider.fromResource(context, R.drawable.map_marker_blue_balloon),
                    new IconStyle().setAnchor(new PointF(0.0f, 0.0f)));
        }
    }

    @Override
    protected void onStop() {
        map.removeCameraListener(cameraListener);
        map.removeInputListener(inputListener);
        locationManager.suspend();
        super.onStop();
    }

    private LocalPlacemark getPlacemarkById(LocalPlacemarkIdentifier placemarkId) {
        if (placemarkId == null) {
            return null;
        }
        for (LocalPlacemark placemark : walkManager.getLocalPlacemarks()) {
            if (placemark.id().equals(placemarkId)) {
                return placemark;
            }
        }
        return null;
    }

    private void setExistingPlacemark() {
        PlacemarkData data = existingPlacemark.getData();
        commentView.setText(data.getComment());
        feedbackTypeSpinner.setSelection(getObjectTypePositionInSpinner(data.getFeedbackType()));
        Point point = data.getGeometry().getPoint();
        if (point != null) {
            if (objectPin != null) {
                objectPin.setGeometry(point);
            }
            moveToPosition(point);
        }

        setExistingPinObjectImages();
    }

    public void addFakeImage() {
        if (lastKnownLocation == null) {
            showUserMessage("Wait till current location is detected");
            return;
        }

        byte[] imageBytes = ImageUtils.readFakeImageFromAssets(this);
        if (imageBytes == null) {
            showUserMessage("Failed to read image");
            return;
        }

        long created = System.currentTimeMillis();
        String id = String.valueOf(created);

        Bitmap thumbnailBitmap = ImageUtils.decodeThumbnail(this, imageBytes);
        if (thumbnailBitmap == null) {
            showUserMessage("Failed to decide image");
            return;
        }

        double heading = lastKnownLocation.getHeading() != null ? lastKnownLocation.getHeading() : 0.0;
        GeoPhoto.ShootingPoint shootingPoint = new GeoPhoto.ShootingPoint(
                new GeoPhoto.Point3D(lastKnownLocation.getPosition(), lastKnownLocation.getAltitude()),
                new Direction(heading, 0.0)
        );

        addedImages.add(new PlacemarkImage(created, imageBytes, shootingPoint));
        if (previewImagesAdapter != null) {
            previewImagesAdapter.addImage(new PreviewImagesAdapter.PreviewImage(id, shootingPoint, thumbnailBitmap, null));
        } else {
            createPreviewImagesAdapter(makePreviewsFromImages(addedImages));
        }
    }

    private void createPreviewImagesAdapter(List<PreviewImagesAdapter.PreviewImage> images) {
        previewImagesAdapter = new PreviewImagesAdapter(
                imageDownloader,
                images,
                imageActionListener,
                PreviewImagesAdapter.IsEditable.YES);
        previewImagesView.setAdapter(previewImagesAdapter);
        previewImagesAdapter.notifyDataSetChanged();
    }

    private List<PreviewImagesAdapter.PreviewImage> makePreviewsFromImages(List<PlacemarkImage> images) {
        List<PreviewImagesAdapter.PreviewImage> previewImages = new ArrayList<>();
        for (PlacemarkImage image : images) {
            previewImages.add(makePreviewFromImage(image));
        }
        return previewImages;
    }

    private PreviewImagesAdapter.PreviewImage makePreviewFromImage(PlacemarkImage image) {
        return new PreviewImagesAdapter.PreviewImage(
                String.valueOf(image.getTakenAt()),
                image.getShootingPoint(),
                ImageUtils.decodeThumbnail(this, image.getImage()),
                null);
    }

    private PreviewImagesAdapter.PreviewImage makePreviewFromPlacemarkPhoto(PlacemarkData.Photo photo) {
        return new PreviewImagesAdapter.PreviewImage(
                String.valueOf(photo.getPhotoId()),
                photo.getGeoPhoto().getShootingPoint(),
                null,
                photo.getGeoPhoto().getImage());
    }

    public List<String> getRemovedImageIds() {
        return removedImageIds;
    }

    public List<PlacemarkImage> getAddedImages() {
        return addedImages;
    }

    public int getNumberOfImages() {
        return previewImagesAdapter != null ? previewImagesAdapter.getNumberOfImages() : 0;
    }

    public void setExistingPinObjectImages()
    {
        if (existingPlacemark == null) {
            return;
        }
        if (previewImagesAdapter == null) {
            createPreviewImagesAdapter(Collections.emptyList());
        }
        for (PlacemarkData.Photo photo : existingPlacemark.getData().getPhotos()) {
            if (!removedImageIds.contains(photo.getPhotoId())) {
                previewImagesAdapter.addImage(makePreviewFromPlacemarkPhoto(photo));
            }
        }
    }

    public void onSaveObjectClick(View view) {
        PlacemarkData.FeedbackType feedbackType = getSelectedFeedbackType();

        if (existingPlacemark == null) {
            walkManager.createPlacemark(
                    feedbackType,
                    Geometry.fromPoint(objectPin.getGeometry()),
                    commentView.getText().toString(),
                    getAddedImages(),
                    createPlacemarkListener);
        } else {
            existingPlacemark.update(
                    feedbackType,
                    Geometry.fromPoint(objectPin.getGeometry()),
                    commentView.getText().toString(),
                    getRemovedImageIds(),
                    getAddedImages(),
                    editLocalPlacemarkListener
            );
        }
    }

    private PlacemarkData.FeedbackType getSelectedFeedbackType()
    {
        int pos = feedbackTypeSpinner.getSelectedItemPosition();
        switch (pos) {
            case 0: return PlacemarkData.FeedbackType.ADDRESS_PLATE;
            case 1: return PlacemarkData.FeedbackType.BARRIER;
            case 2: return PlacemarkData.FeedbackType.BUILDING;
            case 3: return PlacemarkData.FeedbackType.BUILDING_ENTRANCE;
            case 4: return PlacemarkData.FeedbackType.BUSINESS_SIGN;
            case 5: return PlacemarkData.FeedbackType.BUSINESS_WORKING_HOURS;
            case 6: return PlacemarkData.FeedbackType.CYCLE_PATH;
            case 7: return PlacemarkData.FeedbackType.ENTRANCE_PLATE;
            case 8: return PlacemarkData.FeedbackType.FENCE;
            case 9: return PlacemarkData.FeedbackType.FOOT_PATH;
            case 10: return PlacemarkData.FeedbackType.PARKING;
            case 11: return PlacemarkData.FeedbackType.STAIRS;
            case 12: return PlacemarkData.FeedbackType.RAMP;
            case 13: return PlacemarkData.FeedbackType.ROOM;
            case 14: return PlacemarkData.FeedbackType.WALL;
            case 15: default: return PlacemarkData.FeedbackType.OTHER;
        }
    }

    private int getObjectTypePositionInSpinner(PlacemarkData.FeedbackType feedbackType) {
        switch (feedbackType) {
            case ADDRESS_PLATE: return 0;
            case BARRIER: return 1;
            case BUILDING: return 2;
            case BUILDING_ENTRANCE: return 3;
            case BUSINESS_SIGN: return 4;
            case BUSINESS_WORKING_HOURS: return 5;
            case CYCLE_PATH: return 6;
            case ENTRANCE_PLATE: return 7;
            case FENCE: return 8;
            case FOOT_PATH: return 9;
            case PARKING: return 10;
            case STAIRS: return 11;
            case RAMP: return 12;
            case ROOM: return 13;
            case WALL: return 14;
            case OTHER: default: return 15;
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationUpdated(@NonNull Location location) {
            if (lastKnownLocation == null && existingPlacemark == null) {
                moveToPosition(location.getPosition());
            }
            lastKnownLocation = location;
        }

        @Override
        public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {

        }
    };

    InputListener inputListener = new InputListener() {
        @Override
        public void onMapTap(@NonNull Map map, @NonNull Point point) {

        }

        @Override
        public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
            if (objectPin == null) {
                map.getMapObjects().addPlacemark(point);
            }
        }
    };

    CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(
                @NonNull Map map,
                @NonNull CameraPosition cameraPosition,
                @NonNull CameraUpdateReason reason,
                boolean finished) {
            if (objectPin != null) {
                objectPin.setGeometry(cameraPosition.getTarget());
            }
        }
    };

    CreatePlacemarkSession.CreatePlacemarkListener createPlacemarkListener = new CreatePlacemarkSession.CreatePlacemarkListener() {
        @Override
        public void onPlacemarkCreated(@NonNull LocalPlacemarkIdentifier localPlacemarkIdentifier) {
            showUserMessage("Created!");
            finish();
        }

        @Override
        public void onPlacemarkCreationError(@NonNull Error error) {
            showUserMessage("Failed to save placemark!");
        }
    };

    EditLocalPlacemarkSession.EditLocalPlacemarkListener editLocalPlacemarkListener = new EditLocalPlacemarkSession.EditLocalPlacemarkListener() {
        @Override
        public void onLocalPlacemarkUpdated(@NonNull LocalPlacemarkIdentifier localPlacemarkIdentifier) {
            showUserMessage("Updated!");
            finish();
        }

        @Override
        public void onLocalPlacemarkUpdateError(@NonNull Error error) {
            showUserMessage("Failed to save placemark");
        }
    };

    private void moveToPosition(Point point) {
        map.move(
                new CameraPosition(point,17.0f, /*azimuth=*/0.0f, /*tilt=*/0.0f),
                new Animation(Animation.Type.SMOOTH, 0.5f),
                null);
    }

    PreviewImagesAdapter.Listener imageActionListener = new PreviewImagesAdapter.Listener() {
        @Override
        public void addImage() {
            if (getNumberOfImages() >= 5) {
                showUserMessage("Too many photos");
            }
            addFakeImage();
        }

        @Override
        public void removeImage(String id) {
            // Check if this image was just added
            int addedPos = -1;
            for (int i = 0; i < addedImages.size(); ++i) {
                String imageId = String.valueOf(addedImages.get(i).getTakenAt());
                if (imageId.equals(id)) {
                    addedPos = i;
                    break;
                }
            }

            if (addedPos >= 0) {
                addedImages.remove(addedPos);
            } else {
                removedImageIds.add(id);
            }
        }
    };

    private void showUserMessage(String message) {
        Toast.makeText(PlacemarkEditActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
