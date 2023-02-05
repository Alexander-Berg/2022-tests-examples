package com.yandex.mail;

import com.yandex.mail.ads.MockAdsProviderModule;
import com.yandex.mail.am.MockPassportApi;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.di.DaggerApplicationComponent;
import com.yandex.mail.di.SimpleApplicationModule;
import com.yandex.mail.entity.AccountEntity;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.fakeserver.MockWebServerHelper;
import com.yandex.mail.metrica.MockYandexMailMetricaModule;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.model.TestCrossAccountModule;
import com.yandex.mail.network.TestNetworkCommonModule;
import com.yandex.mail.network.TestNetworkModule;
import com.yandex.mail.pin.MockPinCodeModule;
import com.yandex.mail.service.CommandsServiceScheduler;
import com.yandex.mail.shadows.ShadowAsyncDifferConfig;
import com.yandex.mail.tools.TestContext;
import com.yandex.mail.tools.TestTimberTree;
import com.yandex.mail.tools.TestTimer;
import com.yandex.mail.tools.TestWorkerFactory;
import com.yandex.mail.tools.Tools;
import com.yandex.mail.util.RobolectricTimeModule;
import com.yandex.xplat.xmail.Registry;

import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.test.InstrumentationRegistry;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;
import timber.log.Timber;

import static com.yandex.mail.model.CleanupModelTest.cacheCleared;
import static com.yandex.mail.tools.RobolectricTools.enableFakeRxScheduler;
import static com.yandex.mail.tools.RobolectricTools.resetFakeRxScheduler;
import static com.yandex.mail.tools.RobolectricTools.setRxUncaughtExceptionHandler;
import static com.yandex.mail.util.ShadowLogUtils.configureTestShadowLog;
import static com.yandex.mail.util.Utils.requireNotNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read the following for the reference:
 * http://robolectric.org/custom-test-runner/
 * http://robolectric.blogspot.ru/2013/04/the-test-lifecycle-in-20.html
 */
@SuppressWarnings("unused")
public class TestMailApplication extends BaseMailApplication implements TestLifecycleApplication {

    @NonNull
    public static TestWorkerFactory workerFactory = new TestWorkerFactory();

    @NonNull
    private final MockWebServerHelper mockWebServerHelper = new MockWebServerHelper(this);

    @Nullable
    private TestTimberTree testTimberTree;

    @Override
    @NonNull
    protected DaggerApplicationComponent.Builder setupComponent() {
        return super.setupComponent()
                .simpleApplicationModule(new SimpleApplicationModule(this))
                .applicationModule(new TestApplicationModule())
                .networkCommonModule(new TestNetworkCommonModule(mockWebServerHelper))
                .networkModule(new TestNetworkModule())
                .storageModule(new TestStorageModule())
                .developerSettingsModule(new TestDeveloperSettingsModule())
                .pinCodeModule(new MockPinCodeModule())
                .yandexMetricaModule(new MockYandexMailMetricaModule())
                .adsProviderModule(new MockAdsProviderModule())
                .timeModule(new RobolectricTimeModule())
                .diskModule(new MockDiskModule())
                .crossAccountModule(new TestCrossAccountModule())
                .schedulersModule(new TestSchedulersModule());
    }

    @NonNull
    public MockPassportApi getPassportApi() {
        return (MockPassportApi) getApplicationComponent().passportApi();
    }

    @Override
    public void onCreate() {
        // injecting security policy hack before account manager initialization.
        Tools.removeCryptographyRestrictions();
        configureTestShadowLog();
        initStatic();
        mockWebServerHelper.start();
        super.onCreate();
        super.onCreateMainProcess();
    }

    /*
        TODO there's got to be a nicer way of resetting static state
        It might require annotating classes we test, however.
     */
    public static void initStatic() {
        FakeServer.reset();
        resetFakeRxScheduler();
        enableFakeRxScheduler(); // change default scheduler early for StorIO initialization
        prepareWorkManagerForTests();
    }

    private static void prepareWorkManagerForTests() {
        final Configuration configuration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setTaskExecutor(new SynchronousExecutor())
                .setWorkerFactory(workerFactory)
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(InstrumentationRegistry.getTargetContext(), configuration);
    }

    @Override
    protected void onCreateYandexMailProcess() {
        super.onCreateYandexMailProcess();
        reinitXmail();
        setRxUncaughtExceptionHandler();
    }

    private void reinitXmail() {
        Registry.registerHighPrecisionTimer(new TestTimer());
    }

    @Override
    public void beforeTest(@NonNull Method method) {
        TestContext.init();
        FakeServer.getInstance().getHandledRequests().clear();
        ShadowAsyncDifferConfig.setBackgroundThreadExecutor(ContextCompat.getMainExecutor(this));
    }

    @Override
    public void prepareTest(@NonNull Object o) {
    }

    @Override
    public void afterTest(@NonNull Method method) {
        final AccountModel accountModel = getApplicationComponent(this).accountModel();
        List<AccountEntity> accounts = accountModel.getAllAccounts().blockingGet();

        for (AccountEntity accountEntity : accounts) {
            AccountComponent component = getAccountComponent(this, accountEntity.getUid());
            component.cleanupModel().clearCache();
            assertThat(component.mailSqliteDriver()).is(cacheCleared());
        }
        TestContext.reset();
        mockWebServerHelper.shutdown();
        WorkManager.getInstance(getApplicationContext()).cancelAllWork();
        CommandsServiceScheduler.INSTANCE.reset();
        workerFactory.reset();
    }

    @Override
    protected void initTimber() {
        Timber.uprootAll();
        if (FeaturesConfig.getLOG()) {
            testTimberTree = new TestTimberTree();
            Timber.plant(testTimberTree);
        }
    }

    @NonNull
    public TestTimberTree getTestTimberTree() {
        return requireNotNull(testTimberTree, "Log should be enabled in tests");
    }
}
