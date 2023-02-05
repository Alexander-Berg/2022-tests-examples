
package com.yandex.mail.developer_settings;

import com.yandex.mail.ads.NativeAdLoaderWrapper;
import com.yandex.mail.runners.UnitTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(UnitTestRunner.class)
public class AdsProxyNoOpTest {

    @Test
    public void proxyNativeAdLoader_shouldJustReturnPassedAdLoader() {
        AdsProxy adsProxy = new AdsProxyNoOp();
        NativeAdLoaderWrapper nativeAdLoader = mock(NativeAdLoaderWrapper.class);

        assertThat(adsProxy.proxyNativeAdLoader(nativeAdLoader)).isSameAs(nativeAdLoader);
        verifyNoInteractions(nativeAdLoader);
    }

    @Test
    public void proxyNativeAdLoader_shouldJustReturnPassedPrefix() {
        final String prefix = "drawer";
        AdsProxy adsProxy = new AdsProxyNoOp();

        assertThat(adsProxy.proxyMetricaPrefix(prefix)).isSameAs(prefix);
    }
}
