package com.yandex.maps.testapp.mrcphoto;

import static com.yandex.maps.testapp.mrcphoto.MrcPhotoTrackActivity.PARAM_POSITION;
import static com.yandex.maps.testapp.mrcphoto.MrcPhotoTrackActivity.PARAM_TRACK_POLYLINE;
import static com.yandex.maps.testapp.mrcphoto.MrcPhotoTrackActivity.PARAM_TRACK_TYPE;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.places.mrc.MrcPhotoLayer;
import com.yandex.mapkit.places.mrc.MrcPhotoTrack;
import com.yandex.maps.testapp.common.internal.Serialization;
import com.yandex.runtime.Error;

import java.util.List;

public class MrcPhotoTrackMapActivity extends MrcPhotoLayerMapActivity
                                      implements MrcPhotoTrackProvider.PhotoTrackListener,
                                                 RequestPointsProvider.PointsReceiver {
    private final int REQUEST_POINTS_LIMIT = 2;

    private RequestPointsProvider requestPointsProvider = null;
    private MrcPhotoTrackProvider mrcPhotoTrackProvider = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPointsProvider = new RequestPointsProvider(mapview, REQUEST_POINTS_LIMIT);
    }

    @Override
    protected void onMrcPhotoLayerChange() {
        if (mrcPhotoTrackProvider != null) {
            mrcPhotoTrackProvider.resetRoute();
            mrcPhotoTrackProvider = null;
        }

        MrcPhotoLayer.VisibleLayer visibleLayer = getVisibleLayer();
        if (visibleLayer == null) {
            requestPointsProvider.unsubscribe(MrcPhotoTrackMapActivity.this);
        } else {
            requestPointsProvider.subscribe(MrcPhotoTrackMapActivity.this);
        }
        requestPointsProvider.clear();

        if (visibleLayer != null) {
            switch (visibleLayer) {
                case AUTOMOTIVE:
                case AUTOMOTIVE_AGE:
                    mrcPhotoTrackProvider = new AutomotiveMrcPhotoTrackProvider(mapview, this);
                    break;
                case PEDESTRIAN:
                case PEDESTRIAN_AGE:
                    mrcPhotoTrackProvider = new PedestrianMrcPhotoTrackProvider(mapview, this);
                    break;
            }
        }
    }

    @Override
    public void receivePoints(List<Point> points) {
        assert (mrcPhotoTrackProvider != null);
        mrcPhotoTrackProvider.resetRoute();
        mrcPhotoTrackProvider.submitRoutingRequest(points);
    }

    @Override
    public void onMrcPhotoTrackError(@NonNull Error error) {
        requestPointsProvider.clear();
        if (mrcPhotoTrackProvider != null) {
            mrcPhotoTrackProvider.resetRoute();
        }
        Toast.makeText(this, "Failed to get MRC track preview", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMrcPhotoTrackTap(@NonNull MrcPhotoTrack mrcPhotoTrack,
                                   @Nullable PolylinePosition position) {
        if (position == null) {
            Toast.makeText(this, "No MRC photo at this point", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MrcPhotoTrackMapActivity.this, MrcPhotoTrackActivity.class);
        intent.putExtra(PARAM_POSITION, Serialization.serialize(position, PolylinePosition.class));
        intent.putExtra(PARAM_TRACK_POLYLINE, Serialization.serialize(mrcPhotoTrack.getTrackPolyline(), Polyline.class));
        intent.putExtra(PARAM_TRACK_TYPE, mrcPhotoTrack.getTrackType());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (mrcPhotoTrackProvider != null) {
            mrcPhotoTrackProvider.onDestroy();
            mrcPhotoTrackProvider = null;
        }
        super.onDestroy();
    }
}
