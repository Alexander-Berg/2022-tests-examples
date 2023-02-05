package com.yandex.mail.ads;

import android.view.ViewStub;

import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.rules.BuildConfigRule;
import com.yandex.mail.rules.RunOnlyOnDebugBuilds;
import com.yandex.mail.rules.RunOnlyOnReleaseBuilds;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdException;
import com.yandex.mobile.ads.nativeads.NativeAdView;
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(IntegrationTestRunner.class)
public class NativeAdViewHolderTest {

    @Rule
    public BuildConfigRule buildConfigRule = new BuildConfigRule();

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private NativeAd nativeAppInstallAd;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private NativeAd nativeContentAd;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private NativeAdViewHolder nativeAdViewHolder;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private NativeAdView nativeAppInstallAdView;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private NativeAdView nativeContentAdView;

    @Before
    public void beforeEachTest() {
        nativeAppInstallAd = mock(NativeAd.class);
        nativeContentAd = mock(NativeAd.class);
        ViewStub contentAdViewStub = mock(ViewStub.class);
        ViewStub installAdViewStub = mock(ViewStub.class);

        nativeAppInstallAdView = new NativeAdView(IntegrationTestRunner.app());
        when(installAdViewStub.inflate()).thenReturn(nativeAppInstallAdView);
        nativeContentAdView = new NativeAdView(IntegrationTestRunner.app());
        when(contentAdViewStub.inflate()).thenReturn(nativeContentAdView);

        nativeAdViewHolder =
                new NativeAdViewHolder(mock(YandexMailMetrica.class), contentAdViewStub, installAdViewStub, false);
    }

    @Test
    public void bindView_shouldNoOpIfAppInstallAdNull() throws NativeAdException {
        nativeAdViewHolder.bindInstallView(null);
        verify(nativeAppInstallAd, never()).bindNativeAd(any());
    }

    @Test
    @RunOnlyOnDebugBuilds
    public void bindView_shouldCrashOnDebugBuildsIfAppInstallAdCanNotBeBound() throws NativeAdException {
        doThrow(new RuntimeException("test exception")).when(nativeAppInstallAd)
                .bindNativeAd(new NativeAdViewBinder.Builder(nativeAppInstallAdView).build());

        assertThatThrownBy(() -> nativeAdViewHolder.bindInstallView(nativeAppInstallAd))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Can not bind app install ad to the view");
    }

    @Test
    @RunOnlyOnReleaseBuilds
    public void bindView_shouldNotCrashOnReleaseBuildsIfAppInstallAdCanNotBeBound() throws NativeAdException {
        doThrow(new RuntimeException("test exception")).when(nativeAppInstallAd)
                .bindNativeAd(new NativeAdViewBinder.Builder(nativeAppInstallAdView).build());

        nativeAdViewHolder.bindInstallView(nativeAppInstallAd);
        // We do not expect exceptions here.
    }

    @Test
    public void bindView_shouldNoOpIfContentAdNull() throws NativeAdException {
        nativeAdViewHolder.bindContentView(null);
        verify(nativeContentAd, never()).bindNativeAd(any());
    }

    @Test
    @RunOnlyOnDebugBuilds
    public void bindView_shouldCrashOnDebugBuildsIfContentAdCanNotBeBound() throws NativeAdException {
        doThrow(new RuntimeException("test exception")).when(nativeContentAd)
                .bindNativeAd(new NativeAdViewBinder.Builder(nativeContentAdView).build());

        assertThatThrownBy(() -> nativeAdViewHolder.bindContentView(nativeContentAd))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Can not bind content ad to the view");
    }

    @Test
    @RunOnlyOnReleaseBuilds
    public void bindView_shouldCatchExceptionsInReleaseBuildsIfContentAdCanNotBeBound() throws NativeAdException {
        doThrow(new RuntimeException("test exception")).when(nativeContentAd)
                .bindNativeAd(new NativeAdViewBinder.Builder(nativeContentAdView).build());

        nativeAdViewHolder.bindContentView(nativeContentAd);
        // We do not expect exceptions here.
    }
}
