package com.yandex.launcher.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.yandex.alice.SpeechKitManager;
import com.yandex.launcher.common.app.AppConfig;
import com.yandex.launcher.common.app.IAppConfigBuilder;
import com.yandex.launcher.common.util.IntentUtils;
import com.yandex.launcher.common.util.TextUtils;
import com.yandex.images.DefaultImageCache;
import com.yandex.images.DefaultImagesParams;
import com.yandex.images.SharedBitmapLruCache;
import com.yandex.launcher.BuildConfig;
import com.yandex.launcher.R;
import com.yandex.launcher.alice.TestAliceAccountManagerLauncherActivity;
import com.yandex.launcher.alice.TestAliceDialogActivity;
import com.yandex.launcher.alice.TestAliceLauncherProxyActivity;
import com.yandex.launcher.api.LauncherConfig;
import com.yandex.launcher.api.LauncherHost;
import com.yandex.launcher.api.LauncherPreference;
import com.yandex.launcher.api.PrerenderFactory;
import com.yandex.launcher.api.SuggestToolkit;
import com.yandex.launcher.api.alice.AliceActivityStarter;
import com.yandex.launcher.api.alice.AliceImageCacheManager;
import com.yandex.launcher.api.alice.SpeechKitManagerAccess;
import com.yandex.launcher.api.alice.SpeechKitManagerFactory;
import com.yandex.launcher.api.alice.SpeechKitManagerProvider;
import com.yandex.launcher.api.alice.UriHandlerManager;
import com.yandex.launcher.api.alice.UriHandlerResult;
import com.yandex.launcher.api.auth.AccountManagerFacade;
import com.yandex.launcher.api.auth.NoOpAccountManagerFacade;
import com.yandex.launcher.api.minusone.MinusOneKit;
import com.yandex.launcher.api.minusone.MinusOneKitFactory;
import com.yandex.launcher.api.searchappshortcuts.SearchAppShortcutsDelegate;
import com.yandex.launcher.api.searchappshortcuts.SearchAppShortcutsDelegateFactory;
import com.yandex.launcher.preferences.Preference;
import com.yandex.launcher.preferences.PreferencesManager;

public class TestApplication extends Application {

    @SuppressWarnings("YandexLauncherKitApplicationId")
    final LauncherConfig config = new LauncherConfig() {

        private static final String APPLICATION_ID = "com.yandex.launcher";

        @NonNull
        @Override
        public String getApplicationId() {
            return APPLICATION_ID;
        }

        @NonNull
        @Override
        public String getAliceProcessId() {
            return APPLICATION_ID;
        }

        @Override
        public boolean needsAliceIconOnWorkspace() {
            return true;
        }

        @NonNull
        @Override
        public String getIntentChooserAppName() {
            return getString(R.string.general_app_name);
        }

        @Override
        public boolean hasMinusOneInHomescreenSettings() {
            return true;
        }

        @Override
        public boolean hasOwnMetrica() {
            return true;
        }

        @Override
        public boolean hasOwnMetricaPushLib() {
            return true;
        }

        @Override
        public boolean hasSearchAppShortcuts() {
            return false;
        }

        @Override
        public boolean needPartnerAppsClidsUpdate() {
            return true;
        }

        @Override
        public boolean hasChangeLauncherSettingsItem() {
            return PreferencesManager.getBoolean(Preference.ENABLE_SETTINGS_CHANGE_LAUNCHER);
        }
    };

    public final LauncherHost testLauncherHost = new LauncherHost() {
        @NonNull
        private final AccountManagerFacade accountManagerFacade = new NoOpAccountManagerFacade();

        @Override
        public void setPreference(@NonNull LauncherPreference preference, @NonNull String value) {

        }

        @NonNull
        @Override
        public String getPreference(@NonNull LauncherPreference preference) {
            return TextUtils.EMPTY;
        }

        @Override
        public void initializeLauncher() {
        }

        @Override
        public void initializeSpeechKit() {
        }

        @Override
        public void initializeAlice() {
        }

        @Override
        public void initializeInteractor() {
        }

        @Override
        public void initializeWebBrowser() {
        }

        @Override
        public void onBeforeLauncherActivityOnCreate() {
        }

        @Override
        public void notifyInitIfNeeded() {
        }

        @NonNull
        @Override
        public LauncherConfig getConfig() {
            return config;
        }

        @NonNull
        @Override
        public MinusOneKitFactory getMinusOneKitFactory() {
            return launcher -> MinusOneKit.NOOP;
        }

        @Nullable
        @Override
        public PrerenderFactory getPrerenderFactory() {
            return null;
        }

        @Nullable
        @Override
        public SuggestToolkit getSuggestToolkit() {
            return null;
        }

        @NonNull
        @Override
        public UriHandlerManager getUriHandlerManager() {
            return new UriHandlerManager() {
                @Override
                public boolean handleUri(@NonNull Uri uri, int from) {
                    return false;
                }

                @NonNull
                @Override
                public UriHandlerResult handleUriWithResult(@NonNull Uri uri, int from) {
                    return UriHandlerResult.NOT_HANDLED;
                }
            };
        }

        @NonNull
        @Override
        public SpeechKitManagerFactory getSpeechKitManagerFactory() {
            return context -> new SpeechKitManagerProvider() {
                @Nullable
                @Override
                public SpeechKitManager getSpeechKitManager() {
                    return null;
                }

                @Nullable
                @Override
                public SpeechKitManagerAccess getSpeechKitManagerAccess() {
                    return null;
                }
            };
        }

        @NonNull
        @Override
        public AliceActivityStarter getAliceActivityStarter() {
            return new AliceActivityStarter() {
                @Override
                public boolean launchAlice(@NonNull Context context) {
                    launchAliceFromContextCard(context);
                    return true;
                }

                @Override
                public void launchAliceFromContextCard(@NonNull Context context) {
                    launchAliceActivity(context);
                }

                private void launchAliceActivity(@NonNull Context context) {
                    Intent intent = new Intent(context, TestAliceDialogActivity.class);
                    IntentUtils.fillAliceFlags(intent);
                    context.startActivity(intent);
                }

                @NonNull
                @Override
                public Class<? extends Activity> getAliceLauncherProxyActivity() {
                    return TestAliceLauncherProxyActivity.class;
                }

                @NonNull
                @Override
                public Class<? extends Activity> getAliceAuthActivity() {
                    return TestAliceAccountManagerLauncherActivity.class;
                }
            };
        }

        @NonNull
        @Override
        public AliceImageCacheManager getImageCacheManager() {
            return new AliceImageCacheManager() {
                @NonNull
                @Override
                public DefaultImageCache getDefaultImageCache() {
                    return new DefaultImageCache(ApplicationProvider.getApplicationContext(),
                            new DefaultImagesParams(), new SharedBitmapLruCache(500), null);
                }

                @NonNull
                @Override
                public SharedBitmapLruCache getLruCache() {
                    return new SharedBitmapLruCache(500);
                }

                @Override
                public void onTrimMemory(int level) {
                }
            };
        }

        @NonNull
        @Override
        public SearchAppShortcutsDelegateFactory getSearchAppShortcutsDelegateFactory() {
            return activity -> SearchAppShortcutsDelegate.NOOP;
        }

        @NonNull
        @Override
        public AccountManagerFacade getAccountManagerFacade() {
            return accountManagerFacade;
        }
    };

    public TestApplication() {
        IAppConfigBuilder appConfigBuilder = AppConfig.newBuilder();
        // disabling Metrica since it breaks robolectric tests
        appConfigBuilder.setMetrikaEnabled(false);
        appConfigBuilder.setMetrikaId(BuildConfig.METRIKA_ID);
        appConfigBuilder.setLogEnabled(BuildConfig.LOG_ENABLED);
        appConfigBuilder.setDebugSettingsEnabled(BuildConfig.DEBUG_SETTINGS);
        appConfigBuilder.setFeedbackLogsEnabled(BuildConfig.FEEDBACK_LOGS_ENABLED);
        // disabling Feedback to prevent crash in corner cases
        appConfigBuilder.setFeedbackEnabled(false);
        appConfigBuilder.setFilterUnsafeLogs(BuildConfig.FILTER_UNSAFE_LOGS);
        AppConfig.setNewConfig(appConfigBuilder);
    }
}
