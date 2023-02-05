package com.yandex.mail.ads;

import com.yandex.mail.runners.UnitTestRunner;
import com.yandex.mobile.ads.nativeads.NativeAd;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import nl.jqno.equalsverifier.EqualsVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(UnitTestRunner.class)
public class NativeAdHolderTest {

    // Before
    @SuppressWarnings("NullableProblems")
    @NonNull
    NativeAd appInstallAd;

    // Before
    @SuppressWarnings("NullableProblems")
    @NonNull
    NativeAd contentAd;

    // Before
    @SuppressWarnings("NullableProblems")
    @NonNull
    NativeAdViewHolder adViewHolder;

    @Before
    public void beforeEachTestCase() {
        appInstallAd = mock(NativeAd.class);
        contentAd = mock(NativeAd.class);
        adViewHolder = mock(NativeAdViewHolder.class);
    }

    @Test
    public void equals_hashcode_verify() {
        EqualsVerifier
                .forClass(NativeAdHolder.class)
                .withPrefabValues(NativeAd.class, mock(NativeAd.class), mock(NativeAd.class))
                .withPrefabValues(NativeAd.class, mock(NativeAd.class), mock(NativeAd.class))
                .withIgnoredFields("metricaPrefix")
                .verify();
    }

    @Test
    public void getAdType_appInstallAd() {
        NativeAdHolder nativeAdHolder = NativeAdHolder.createAppInstallAdHolder(appInstallAd, "prefix");
        assertThat(nativeAdHolder.getAdType()).isEqualTo("install_ad");
    }

    @Test
    public void getAdType_contentAd() {
        NativeAdHolder nativeAdHolder = NativeAdHolder.createContentAdHolder(contentAd, "prefix");
        assertThat(nativeAdHolder.getAdType()).isEqualTo("content_ad");
    }

    @Test
    public void bind_appInstallAdView() {
        NativeAdHolder.createAppInstallAdHolder(appInstallAd, "mock").bind(adViewHolder);
        verify(adViewHolder).bindInstallView(appInstallAd);
        verify(adViewHolder).bindContentView(null);
    }

    @Test
    public void bind_contentAdView() {
        NativeAdHolder.createContentAdHolder(contentAd, "mock").bind(adViewHolder);
        verify(adViewHolder).bindContentView(contentAd);
        verify(adViewHolder).bindInstallView(null);
    }
}
