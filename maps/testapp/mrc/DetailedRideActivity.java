package com.yandex.maps.testapp.mrc;

import static com.yandex.maps.testapp.mrc.ImageUtils.getImageSize;
import static com.yandex.maps.testapp.mrc.ImageUtils.scaleBitmap;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.Image;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.geo.PolylineIndex;
import com.yandex.mapkit.geometry.geo.PolylineUtils;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.mrc.BriefRideInfo;
import com.yandex.mrc.DetailedRide;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ImageSession;
import com.yandex.mrc.LocalRideIdentifier;
import com.yandex.mrc.OpenRideSession;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.ServerRideIdentifier;
import com.yandex.mrc.TrackPreviewItem;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.Error;
import com.yandex.runtime.ui_view.ViewProvider;

import java.util.List;


public class DetailedRideActivity extends BaseMrcActivity {
    private static final String TAG = "DetailedRide";
    public static final String INTENT_EXTRA_LOCAL_RIDE_ID = "local_ride_id";
    public static final String INTENT_EXTRA_SERVER_RIDE_ID = "server_ride_id";

    private RideManager rideManager;
    private ImageDownloader imageDownloader;
    private DetailedRide detailedRide = null;

    private OpenRideSession openRideSession = null;
    private ImageSession imageLoadingSession = null;

    private LocalRideIdentifier localRideIdentifier;
    private ServerRideIdentifier serverRideIdentifier;

    private Map map;
    private MapObjectCollection mapObjectCollection;
    private PlacemarkMapObject imagePin;
    private PolylineIndex polylineIndex;

    private MapView mapview;
    private TextView rideIdView;
    private TextView rideStatusView;
    private TextView photosCountView;
    private TextView rideDurationView;
    private TextView trackDistanceView;
    private TextView hypothesesCountView;

    private RecyclerView previewImagesView;
    private RidePreviewImagesAdapter previewImagesAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_mrc_detailed_ride);

        rideManager = MRCFactory.getInstance().getRideManager();
        imageDownloader = MRCFactory.getInstance().getImageDownloader();

        mapview = findViewById(R.id.mapview);
        rideIdView = findViewById(R.id.ride_id);
        rideStatusView = findViewById(R.id.ride_status);
        photosCountView = findViewById(R.id.photos_count);
        rideDurationView = findViewById(R.id.ride_duration);
        trackDistanceView = findViewById(R.id.track_distance);
        hypothesesCountView = findViewById(R.id.hypotheses_count);

        map = mapview.getMap();
        mapObjectCollection = map.getMapObjects().addCollection();

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        previewImagesView = findViewById(R.id.ride_photo_preview_list);
        previewImagesView.setLayoutManager(layoutManager);

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
        map.addInputListener(mapInputListener);

        updateRideInfoInUi();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        map.removeInputListener(mapInputListener);
        mapview.onStop();
        super.onStop();
    }

    private void updateRideInfoInUi() {
        if (detailedRide == null) {
            return;
        }

        BriefRideInfo bri = detailedRide.getBriefRideInfo();
        rideIdView.setText(getString(R.string.mrc_ride_id, detailedRide.getBriefRideInfo().getId()));
        rideStatusView.setText(getString(R.string.mrc_ride_status, bri.getStatus().toString()));
        photosCountView.setText(getString(R.string.mrc_ride_photos_published_count,
                bri.getPublishedPhotosCount(), bri.getPhotosCount()));
        rideDurationView.setText(getString(R.string.mrc_ride_duration, bri.getDuration().getText()));
        trackDistanceView.setText(getString(R.string.mrc_ride_track_distance, bri.getTrackDistance().getText()));
        hypothesesCountView.setText(getString(R.string.mrc_ride_hypotheses_count, bri.getHypotheses().size()));
    }

    OpenRideSession.OpenRideListener openRideListener = new OpenRideSession.OpenRideListener() {
        @Override
        public void onRideOpened(@NonNull DetailedRide ride) {
            detailedRide = ride;
            updateRideInfoInUi();

            PolylineMapObject trackPolyline = mapObjectCollection.addPolyline(detailedRide.getTrack());
            trackPolyline.setStrokeColor(Color.argb(255, 0, 0, 255));
            focusOnTrack(detailedRide.getTrack());
            polylineIndex = PolylineUtils.createPolylineIndex(detailedRide.getTrack());

            previewImagesAdapter = new RidePreviewImagesAdapter(imageDownloader, detailedRide.getTrackPreview());
            previewImagesView.setAdapter(previewImagesAdapter);
            previewImagesAdapter.notifyDataSetChanged();
        }

        @Override
        public void onOpenRideError(@NonNull Error error) {
            Log.e(TAG, "Failed to open detailed ride: " + error);
            showUserMessage("Failed to open detailed ride");
            finish();
        }
    };

    private final InputListener mapInputListener = new InputListener() {
        final Context context = DetailedRideActivity.this;

        @Override
        public void onMapTap(@NonNull Map map, @NonNull Point point) {
            PolylinePosition polylinePosition
                    = polylineIndex.closestPolylinePosition(point, PolylineIndex.Priority.CLOSEST_TO_RAW_POINT, 100);
            if (polylinePosition == null) {
                return;
            }

            TrackPreviewItem item = findLowerBoundItem(detailedRide.getTrackPreview(), polylinePosition);

            // Remove old pin
            if (imagePin != null) {
                mapObjectCollection.remove(imagePin);
                imagePin = null;
            }
            // Cancel old session
            if (imageLoadingSession != null) {
                imageLoadingSession.cancel();
            }

            Image previewImage = item.getPreviewImage();
            if (previewImage == null) {
                showUserMessage("There is no photo at this position");
                return;
            }

            Image.ImageSize imageSize = getImageSize(previewImage, "thumbnail");
            if (imageSize == null) {
                Log.e(TAG, "Missing image sizes for image " + previewImage.getUrlTemplate());
                return;
            }

            imageLoadingSession = imageDownloader.loadImageBitmap(
                    previewImage.getUrlTemplate(),
                    imageSize,
                    new ImageSession.ImageListener() {
                        @Override
                        public void onImageLoaded(@NonNull Bitmap bitmap) {
                            Point point = PolylineUtils.pointByPolylinePosition(detailedRide.getTrack(), polylinePosition);
                            imagePin = mapObjectCollection.addPlacemark(point);
                            ImageView view = new ImageView(context);
                            view.setImageBitmap(scaleBitmap(context, bitmap, 60));
                            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            ViewProvider viewProvider = new ViewProvider(view);
                            viewProvider.snapshot();
                            imagePin.setView(viewProvider);
                        }

                        @Override
                        public void onImageLoadingError(@NonNull Error error) {
                            Log.e(TAG, "Failed to load image: " + previewImage.getUrlTemplate());
                        }
                    });
        }

        @Override
        public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
        }
    };

    private static TrackPreviewItem findLowerBoundItem(List<TrackPreviewItem> items, PolylinePosition position) {
        int low = 0, high = items.size();
        while (high - low > 1) {
            int mid = low + (high - low) / 2;
            int res = compare(items.get(mid).getTrackPosition(), position);
            if (res < 0) {
                low = mid;
            } else if (res > 0) {
                high = mid;
            } else {
                return items.get(mid);
            }
        }
        return items.get(low);
    }

    private static int compare(PolylinePosition lhs, PolylinePosition rhs) {
        if (lhs.getSegmentIndex() < rhs.getSegmentIndex())
            return -1;
        if (rhs.getSegmentIndex() < lhs.getSegmentIndex())
            return 1;
        return Double.compare(lhs.getSegmentPosition(), rhs.getSegmentPosition());
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

    public void viewAllPhotos(View view) {
        Intent intent = new Intent(DetailedRideActivity.this, DetailedRidePhotosActivity.class);
        if (localRideIdentifier != null) {
            intent.putExtra(INTENT_EXTRA_LOCAL_RIDE_ID, rideManager.serializeLocalRideId(localRideIdentifier));
        } else if (serverRideIdentifier != null) {
            intent.putExtra(INTENT_EXTRA_SERVER_RIDE_ID, rideManager.serializeServerRideId(serverRideIdentifier));
        }
        startActivity(intent);
    }
}
