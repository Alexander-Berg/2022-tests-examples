package com.yandex.maps.testapp;

import androidx.multidex.MultiDexApplication;

import android.app.NotificationManager;
import android.content.Context;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yandex.maps.testapp.offlinecaches.DownloadNotificationsListenerImpl;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.storage.StorageErrorListener;
import com.yandex.mapkit.offline_cache.OfflineCacheManager;
import com.yandex.mapkit.offline_cache.CachePathUnavailable;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.mrc.MrcAdapter;
import com.yandex.runtime.LocalError;
import com.yandex.runtime.network.RemoteError;
import com.yandex.maps.testapp.experiments.ExperimentsUtils;
import com.yandex.maps.testapp.logs.LogController;
import com.yandex.passport.api.Passport;
import com.yandex.passport.api.PassportApi;
import com.yandex.passport.api.PassportCredentials;
import com.yandex.passport.api.PassportProperties;

import com.yandex.runtime.Runtime;
import com.yandex.runtime.Error;
import com.yandex.runtime.DiskCorruptError;
import com.yandex.runtime.DiskFullError;
import com.yandex.runtime.DiskWriteAccessError;
import com.yandex.runtime.FailedAssertionListener;

import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.logging.Logger;

public class MainApplication extends MultiDexApplication {
    public static String HOST_PREFERNCES_KEY = "com.yandex.maps.testapp.HOST_PREFERENCES_KEY";
    public static String CUSTOM_HOST_KEY = "CUSTOM_HOST_KEY";
    private static Logger LOGGER = Logger.getLogger("yandex.maps");
    /**
     * Encrypted application id and secret.
     * This values are testing ids.
     * Read this instruction for obtaining productions ids:
     * https://wiki.yandex-team.ru/yandexmobile/AccountManager#peredintegraciejj
     */
    private static final String CLIENT_ID       = "2RqxSoWQtJrQDcGwhy2O+PTlSeFfY/39uez8HZ4kpKnrWEugv0L943dMjYnZFlIB";
    private static final String CLIENT_SECRET   = "0EuyEIKU5JqAX8K4hy3ZrVTyR/VARkh+VX8/nC6Kwba3jCK6Waz0Y4A/Eeh5o+Zo";
    private final static PassportCredentials PRODUCTION_CREDENTIALS = PassportCredentials.Factory.from(CLIENT_ID, CLIENT_SECRET);
    private boolean initialized = false;
    DownloadNotificationsListenerImpl downloadNotificationsListener;
    private static MainApplication instance;

    private ErrorListenerImpl errorListener;

    public DeviceInfo getDeviceInfo() throws ExecutionException, InterruptedException {
        return MapkitAdapter.getDeviceInfo();
    }

    private class ErrorListenerImpl implements StorageErrorListener, OfflineCacheManager.ErrorListener {
        public ErrorListenerImpl(Context ctx) {
            context = ctx;
        }

        @Override
        public void onError(Error error) {
            showError(error, -1);
        }

         @Override
        public void onRegionError(Error error, int regionId) {
            showError(error, regionId);
        }

        @Override
        public void onStorageError(LocalError error) {
            showError(error, -1);
        }

        private void showError(Error error, int regionId) {
            int messageId;
            if (error instanceof RemoteError) {
                messageId = R.string.remote_error;
            } else if (error instanceof DiskWriteAccessError) {
                messageId = R.string.disk_write_access_error;
            } else if (error instanceof DiskCorruptError) {
                messageId = R.string.corrupted_storage;
            } else if (error instanceof DiskFullError) {
                messageId = R.string.not_enough_space;
            } else if (error instanceof CachePathUnavailable) {
                messageId = R.string.cache_path_unavailable;
            } else {
                return;
            }

            String message = context.getString(messageId);
            if (regionId >= 0) {
                message += " " + context.getString(R.string.in_region) + " " + String.valueOf(regionId);
            }

            Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
        }

        private Context context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannels.initChannels(notificationManager);

        LogController.getInstance().setNotificationManager(notificationManager);
        LogController.getInstance().setContext(getApplicationContext());

        if (Passport.isInPassportProcess()) {
            final PassportProperties passportProperties = PassportProperties.Builder.Factory.createBuilder()
                    .addCredentials(Passport.PASSPORT_ENVIRONMENT_PRODUCTION, PRODUCTION_CREDENTIALS)
                    .setBackendHost(getResources().getString(R.string.preferences_backend_host_key))
                    .build();
            Passport.initializePassport(this, passportProperties);
        } else if (AuthUtil.passportApi_ == null) {
            AuthUtil.passportApi_ = PassportApi.Factory.from(this);
        }

        if (!Runtime.isMainProcess(this)) {
            return;
        }

        MapKitFactory.initializeBackgroundDownload(this, this::initializeMapkit);
        instance = this;
    }

    private void initializeMapkit() {
        if (initialized)
            return;
        initialized = true;

        MapkitAdapter.initialize(this);
        MrcAdapter.initialize(this);

        Runtime.setFailedAssertionListener(new FailedAssertionListener() {
            @Override
            public void onFailedAssertion(String file, int line, String condition, String message, List<String> stack) {
                String reportMessage = String.format("Assertion failed in %s:%d (%s, %s). Backtrace:", file, line, condition, message);
                for (String elem: stack) {
                    reportMessage = reportMessage + "\n\t" + elem;
                }

                FirebaseCrashlytics.getInstance().log(reportMessage);
                LOGGER.warning(reportMessage);
            }
        });

        ExperimentsUtils.refreshCustomExperiments(ExperimentsUtils.loadExperimentsList(this));

        errorListener = new ErrorListenerImpl(
            getApplicationContext());

        downloadNotificationsListener = new DownloadNotificationsListenerImpl();
        downloadNotificationsListener.startNotifications(MapKitFactory.getInstance().getOfflineCacheManager());

        MapKitFactory.getInstance().getOfflineCacheManager().addErrorListener(errorListener);
        MapKitFactory.getInstance().getStorageManager().addStorageErrorListener(errorListener);
    }

    public static void initialize() {
        instance.initializeMapkit();
    }
}
