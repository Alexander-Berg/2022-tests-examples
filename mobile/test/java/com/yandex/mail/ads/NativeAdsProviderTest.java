package com.yandex.mail.ads;

import com.pushtorefresh.storio3.Optional;
import com.yandex.mail.ads.AdsProvider.Status;
import com.yandex.mail.ads.NativeAdLoaderWrapper.AdRequestErrorWrapper;
import com.yandex.mail.ads.NativeAdLoaderWrapper.OnLoadListenerWrapper;
import com.yandex.mail.developer_settings.AdsProxy;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.GeneralSettings;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.nativeads.NativeAd;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;

import static android.os.Looper.getMainLooper;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.robolectric.Shadows.shadowOf;

@RunWith(IntegrationTestRunner.class)
public class NativeAdsProviderTest extends BaseIntegrationTest {

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private NativeAdLoaderWrapper nativeAdLoader;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private final AtomicReference<OnLoadListenerWrapper> onLoadListener = new AtomicReference<>();

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private NativeAdsProvider nativeAdsProvider;

    @SuppressWarnings("NullableProblems")  // Initialized in @Before.
    @NonNull
    private AdsProxy adsProxy;

    @SuppressWarnings("NullableProblems")  // Initialized in @Before.
    @NonNull
    private TestScheduler timeScheduler;

    @Before
    public void beforeEachTest() {
        init(Accounts.testLoginData);
        nativeAdLoader = mock(NativeAdLoaderWrapper.class);

        doAnswer(
                invocation -> {
                    onLoadListener.set((OnLoadListenerWrapper) invocation.getArguments()[0]);
                    return null;
                }
        )
                .when(nativeAdLoader)
                .setListener(any(OnLoadListenerWrapper.class));

        adsProxy = mock(AdsProxy.class);
        when(adsProxy.proxyNativeAdLoader(nativeAdLoader)).thenReturn(nativeAdLoader);
        when(adsProxy.proxyMetricaPrefix(anyString())).thenReturn("prefix_");

        timeScheduler = new TestScheduler();

        initNativeAdsProvider(true);
    }

    public void initNativeAdsProvider(boolean showAds) {
        final GeneralSettings generalSettings = new GeneralSettings(IntegrationTestRunner.app());
        generalSettings.edit().setAdShown(showAds).apply();

        nativeAdsProvider = new NativeAdsProvider(
                new ClassicAdsAvailability(generalSettings),
                nativeAdLoader,
                adsProxy,
                metrica,
                "mock",
                timeScheduler
        );
        shadowOf(getMainLooper()).idle();
    }

    @Test
    public void getAdsObservable_shouldCacheLastValueOfAppInstallAd() {
        TestObserver<NativeAdHolder> testObserver1 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        verify(nativeAdLoader).loadAd(any(AdRequest.class));

        // No ads were loaded, so subscriber should not receive any value at this moment
        testObserver1.assertNoValues();
        testObserver1.assertNoErrors();

        NativeAd appInstallAd1 = mock(NativeAd.class);
        NativeAdHolder expectedAd1 = NativeAdHolder.createAppInstallAdHolder(appInstallAd1, "mock");
        onLoadListener.get().onAppInstallAdLoaded(appInstallAd1);
        shadowOf(getMainLooper()).idle();

        // Subscriber should receive appInstallAd
        testObserver1.assertValue(expectedAd1);

        // This subscriber subscribes after first load of the add
        TestObserver<NativeAdHolder> testObserver2 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        testObserver2.assertValue(expectedAd1);

        // Emit new app install ad
        NativeAd appInstallAd2 = mock(NativeAd.class);
        NativeAdHolder expectedAd2 = NativeAdHolder.createAppInstallAdHolder(appInstallAd2, "mock");
        onLoadListener.get().onAppInstallAdLoaded(appInstallAd2);
        shadowOf(getMainLooper()).idle();

        testObserver1.assertValues(expectedAd1, expectedAd2);
        testObserver2.assertValues(expectedAd1, expectedAd2);

        // Verify that NativeAdsProvider caches only one last value
        TestObserver<NativeAdHolder> testObserver3 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        testObserver3.assertValue(expectedAd2);

        testObserver1.assertNotTerminated();
        testObserver2.assertNotTerminated();
        testObserver3.assertNotTerminated();
    }

    @Test
    public void getAdsObservable_shouldCacheLastValueOfContentAd() {
        TestObserver<NativeAdHolder> testObserver1 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        verify(nativeAdLoader).loadAd(any(AdRequest.class));
        shadowOf(getMainLooper()).idle();

        // No ads were loaded, so subscriber should not receive any value at this moment
        testObserver1.assertNoValues();
        testObserver1.assertNoErrors();

        NativeAd contentAd1 = mock(NativeAd.class);
        NativeAdHolder expectedAd1 = NativeAdHolder.createContentAdHolder(contentAd1, "mock");
        onLoadListener.get().onContentAdLoaded(contentAd1);
        shadowOf(getMainLooper()).idle();

        // Subscriber should receive appInstallAd
        testObserver1.assertValue(expectedAd1);

        // This subscriber subscribes after first load of the add
        TestObserver<NativeAdHolder> testObserver2 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        testObserver2.assertValue(expectedAd1);

        // Emit new content ad
        NativeAd contentAd2 = mock(NativeAd.class);
        NativeAdHolder expectedAd2 = NativeAdHolder.createContentAdHolder(contentAd2, "mock");
        onLoadListener.get().onContentAdLoaded(contentAd2);
        shadowOf(getMainLooper()).idle();

        testObserver1.assertValues(expectedAd1, expectedAd2);
        testObserver2.assertValues(expectedAd1, expectedAd2);

        // Verify that NativeAdsProvider caches only one last value
        TestObserver<NativeAdHolder> testObserver3 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        testObserver3.assertValue(expectedAd2);

        testObserver1.assertNotTerminated();
        testObserver2.assertNotTerminated();
        testObserver3.assertNotTerminated();
    }

    @Test
    public void getAdsObservable_shouldCacheLastValueOfContentAndAppInstallAd() {
        TestObserver<NativeAdHolder> testObserver1 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        verify(nativeAdLoader).loadAd(any(AdRequest.class));

        // No ads were loaded, so subscriber should not receive any value at this moment
        testObserver1.assertNoValues();
        testObserver1.assertNoErrors();

        // First ad is NativeContentAd
        NativeAd ad1 = mock(NativeAd.class);
        NativeAdHolder expectedAd1 = NativeAdHolder.createContentAdHolder(ad1, "mock");
        onLoadListener.get().onContentAdLoaded(ad1);
        shadowOf(getMainLooper()).idle();

        // Subscriber should receive appInstallAd
        testObserver1.assertValue(expectedAd1);

        // This subscriber subscribes after first load of the add
        TestObserver<NativeAdHolder> testObserver2 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        testObserver2.assertValue(expectedAd1);

        // Second ad is NativeAppInstallAd
        NativeAd ad2 = mock(NativeAd.class);
        NativeAdHolder expectedAd2 = NativeAdHolder.createAppInstallAdHolder(ad2, "mock");
        onLoadListener.get().onAppInstallAdLoaded(ad2);
        shadowOf(getMainLooper()).idle();

        testObserver1.assertValues(expectedAd1, expectedAd2);
        testObserver2.assertValues(expectedAd1, expectedAd2);

        // Verify that NativeAdsProvider caches only one last value
        TestObserver<NativeAdHolder> testObserver3 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        testObserver3.assertValue(expectedAd2);

        // Third ad is NativeContentAd
        // Mix types of the ad to verify that provider caches values of all types in one cache
        NativeAd ad3 = mock(NativeAd.class);
        NativeAdHolder expectedAd3 = NativeAdHolder.createContentAdHolder(ad3, "mock");
        onLoadListener.get().onContentAdLoaded(ad3);
        shadowOf(getMainLooper()).idle();

        testObserver1.assertValues(expectedAd1, expectedAd2, expectedAd3);
        testObserver2.assertValues(expectedAd1, expectedAd2, expectedAd3);
        testObserver3.assertValues(expectedAd2, expectedAd3);

        // Verify that NativeAdsProvider caches only one last value
        TestObserver<NativeAdHolder> testObserver4 = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();
        shadowOf(getMainLooper()).idle();

        testObserver4.assertValue(expectedAd3);
    }

    @Test
    public void reloadAds_shouldReturnSameAdsProvider() {
        assertThat(nativeAdsProvider.reloadAds()).isSameAs(nativeAdsProvider);
    }

    @Test
    public void reloadAds_shouldEmitNullIfCurrentNativeAdLoaderNull() {
        when(adsProxy.proxyNativeAdLoader(nativeAdLoader)).thenReturn(null);
        initNativeAdsProvider(true);

        TestObserver<Optional<NativeAdHolder>> testObserver = nativeAdsProvider.getAdsObservable().test();
        nativeAdsProvider.reloadAds();

        // 1st null from constructor with null NativeAdLoader
        // 2nd null from reloadAds with null NativeAdLoader
        testObserver.assertValues(Optional.empty(), Optional.empty());
        testObserver.assertNotTerminated();
    }

    @Test
    public void reloadAds_shouldTriggerLoadOfAdsFromNativeAdLoader() {
        TestObserver<NativeAdHolder> testObserver = nativeAdsProvider.getAdsObservable().map(Optional::orNull).test();

        onLoadListener.get().onContentAdLoaded(mock(NativeAd.class));
        shadowOf(getMainLooper()).idle();
        clearInvocations(nativeAdLoader);

        nativeAdsProvider.reloadAds();
        shadowOf(getMainLooper()).idle();

        verify(nativeAdLoader).loadAd(any());
        testObserver.assertNotTerminated();
    }

    @Test
    public void reloadAds_shouldReloadOnlyIfProviderReady() {
        TestObserver<Status> testObserver = new TestObserver<>();
        nativeAdsProvider.getStatusObservable().subscribe(testObserver);
        shadowOf(getMainLooper()).idle();

        InOrder inOrder = inOrder(nativeAdLoader);

        inOrder.verify(nativeAdLoader).loadAd(any());
        shadowOf(getMainLooper()).idle();

        testObserver.assertValues(Status.LOADING);

        nativeAdsProvider.reloadAds();
        shadowOf(getMainLooper()).idle();

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void reloadAds_shouldLoadAdWithoutUid_ifExperimentIsOff() {
        initNativeAdsProvider(true);
        onLoadListener.get().onContentAdLoaded(mock(NativeAd.class));
        shadowOf(getMainLooper()).idle();
        nativeAdsProvider.reloadAds();
        shadowOf(getMainLooper()).idle();
        ArgumentCaptor<AdRequest> argumentCaptor = ArgumentCaptor.forClass(AdRequest.class);

        verify(nativeAdLoader, times(3)).loadAd(argumentCaptor.capture());
        AdRequest adRequest = argumentCaptor.getValue();
        String passportParam = adRequest.getParameters().get(NativeAdsProvider.PASSPORT_PARAM);
        assertThat(passportParam).isNull();
    }

    @Test
    public void adsObservable_shouldEmitNullAfterAdsDisablingInSettings() {
        TestObserver<Optional<NativeAdHolder>> testObserver = nativeAdsProvider.getAdsObservable().test();

        NativeAd appInstallAd1 = mock(NativeAd.class);
        NativeAdHolder expectedAd1 = NativeAdHolder.createAppInstallAdHolder(appInstallAd1, "mock");
        onLoadListener.get().onAppInstallAdLoaded(appInstallAd1);

        generalSettings.edit().setAdShown(false).apply();
        shadowOf(getMainLooper()).idle();

        testObserver.assertValues(Optional.of(expectedAd1), Optional.empty());
        testObserver.assertNotTerminated();
    }

    @Test
    public void statusObservable_shouldEmitUnavailableStatusOnError() {
        TestObserver<Status> testObserver = new TestObserver<>();
        nativeAdsProvider.getStatusObservable().subscribe(testObserver);
        shadowOf(getMainLooper()).idle();

        testObserver.assertValues(Status.LOADING);

        onLoadListener.get().onAdFailedToLoad(mock(AdRequestErrorWrapper.class));
        shadowOf(getMainLooper()).idle();

        testObserver.assertValues(Status.LOADING, Status.UNAVAILABLE);
    }

    @Test
    public void statusObservable_shouldEmitReadyStatusAfterDelayOnNetworkError() {
        statusObservable_shouldEmitReadyStatusAfterDelayOnError(AdRequestError.Code.NETWORK_ERROR, NativeAdsProvider.DELAY_NETWORK_ERROR);
    }

    @Test
    public void statusObservable_shouldEmitReadyStatusAfterDelayOnNoFill() {
        statusObservable_shouldEmitReadyStatusAfterDelayOnError(AdRequestError.Code.NO_FILL, NativeAdsProvider.DELAY_NO_FILL_ERROR);
    }

    @Test
    public void statusObservable_shouldEmitReadyStatusAfterDelayOnUnknownError() {
        statusObservable_shouldEmitReadyStatusAfterDelayOnError(AdRequestError.Code.UNKNOWN_ERROR, NativeAdsProvider.DELAY_UNKNOWN_ERROR);
    }

    private void statusObservable_shouldEmitReadyStatusAfterDelayOnError(int errorCode, long delay) {
        TestObserver<Status> testObserver = new TestObserver<>();
        nativeAdsProvider.getStatusObservable().subscribe(testObserver);
        shadowOf(getMainLooper()).idle();

        AdRequestErrorWrapper adRequestError = mock(AdRequestErrorWrapper.class);
        when(adRequestError.getCode()).thenReturn(errorCode);
        onLoadListener.get().onAdFailedToLoad(adRequestError);
        shadowOf(getMainLooper()).idle();

        timeScheduler.advanceTimeBy(delay - 1, MILLISECONDS);
        shadowOf(getMainLooper()).idle();

        testObserver.assertValues(Status.LOADING, Status.UNAVAILABLE);

        timeScheduler.advanceTimeBy(1, MILLISECONDS);
        shadowOf(getMainLooper()).idle();

        testObserver.assertValues(Status.LOADING, Status.UNAVAILABLE, Status.READY);
    }

    @Test
    public void statusObservable_shouldEmitReadyOnSuccessAfterError() {
        TestObserver<Status> testObserver = new TestObserver<>();
        nativeAdsProvider.getStatusObservable().subscribe(testObserver);
        shadowOf(getMainLooper()).idle();

        onLoadListener.get().onAdFailedToLoad(mock(AdRequestErrorWrapper.class));
        onLoadListener.get().onContentAdLoaded(mock(NativeAd.class));
        shadowOf(getMainLooper()).idle();

        testObserver.assertValues(Status.LOADING, Status.UNAVAILABLE, Status.READY);
    }
}
