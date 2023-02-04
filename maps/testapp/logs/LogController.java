package com.yandex.maps.testapp.logs;

import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.widget.RemoteViews;
import android.os.Build;

import java.util.ArrayDeque;
import java.util.Deque;

import com.yandex.maps.testapp.NotificationChannels;
import com.yandex.runtime.logging.LogListener;
import com.yandex.runtime.logging.LogMessage;
import com.yandex.runtime.logging.LoggingFactory;
import com.yandex.runtime.Runtime;
import com.yandex.maps.testapp.R;

public class LogController {
    private static LogController instance = new LogController();
    private LogController() {}
    public static LogController getInstance() {
        return instance;
    }

    private int NOTIFY_ID = 1;
    private int HISTORY_SIZE = 200;
    private NotificationManager notificationManager;

    private Context context;
    private String notificationChannelId = NotificationChannels.LOG_NOTIFICATIONS_CHANNEL_ID;

    private int errorCount = 0;
    private int warningCount = 0;
    private String lastError = new String("No errors");
    private Deque<LogEvent> history = new ArrayDeque();

    private LogListener logListener = new LogListener() {
        @Override
        public void onMessageRecieved(LogMessage message) {
            history.add(new LogEvent(message));
            if (history.size() > HISTORY_SIZE) {
                history.removeFirst();
            }

            if (message.getLevel() == LogMessage.LogLevel.ERROR) {
                ++errorCount;
                lastError = message.getMessage();
            }
            if (message.getLevel() == LogMessage.LogLevel.WARNING) {
                ++warningCount;
            }
            updateNotification();
        }
    };

    public void addEvent(YandexMetricaMessage message) {
        history.add(new LogEvent(message));
        if (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }
        updateNotification();
    }

    interface ClickHandler {
        void onClick(LogEvent msg);
    };

    public SpannableStringBuilder getHistory(final ClickHandler clickHandler, String filter) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for(final LogEvent event : history) {
            if (!filter.isEmpty() && !event.getMessage().contains(filter))
                continue;

            int messageStart = builder.length();
            int start = builder.length();
            builder.append("[");
            builder.append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ").format(
                new java.util.Date(event.getTime())));

            builder.append(event.getType());
            builder.append("] ");
            builder.setSpan(new ForegroundColorSpan(0xFF55FF55), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            start = builder.length();
            builder.append(event.getMessage());

            ForegroundColorSpan color = new ForegroundColorSpan(event.getColor());
            builder.setSpan(color, start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            ClickableSpan showInfoSpan = new ClickableSpan() {
                @Override
                public void onClick(android.view.View widget) {
                    clickHandler.onClick(event);
                }
                @Override
                public void updateDrawState (android.text.TextPaint ds) {
                    // Don't alter wraw style
                }
            };
            builder.setSpan(showInfoSpan, messageStart, builder.length(), 0);

            builder.append("\n");
        }
        return builder;
    }

    public void setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void showNotification() {
        LoggingFactory.getLogging().subscribe(logListener);
        updateNotification();
    }

    public void hideNotification() {
        LoggingFactory.getLogging().unsubscribe(logListener);
        notificationManager.cancel(NOTIFY_ID);
    }

    public void resetCounters() {
        errorCount = 0;
        warningCount = 0;
        updateNotification();
    }

    public CharSequence infoText() {
        return "Errors: " + errorCount + ". Warnings: " + warningCount + ".";
    }

    private int shownErrorCount = -1;
    private int shownWarningCount = -1;
    private RateLimiter rateLimiter = new RateLimiter(1000);

    private void updateNotification() {
        if (errorCount != shownErrorCount || warningCount != shownWarningCount) {
            rateLimiter.run(new Runnable() {
                    @Override
                    public void run() {
                        sendNotification();
                    }
                });
        }
    }

    private void sendNotification() {
        CharSequence logInfoText = infoText();

        long when = System.currentTimeMillis();

        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setContentText(logInfoText)
                .setSmallIcon(R.drawable.log_recieved)
                .setWhen(when);

        if (Build.VERSION.SDK_INT >= 26) {
            notificationBuilder.setChannelId(notificationChannelId);
        }

        Notification notification = notificationBuilder.build();

        Intent notificationIntent = new Intent(Runtime.getApplicationContext(), LogViewActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
            Runtime.getApplicationContext(),
            0,
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notification.contentIntent = contentIntent;

        RemoteViews contentView = new RemoteViews(Runtime.getApplicationContext().getPackageName(), R.layout.log_notification);
        contentView.setTextViewText(R.id.log_info, logInfoText);
        contentView.setTextViewText(R.id.log_text, lastError);
        contentView.setTextColor(R.id.log_info, Color.RED);
        notification.contentView = contentView;

        if (errorCount + warningCount > 0) {
            notification.ledARGB = 0xffff0000;
            notification.ledOnMS = 300;
            notification.ledOffMS = 500;
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        }

        //do not hide notification after press "Clear events"
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        notificationManager.notify(NOTIFY_ID, notification);
    }
}
