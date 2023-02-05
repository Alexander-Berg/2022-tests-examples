package com.yandex.mail.ads;

import com.pushtorefresh.storio3.Optional;

import org.junit.Before;
import org.junit.Test;

import androidx.annotation.NonNull;
import io.reactivex.observers.TestObserver;

import static org.assertj.core.api.Assertions.assertThat;

public class NoAdsProviderTest {

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private AdsProvider noAdsProvider;

    @Before
    public void beforeEachTest() {
        noAdsProvider = new NoAdsProvider();
    }

    @Test
    public void reloadAds_shouldReturnSameAdsProvider() {
        assertThat(noAdsProvider.reloadAds()).isSameAs(noAdsProvider);
    }

    @Test
    public void reloadAds_shouldNoOp() {
        TestObserver<Optional<NativeAdHolder>> testObserver = noAdsProvider.getAdsObservable().test();
        testObserver.assertValue(Optional.empty());

        noAdsProvider.reloadAds();
        // We don't expect new emission after reloadAds(), subscriber should see only old null value.
        testObserver.assertValue(Optional.empty());
    }

    @Test
    public void getAdsObservable_shouldEmitNullForEachNewSubscriber() {
        TestObserver<Optional<NativeAdHolder>> testObserver1 = noAdsProvider.getAdsObservable().test();
        testObserver1.assertValue(Optional.empty());

        TestObserver<Optional<NativeAdHolder>> testObserver2 = noAdsProvider.getAdsObservable().test();
        testObserver2.assertValue(Optional.empty());

        // Old subscriber should not see new emission, he should just see one old null value
        testObserver1.assertValue(Optional.empty());
    }
}
