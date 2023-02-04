package com.yandex.maps.testapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class NotificationChannels {
    public static String LOG_NOTIFICATIONS_CHANNEL_ID = "testapp_log_channel";
    public static String BG_GUIDANCE_CHANNEL_ID = "background_guidance_channel";
    public static String BG_NAVIGATION_CHANNEL_ID = "background_navigation_channel";

    public static void initChannels(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                    new NotificationChannel(
                            LOG_NOTIFICATIONS_CHANNEL_ID,
                            LOG_NOTIFICATIONS_CHANNEL_ID,
                            NotificationManager.IMPORTANCE_LOW));
            manager.createNotificationChannel(
                    new NotificationChannel(
                            BG_GUIDANCE_CHANNEL_ID,
                            BG_GUIDANCE_CHANNEL_ID,
                            NotificationManager.IMPORTANCE_LOW));
            manager.createNotificationChannel(
                    new NotificationChannel(
                            BG_NAVIGATION_CHANNEL_ID,
                            BG_NAVIGATION_CHANNEL_ID,
                            NotificationManager.IMPORTANCE_LOW));
        }
    }
}
