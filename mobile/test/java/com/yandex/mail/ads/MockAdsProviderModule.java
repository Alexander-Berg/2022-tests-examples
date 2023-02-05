package com.yandex.mail.ads;

import com.pushtorefresh.storio3.Optional;
import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.ads.AdsProvider.Status;
import com.yandex.mail.developer_settings.AdsProxy;
import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.settings.GeneralSettings;

import androidx.annotation.NonNull;
import io.reactivex.Observable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockAdsProviderModule extends AdsProviderModule {

    @NonNull
    @Override
    public AdsProvider provideTopContentProvider(
            @NonNull BaseMailApplication context,
            @NonNull AdsAvailability adsAvailability,
            @NonNull AdsProxy adsProxy,
            @NonNull YandexMailMetrica metrica,
            @NonNull GeneralSettings generalSettings
    ) {
        return mockNativeAdsProvider();
    }

    @NonNull
    private AdsProvider mockNativeAdsProvider() { // ja pierdole
        NativeAdsProvider nativeAdsProvider = mock(NativeAdsProvider.class);
        when(nativeAdsProvider.reloadAds()).thenReturn(nativeAdsProvider);
        when(nativeAdsProvider.getAdsObservable()).thenReturn(Observable.just(Optional.empty()));
        when(nativeAdsProvider.getStatusObservable()).thenReturn(io.reactivex.Observable.just(Status.UNAVAILABLE));
        return nativeAdsProvider;
    }
}
