package ru.yandex.market.net;

import junit.framework.Assert;

import org.junit.Test;

import ru.yandex.market.BaseTest;


public final class ContentVersionHelperTest extends BaseTest {

    /**
     * Тест из тикета
     */
    @Test
    public void basicTest() {
        final String actual = ContentVersionHelper.generateContentVersion("2017-01-19",
                "GET",
                "3.31", "ANDROID", "TABLET",
                "cdf36c5e1f5a0b031a6e52ddd4677443",
                "/v2.0.0/notifications/recommendations?geo_id=108052&ip=::ffff:78.191.200.37" +
                        "&lac=60208&cellid=144441454&operatorid=1&countrycode=286&signalstrength=0" +
                        "&wifinetworks=7AF3A34A087C:-69,AC9E17504DCC:-88" +
                        "&uuid=cdf36c5e1f5a0b031a6e52ddd4677443&sections=medicine",
                "a4f2"
        );

        Assert.assertEquals(
                "685172d420b9a5f4f0d11427ec710c78a4f2",
                actual
        );
    }

}
