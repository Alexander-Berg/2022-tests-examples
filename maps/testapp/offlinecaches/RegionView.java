package com.yandex.maps.testapp.offlinecaches;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.offline_cache.OfflineCacheManager;
import com.yandex.mapkit.offline_cache.Region;
import com.yandex.mapkit.offline_cache.RegionListener;
import com.yandex.mapkit.offline_cache.RegionState;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapActivity;
import com.yandex.runtime.Error;
import com.yandex.runtime.bindings.Serialization;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RegionView extends FrameLayout
    implements RegionListener {

    public RegionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View.inflate(context, R.layout.region_view, this);

        regionInfo_ = (TextView) findViewById(R.id.custom_region_info);
        regionDownloadInfo_ = (TextView) findViewById(R.id.custom_region_download_info);
        downloadedRegionReleaseTime_ = (TextView) findViewById(R.id.downloaded_region_release_time);
        regionCities_ = (TextView) findViewById(R.id.custom_region_cities);

        regionStart_ = (Button) findViewById(R.id.custom_region_start);
        regionStop_ = (Button) findViewById(R.id.custom_region_stop);
        regionPause_ = (Button) findViewById(R.id.custom_region_pause);
        regionDrop_ = (Button) findViewById(R.id.custom_region_drop);
        regionSetUnsupported_ = (Button) findViewById(R.id.custom_region_set_unsupported);

        regionStart_.setOnClickListener(new OnRegionStartButtonClickListener());
        regionStop_.setOnClickListener(new OnRegionStopButtonClickListener());
        regionPause_.setOnClickListener(new OnRegionPauseButtonClickListener());
        regionDrop_.setOnClickListener(new OnRegionDropButtonClickListener());
        regionSetUnsupported_.setOnClickListener(new OnRegionUnsupportButtonClickListener());

        regionProgress_ = (ProgressBar) findViewById(R.id.custom_region_progress);

        offlineCacheManager_ = MapKitFactory.getInstance().getOfflineCacheManager();
    }

    public RegionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RegionView(Context context) {
        this(context, null, 0);
    }

    public boolean isRegionValid() {
        return region_ != null; 
    }

    @Override
    public void onRegionProgress(int regionId) {
        if(regionId == region_.getId()) {
            regionProgress_.setProgress((int)(offlineCacheManager_.getProgress(regionId) * 100));
        }
    }

    @Override
    public void onRegionStateChanged(int regionId) {
        if(regionId == region_.getId()) {
            regionDownloadInfo_.setText(getDownloadInfo());
            
            Long downloadedRegionReleaseTime = offlineCacheManager_.getDownloadedReleaseTime(region_.getId());
            if (downloadedRegionReleaseTime != null)
                downloadedRegionReleaseTime_.setText(convertTime(downloadedRegionReleaseTime));
            else
                downloadedRegionReleaseTime_.setText("");

            if (isRegionInProgress(regionId)) {
                regionProgress_.setProgress((int)(offlineCacheManager_.getProgress(regionId) * 100));
                regionProgress_.setVisibility(VISIBLE);
            } else {
                regionProgress_.setVisibility(INVISIBLE);
            }
        }
    }

    public void setRegion(Region region) {
        offlineCacheManager_.removeRegionListener(this);

        region_ = region;
        offlineCacheManager_.addRegionListener(this);

        regionInfo_.setText(getRegionInfo());
        regionCities_.setText(TextUtils.join(", ", offlineCacheManager_.getCities(region.getId())));

        onRegionStateChanged(region.getId());
        onRegionProgress(region.getId());
    }

    public void onRegionOpenClick() {
        Point cameraTarget = region_.getCenter();
        Intent intent = new Intent(getContext(), MapActivity.class);
        intent.putExtra(
            MapActivity.CAMERA_TARGET_EXTRA,
            Serialization.serializeToBytes(cameraTarget));
        getContext().startActivity(intent);
    }

    private String getRegionInfo() {
        return region_.getName() + ", " +
                region_.getCountry() + ", " +
                    "(" + region_.getCenter().getLatitude() + ", " +
                    region_.getCenter().getLongitude() + "), " +
                region_.getId() +
                (offlineCacheManager_.getState(region_.getId()) == RegionState.OUTDATED ? ", outdated" : "");
    }

    private String getDownloadInfo() {
        String result = region_.getSize().getText() + ", " +
                convertTime(region_.getReleaseTime()) + ", " +
                offlineCacheManager_.getState(region_.getId()).name();
        if (offlineCacheManager_.isLegacyPath(region_.getId()))
            result += ", legacy path";
        return result;
    }

    private String convertTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("dd.MM.yyyy");
        return format.format(date);
    }

    private boolean isRegionInProgress(int regionId) {
        RegionState state = offlineCacheManager_.getState(regionId);
        return state == RegionState.DOWNLOADING || state == RegionState.PAUSED;
    }

    private class OnRegionStartButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (offlineCacheManager_.mayBeOutOfAvailableSpace(region_.getId())) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Not enough disk space")
                        .setMessage("If you proceed download might finish with error")
                        .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                offlineCacheManager_.startDownload(region_.getId());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            } else {
                offlineCacheManager_.startDownload(region_.getId());
            }
        }
    }

    private class OnRegionStopButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            offlineCacheManager_.stopDownload(region_.getId());
        }
    }

    private class OnRegionPauseButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            offlineCacheManager_.pauseDownload(region_.getId());
        }
    }

    private class OnRegionDropButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            offlineCacheManager_.drop(region_.getId());
        }
    }

    private class OnRegionUnsupportButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            offlineCacheManager_.simulateUnsupported(region_.getId());
        }
    }

    private Region region_;
    private OfflineCacheManager offlineCacheManager_;
    private final TextView regionInfo_;
    private final TextView downloadedRegionReleaseTime_;
    private final TextView regionDownloadInfo_;
    private final TextView regionCities_;
    private final Button regionStart_;
    private final Button regionStop_;
    private final Button regionPause_;
    private final Button regionDrop_;
    private final Button regionSetUnsupported_;
    private final ProgressBar regionProgress_;
}
