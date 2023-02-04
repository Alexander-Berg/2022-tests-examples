package com.yandex.maps.testapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yandex.android.startup.identifier.StartupClientIdentifierData;
import com.yandex.android.startup.identifier.StartupClientIdentifierProvider;
import com.yandex.android.startup.identifier.metricawrapper.MetricaStartupClientIdentifierProvider;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.maps.testapp.data_providers.DataProvidersConsts;
import com.yandex.maps.testapp.logs.LogController;
import com.yandex.maps.testapp.logs.YandexMetricaMessage;
import com.yandex.maps.testapp.mrc.MrcAdapter;
import com.yandex.maps.auth.AccountFactory;
import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.testapp.settings.SharedPreferencesConsts;
import com.yandex.maps.testapp.apiKeySettings.ApiKeyConsts;
import com.yandex.mapkit.StyleType;
import com.yandex.metrica.YandexMetricaConfig;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.SwallowedExceptionListener;
import com.yandex.runtime.recording.EventLoggingFactory;
import com.yandex.runtime.recording.EventListener;
import com.yandex.metrica.YandexMetrica;
import com.yandex.runtime.Runtime;
import com.yandex.runtime.recording.LoggingLevel;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.Map;

public class MapkitAdapter {
    private static final String METRICA_API_KEY = "59aec043-98a0-4a25-863e-551cdd955288";
    private static final String STYLE_PREFERENCES_FILE = "style_prefs";
    private static final String STYLE_TYPE_KEY = "style_type";
    private static final long RETRY_INTERVAL = 5000;
    private static volatile MapkitAdapter mapkitAdapter;

    private Future<Object> future;
    private Map<String, Long> initializationTimes = new HashMap<>();
    private EventListenerImpl eventListener;
    private SwallowedExceptionListenerImpl swallowedExceptionListener;

    private class EventListenerImpl implements EventListener {
        private LoggingLevel level;

        EventListenerImpl(LoggingLevel level) {
            this.level = level;
        }

        @Override
        public void onEvent(String event, Map<String, String> data)
        {
            StringBuilder builder = new StringBuilder();
            builder.append(event);

            for (Map.Entry<String, String> entry : data.entrySet()) {
                builder.append(". ").append(entry.getKey()).append(": ").append(entry.getValue());
            }
            LogController.getInstance().addEvent(new YandexMetricaMessage(level, builder.toString()));
            YandexMetrica.reportEvent(event, (Map)data);
        }
    }

    private String getSharedPreferenceString(String preferenceGroupName, String key, final Context context) {
        SharedPreferences sPref =
            context.getSharedPreferences(preferenceGroupName, Context.MODE_PRIVATE);
        return sPref.getString(key, "");
    }

    private void readDataProviderFromPreferences(String prefKey,
                                                 String optionKey,
                                                 Map<String, String> options,
                                                 Context context) {
        String value = getSharedPreferenceString(SharedPreferencesConsts.I18N_PREFS, prefKey, context);
        if (value.equals("Osm"))
            options.put(optionKey, "osm");
    }

    private class SwallowedExceptionListenerImpl implements SwallowedExceptionListener {
        @Override
        public void onSwallowedException(@NonNull String message) {
            Exception report = new Exception(message);
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

            StackTraceElement stacktrace[] = new StackTraceElement[1];
            String messageLines[] = message.split("\n");


            // Method name, file name and line number doesn't used
            // in exception record
            stacktrace[0] = new StackTraceElement(
                messageLines[0].equals("") ? "Empty stacktrace" : messageLines[0],
                "",
                "",
                0);

            report.setStackTrace(stacktrace);

            FirebaseCrashlytics.getInstance().log(message);
            FirebaseCrashlytics.getInstance().recordException(report);
        }
    }

    private MapkitAdapter(final Context context) {

        String savedApiKeyType =
            getSharedPreferenceString(ApiKeyConsts.API_KEY_PREFS, ApiKeyConsts.API_KEY_TYPE_KEY, context);
        String currentApiKey;
        if (savedApiKeyType.isEmpty() || ApiKeyConsts.API_KEYS_MAP.get(savedApiKeyType) == null) {
            currentApiKey = ApiKeyConsts.API_KEYS_MAP.get(ApiKeyConsts.YANDEX_STRING);
        } else {
            currentApiKey = ApiKeyConsts.API_KEYS_MAP.get(savedApiKeyType);
        }
        MapKitFactory.setApiKey(currentApiKey);
        String savedLocale =
            getSharedPreferenceString(SharedPreferencesConsts.I18N_PREFS, SharedPreferencesConsts.LOCALE_KEY, context);
        if (!savedLocale.isEmpty())
            MapKitFactory.setLocale(savedLocale);


        Map<String, String> runtimeOptions = new HashMap();

        readDataProviderFromPreferences(
                DataProvidersConsts.DRIVING_DATAPROVIDER_KEY,
                context.getString(R.string.driving_dataprovider_option),
                runtimeOptions,
                context);
        readDataProviderFromPreferences(
                DataProvidersConsts.MAP_DATAPROVIDER_KEY,
                context.getString(R.string.map_dataprovider_option),
                runtimeOptions,
                context);
        readDataProviderFromPreferences(
                DataProvidersConsts.SEARCH_DATAPROVIDER_KEY,
                context.getString(R.string.search_dataprovider_option),
                runtimeOptions,
                context);

        readDataProviderFromPreferences(
                DataProvidersConsts.SUGGEST_DATAPROVIDER_KEY,
                context.getString(R.string.suggest_dataprovider_option),
                runtimeOptions,
                context);

        String env = Environment.readEnvironmentFromPreferences(context);
        runtimeOptions.put(Environment.environmentKey(context), env);

        initializationTimes.put("runtime-initialization",
                PerfUtils.measureTime(() -> Runtime.init(context, runtimeOptions)));

        LogController.getInstance().showNotification();

        initializationTimes.put("mapkit-instance-initialization",
                    PerfUtils.measureTime(() -> MapKitFactory.initialize(context)));

        StyleType styleType = getMapStyleType(context);
        MapKitFactory.getInstance().setStyleType(styleType);
        if (styleType == StyleType.V_NAV2) {
            MapKitFactory.getInstance().setScaleFactor(calcNaviScaleFactor(context));
        }
        if (styleType == StyleType.V_MAP2 || styleType == StyleType.V_NAV2) {
            MapKitFactory.getInstance().setAdditionalNecessaryOfflineCacheLayers(List.of("vmap3fb", "fonts"));
        }

        RecordingFactory.setApiKey(currentApiKey);
        RecordingFactory.initialize(context);
        RecordingFactory.getInstance().recordCollector().startReport();

        MrcAdapter.initialize(context);
        MRCFactory.setApiKey(currentApiKey);

        if (!BuildConfig.DEBUG) {
            YandexMetricaConfig config = YandexMetricaConfig.newConfigBuilder(METRICA_API_KEY).build();
            YandexMetrica.activate(context, config);
            eventListener = new EventListenerImpl(LoggingLevel.NORMAL);
            EventLoggingFactory.getEventLogging().subscribe(eventListener, LoggingLevel.NORMAL);

            final StartupClientIdentifierProvider idProvider = new MetricaStartupClientIdentifierProvider(context);

            future = Executors.newSingleThreadExecutor().submit(() -> {
                StartupClientIdentifierData data;
                for (;;) {
                    data = idProvider.requestBlocking(context);
                    if (!data.hasError())
                        break;

                    Log.e(MainApplication.class.getCanonicalName(),
                        "Couldn't get identifiers: " + data.getErrorDescription());
                    Thread.sleep(RETRY_INTERVAL);
                }

                String deviceId = data.getDeviceId();
                String uuid = data.getUuid();

                MapKitFactory.getInstance().setMetricaIds(uuid, deviceId);
                RecordingFactory.getInstance().initialize(uuid, deviceId);
                MRCFactory.getInstance().setClientIdentifiers(uuid, deviceId);
                return new DeviceInfo(deviceId, uuid);
            });
            AccountFactory.initialize(context);
        }

        swallowedExceptionListener = new SwallowedExceptionListenerImpl();
        Runtime.setSwallowedExceptionListener(swallowedExceptionListener);
    }
    public static void initialize(Context context) {
        mapkitAdapter = new MapkitAdapter(context);
    }

    //Copied from yandexnavi.core/src/navi/NavigatorApp.cpp: calcScaleFactor().
    private static int calcNaviScaleFactor(final Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);

        int minSize = Math.min(displayMetrics.heightPixels, displayMetrics.widthPixels);
        if (minSize >= 1000 && displayMetrics.densityDpi >= 240) {
            return 4;
        } else if (minSize >= 400 || displayMetrics.densityDpi >= 180) {
            return 2;
        }

        return 1;
    }

    public static StyleType getMapStyleType(final Context context) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(STYLE_PREFERENCES_FILE, Context.MODE_PRIVATE);
        int index = sharedPref.getInt(STYLE_TYPE_KEY, MapKitFactory.getInstance().getStyleType().ordinal());
        return StyleType.values()[index];
    }

    public static void setMapStyleType(Context context, StyleType styleType) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(MapkitAdapter.STYLE_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(MapkitAdapter.STYLE_TYPE_KEY, styleType.ordinal());
        editor.commit();
    }

    public static DeviceInfo getDeviceInfo() throws ExecutionException, InterruptedException {
        if (mapkitAdapter.future != null) {
            return (DeviceInfo) mapkitAdapter.future.get();
        }
        return null;
    }

    public static void onPause(Activity activity) {
        if (!BuildConfig.DEBUG) {
            YandexMetrica.pauseSession(activity);
        }
    }
    public static void onResume(Activity activity) {
        if (!BuildConfig.DEBUG) {
            YandexMetrica.resumeSession(activity);
        }
    }

    public static Future<Map<String, Long>> getInitializationTimes() {
        return Executors.newSingleThreadExecutor().submit(() -> {
            int safeCounter = 0;
            while (mapkitAdapter == null && safeCounter++ < 10)
                Thread.sleep(RETRY_INTERVAL);
            return mapkitAdapter != null ? mapkitAdapter.initializationTimes : new HashMap<>();
        });
    }

    public static void onStop(Activity activity) {
        MapKitFactory.getInstance().onStop();
    }
    public static void onStart(Activity activity) {
        MapKitFactory.getInstance().onStart();
    }
}
