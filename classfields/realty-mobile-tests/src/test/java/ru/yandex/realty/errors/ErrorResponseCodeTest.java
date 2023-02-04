package ru.yandex.realty.errors;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtySeoModule;

import java.io.IOException;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.api.UnsafeOkHttpClient.getUnsafeOkHttpClient;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;

@DisplayName("Errors tests")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtySeoModule.class)
public class ErrorResponseCodeTest {

    private static final String URL_404 = "https://m.realty.yandex.ru/moskva/kupit/kvartira/cardIndex=0";
    private static final String OFFER_URL_404 = "https://m.realty.yandex.ru/offer/1";
    private static final String VILLAGE_URL_404 = "https://m.realty.test.vertis.yandex.ru/moskva/kupit/kottedzhnye-poselki/elizavetinskoe/?id=1852000&rgid=292840";
    private static final String URL_500 = "https://m.realty.test.vertis.yandex.ru/moskva/?disable-api=region_info";

    private static final HttpLoggingInterceptor LOGGING_INTERCEPTOR = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BASIC);

    private final OkHttpClient client = getUnsafeOkHttpClient().newBuilder()
            .addInterceptor(LOGGING_INTERCEPTOR).addInterceptor(new AllureOkHttp3()).build();

    @Test
    @DisplayName("Видим 404")
    @Owner(KANTEMIROV)
    public void shouldSee404() throws IOException {
        Request request = new Request.Builder().get().url(URL_404).build();
        int code = client.newCall(request).execute().code();
        assertThat(code).isEqualTo(HTTP_NOT_FOUND);
    }

    @Test
    @DisplayName("Видим 404 при переходе на несуществующий оффер")
    @Owner(KANTEMIROV)
    public void shouldSee404Offer() throws IOException {
        Request request = new Request.Builder().get().url(OFFER_URL_404).build();
        int code = client.newCall(request).execute().code();
        assertThat(code).isEqualTo(HTTP_NOT_FOUND);
    }

    @Test
    @DisplayName("Видим 404 при переходе на несуществующий поселок")
    @Owner(KANTEMIROV)
    public void shouldSee404Village() throws IOException {
        Request request = new Request.Builder().get().url(VILLAGE_URL_404).build();
        int code = client.newCall(request).execute().code();
        assertThat(code).isEqualTo(HTTP_NOT_FOUND);
    }

    @Test
    @DisplayName("Видим 500")
    @Owner(KANTEMIROV)
    public void shouldSee500() throws IOException {
        Request request = new Request.Builder().get().url(URL_500).build();
        int code = client.newCall(request).execute().code();
        assertThat(code).isEqualTo(HTTP_INTERNAL_ERROR);
    }
}
