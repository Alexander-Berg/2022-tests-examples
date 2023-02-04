package com.yandex.maps.testapp.offlinecaches;

import android.app.NotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import android.os.Build;

import java.util.HashMap;
import java.util.List;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.offline_cache.RegionState;
import com.yandex.runtime.Runtime;
import com.yandex.mapkit.offline_cache.RegionListener;
import com.yandex.mapkit.offline_cache.RegionListUpdatesListener;
import com.yandex.mapkit.offline_cache.OfflineCacheManager;
import com.yandex.mapkit.offline_cache.Region;
import com.yandex.maps.testapp.R;

public class DownloadNotificationsListenerImpl {
    private final int NOTIFY_ID = 2;
    private final String NOTIFY_TAG = "com.yandex.maps.testapp.offlinecaches";

    public void startNotifications(OfflineCacheManager offlineCacheManager) {
        context = Runtime.getApplicationContext();
        notificationChannelId = "mapkit_download_notifications";
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, notificationChannelId, importance);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        this.offlineCacheManager = offlineCacheManager;
        offlineCacheManager.addRegionListener(regionListener);
    }

    private RemoteViews actualContentView() {
        RemoteViews contentView = new RemoteViews(Runtime.getApplicationContext().getPackageName(),
                R.layout.download_notifications);
        if(currentRegion != null) {
            contentView.setViewVisibility(R.id.current_region_info, View.VISIBLE);
            contentView.setViewVisibility(R.id.current_region_progress, View.VISIBLE);

            contentView.setTextViewText(R.id.current_region_info, "Downloading region: "
                    + currentRegion.getName());
            contentView.setTextColor(R.id.current_region_info, Color.BLACK);

            contentView.setProgressBar(R.id.current_region_progress,
                    (int) currentRegion.getSize().getValue(),
                    (int) (currentRegionProgress * currentRegion.getSize().getValue()),
                    false);
        } else {
            contentView.setViewVisibility(R.id.current_region_info, View.INVISIBLE);
            contentView.setViewVisibility(R.id.current_region_progress, View.INVISIBLE);
        }
        contentView.setTextViewText(R.id.total_download_status,
                "Total downloads left: " + String.valueOf(activeDownloads));
        return contentView;
    }

    private void sendNotification() {
        if(activeDownloads == 0) {
            notificationManager.cancel(NOTIFY_TAG, NOTIFY_ID);
            return;
        }

        long when = System.currentTimeMillis();

        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setContentText("Downloading progress")
                .setSmallIcon(R.drawable.active_downloads_logo)
                .setWhen(when);

        if (Build.VERSION.SDK_INT >= 26) {
            notificationBuilder.setChannelId(notificationChannelId);
        }

        Notification notification;
        notification = notificationBuilder.build();

        notification.contentView = actualContentView();

        //do not hide notification after press "Clear events"
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        notificationManager.notify(NOTIFY_TAG, NOTIFY_ID, notification);
    }

    private NotificationManager notificationManager;

    private HashMap<Integer, Region> regions;
    private HashMap<Integer, RegionState> regionStates;

    private Context context;
    private String notificationChannelId;

    private int activeDownloads = 0;
    private float currentRegionProgress = 0;
    private Region currentRegion;
    private boolean initialized = false;

    private OfflineCacheManager offlineCacheManager;

    private RegionListUpdatesListener listUpdatesListener = new RegionListUpdatesListener() {
        @Override
        public void onListUpdated() {
            regions = new HashMap<Integer, Region>();
            regionStates = new HashMap<Integer, RegionState>();
            List<Region> newRegions = offlineCacheManager.regions();
            activeDownloads = 0;
            for (Region region : newRegions) {
                regions.put(region.getId(), region);
                RegionState regionState = offlineCacheManager.getState(region.getId());
                regionStates.put(region.getId(), regionState);
                if(regionState == RegionState.DOWNLOADING)
                    activeDownloads++;
            }
            sendNotification();
        }
    };

    private void initialize() {
        if (initialized)
            return;

        initialized = true;
        offlineCacheManager.addRegionListUpdatesListener(listUpdatesListener);
        listUpdatesListener.onListUpdated();
        sendNotification();
    }

    private RegionListener regionListener = new RegionListener() {
        @Override
        public void onRegionStateChanged(int regionId) {
            if (!initialized)
                return;
            RegionState newRegionState = offlineCacheManager.getState(regionId);
            RegionState oldRegionState = regionStates.get(regionId);
            if(oldRegionState != RegionState.DOWNLOADING
                    && newRegionState == RegionState.DOWNLOADING) {
                activeDownloads++;
                sendNotification();
            } else if(oldRegionState == RegionState.DOWNLOADING
                    && newRegionState != RegionState.DOWNLOADING) {
                activeDownloads--;
                sendNotification();
            }
            regionStates.put(regionId, newRegionState);
        }

        @Override
        public void onRegionProgress(int regionId) {
            initialize();
            float regionProgress = offlineCacheManager.getProgress(regionId);
            if(regionProgress == 1) {
                currentRegion = null;
                currentRegionProgress = 1;
            } else {
                currentRegion = regions.get(regionId);
                currentRegionProgress = regionProgress;
            }
            sendNotification();
        }
    };
}
