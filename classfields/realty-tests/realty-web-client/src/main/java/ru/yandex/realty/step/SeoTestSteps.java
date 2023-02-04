package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.realty.config.RealtyWebConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.realty.api.UnsafeOkHttpClient.getUnsafeOkHttpClient;

public class SeoTestSteps {

    public static final Pattern PATTERN_1 = Pattern.compile("((\\d+) (объявлени.))");
    public static final Pattern PATTERN_2 = Pattern.compile("((\\d+) (жил.. комплекс(ов)?))");

    @Inject
    private RealtyWebConfig config;

    private static final HttpLoggingInterceptor logging = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BASIC);
    private static final OkHttpClient client = getUnsafeOkHttpClient().newBuilder()
            .addInterceptor(logging).addInterceptor(new AllureOkHttp3()).followRedirects(true).build();

    public String getTesting() {
        return config.getTestingURI().toString();
    }

    public String getMobileTesting() {
        URI uri = config.getTestingURI();
        UriBuilder uriBuilder = UriBuilder.fromUri(uri);
        uriBuilder.host(uri.getHost().replace("realty.", "m.realty."));
        return uriBuilder.toString();
    }

    @Step("Получаем урл редиректа для «{url}»")
    public String getNetworkResponseUrl(String url) {
        Request request = new Request.Builder().get().url(url).build();
        return given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(config.getUrlTimeout(), SECONDS).ignoreExceptions()
                .until(() -> client.newCall(request).execute(), response ->
                        response.isSuccessful()
                                && notNullValue().matches(response.request().url()))
                .request().url().toString();
    }

    @Step("Получаем урл мобильного редиректа для «{url}»")
    public String getNetworkMobileResponseUrl(String url) {
        Request request = new Request.Builder().get().url(url)
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 " +
                        "(KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1").build();
        return given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(config.getUrlTimeout(), SECONDS).ignoreExceptions()
                .until(() -> client.newCall(request).execute(), response ->
                        response.isSuccessful()
                                && notNullValue().matches(response.request().url()))
                .request().url().toString();
    }

    public String parametrizePattern(Pattern pattern, int groupToReplace, String stringForReplace) {
        Matcher matcher = pattern.matcher(stringForReplace);
        return matcher.find() ? stringForReplace.replace(matcher.group(groupToReplace), "(.*)") : stringForReplace;
    }

    public String replaceHostToMobile(URI uri) {
        return UriBuilder.fromUri(uri).host(
                UriBuilder.fromUri(uri.getHost().replace("realty.", "m.realty.")).toString()).build().toString();
    }

    public String replaceHostToAmp(URI uri) {
        return UriBuilder.fromUri(uri).path("/amp/").build().toString();
    }
}
