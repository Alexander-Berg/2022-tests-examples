package com.yandex.maps.testapp.directions_navigation;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.yandex.maps.testapp.NotificationChannels;

public class BackgroundService extends Service {
    private static String TAG = "BackgroundNavigationService";
    private static int NOTIFICATION_ID = 3;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, NotificationChannels.BG_NAVIGATION_CHANNEL_ID);
        builder.setContentTitle("Background navigation running");
        builder.setSmallIcon(com.yandex.passport.R.drawable.notification_bg);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setWhen(0);
        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        stopSelf();
    }
}
