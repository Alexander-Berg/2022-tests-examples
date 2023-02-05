package com.yandex.mail.developer_settings;

import com.yandex.mail.ads.NativeAdLoaderWrapper;
import com.yandex.mail.model.DeveloperSettingsModel;
import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(IntegrationTestRunner.class)
public class AdsProxyImplTest {

    @SuppressWarnings("NullableProblems")
    @NonNull
    private DeveloperSettingsModel developerSettingsModel;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private AdsProxy adsProxy;

    @Nullable
    private String preferredBlockId;

    @Before
    public void setUp() {
        developerSettingsModel = mock(DeveloperSettingsModel.class);
        when(developerSettingsModel.getFakeAdBlockId()).thenAnswer(invocation -> preferredBlockId);

        adsProxy = new AdsProxyImpl(IntegrationTestRunner.app(), developerSettingsModel);
    }

    @Test
    public void proxyNativeAdLoader_shouldReturnPassedLoaderIfPreferredBlockIdNull() {
        preferredBlockId = null;
        NativeAdLoaderWrapper nativeAdLoader = mock(NativeAdLoaderWrapper.class);
        assertThat(adsProxy.proxyNativeAdLoader(nativeAdLoader)).isSameAs(nativeAdLoader);
    }

    // Since it works via reflection and Ad SDK is proguarded we can not actually verify the behavior :(
    @Test
    public void proxyNativeAdLoader_shouldReturnAnotherObjectIfPreferredBlockIdIsNotNull() {
        preferredBlockId = "testBlockId";
        NativeAdLoaderWrapper nativeAdLoader = mock(NativeAdLoaderWrapper.class);

        // The only thing we can check is that returned loader is not the original one
        assertThat(adsProxy.proxyNativeAdLoader(nativeAdLoader)).isNotSameAs(nativeAdLoader);
    }

    @Test
    public void proxyMetricaPrefix_shouldChangePrefix() {
        final String prefix = "drawer";
        preferredBlockId = "testBlockId";

        assertThat(adsProxy.proxyMetricaPrefix(prefix)).isNotEmpty();
        assertThat(adsProxy.proxyMetricaPrefix(prefix)).isNotEqualTo(prefix);
    }

    @Test
    public void proxyMetricaPrefix_shouldJustReturnPassedPrefix() {
        final String prefix = "drawer";
        preferredBlockId = null;

        assertThat(adsProxy.proxyMetricaPrefix(prefix)).isSameAs(prefix);
    }
}
