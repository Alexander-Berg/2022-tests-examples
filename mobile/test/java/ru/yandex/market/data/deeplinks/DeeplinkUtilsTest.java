package ru.yandex.market.data.deeplinks;

import android.os.Build;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;

import ru.yandex.market.NetworkSecurityPolicyWorkaround;
import ru.yandex.market.NoOpMarketApplication;
import ru.yandex.market.ShadowMetricaPushBroadcastReceiver;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class DeeplinkUtilsTest {

    @RunWith(ParameterizedRobolectricTestRunner.class)
    @Config(
            sdk = {Build.VERSION_CODES.P},
            application = NoOpMarketApplication.class,
            shadows = {
                    ShadowMetricaPushBroadcastReceiver.class,
                    NetworkSecurityPolicyWorkaround.class
            }
    )
    public static class MarketRelativeUrlTests {

        public String input;

        public String expected;

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: \"{0}\" → \"{1}\"")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"http://beru.ru", ""},
                    {"http://yandex.ru/product/12840640?hid=6427100",
                            "http://yandex.ru/product/12840640?hid=6427100"},
                    {"beru://product/12840640?hid=6427100",
                            "beru://product/12840640?hid=6427100"},
                    {"https://example.com/product/12840640?hid=6427100",
                            "https://example.com/product/12840640?hid=6427100"}
            });
        }

        public MarketRelativeUrlTests(final String input, final String expected) {
            this.input = input;
            this.expected = expected;
        }

        @Test
        public void successToMarketRelativeUrl() {
            assertThat(DeeplinkUtils.toMarketRelativeUrl(input), equalTo(expected));
        }

    }

    @RunWith(ParameterizedRobolectricTestRunner.class)
    @Config(
            sdk = {Build.VERSION_CODES.P},
            application = NoOpMarketApplication.class,
            shadows = {
                    ShadowMetricaPushBroadcastReceiver.class,
                    NetworkSecurityPolicyWorkaround.class
            }
    )
    public static class ToAppLinkTest {

        public String input;

        public String expected;

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: \"{0}\" → \"{1}\"")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"http://beru.ru", "beru://"},
                    {"https://beru.ru", "beru://"},
                    {"https://m.beru.ru/product/12840640?hid=6427100",
                            "beru://product/12840640?hid=6427100"},

                    {"beru://incorrect/path", "beru://incorrect/path"},
                    {"beru://", "beru://"},
                    {"beru://product/12840640?hid=6427100", "beru://product/12840640?hid=6427100"},
                    {"beru://my/orders/12345", "beru://my/orders/12345"},
                    {"beru://my/orders/aaaa", "beru://my/orders/aaaa"},

                    {"https://mail.yandex.ru", "https://mail.yandex.ru"},
                    {"http://yandex.ru/product/12840640?hid=6427100",
                            "http://yandex.ru/product/12840640?hid=6427100"},
                    {"https://yandex.ru/product/12840640?hid=6427100",
                            "https://yandex.ru/product/12840640?hid=6427100"},

                    {"https://example.ru/product/12840640?hid=6427100",
                            "https://example.ru/product/12840640?hid=6427100"}
            });
        }

        public ToAppLinkTest(final String input, final String expected) {
            this.input = input;
            this.expected = expected;
        }

        @Test
        public void convertUrlToSchema() {
            assertThat(DeeplinkUtils.toAppLink(DeeplinkUtils.SCHEME_BERU, input),
                    equalTo(expected));
        }
    }
}