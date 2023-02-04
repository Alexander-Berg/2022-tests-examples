package com.yandex.maps.testapp.mrcphoto;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.mrc.MrcPhotoService;
import com.yandex.runtime.Error;

import static com.yandex.maps.testapp.mrcphoto.MrcPhotoActivity.PARAM_PHOTO_ID;
import static com.yandex.maps.testapp.mrcphoto.MrcPhotoActivity.PARAM_VISIBLE_LAYER;

public class MrcPhotoMapActivity extends MrcPhotoLayerMapActivity
                                 implements MrcPhotoService.SearchListener {
    private MrcPhotoService mrcPhotoService;
    private MrcPhotoService.SearchSession searchSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mrcPhotoService = PlacesFactory.getInstance().createMrcPhotoService();
    }

    @Override
    protected void onMrcLayerTap(@NonNull Map map, @NonNull Point point) {
        if (searchSession != null) {
            searchSession.cancel();
        }

        searchSession = mrcPhotoService.findNearestPhoto(
                getVisibleLayer(),
                point,
                (int) map.getCameraPosition().getZoom(),
                this);
    }

    @Override
    public void onPhotoSearchResult(@NonNull String photoId) {
        Intent intent = new Intent(this, MrcPhotoActivity.class);
        intent.putExtra(PARAM_PHOTO_ID, photoId);
        intent.putExtra(PARAM_VISIBLE_LAYER, getVisibleLayer());
        startActivity(intent);
    }

    @Override
    public void onPhotoSearchError(@NonNull Error error) {
        Toast.makeText(MrcPhotoMapActivity.this,
                "Failed to open mrc photo: " + error.toString(), Toast.LENGTH_LONG).show();
    }
}
