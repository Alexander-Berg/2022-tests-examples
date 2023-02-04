package ru.yandex.general.step;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.rules.TestWatcher;
import ru.yandex.general.config.GeneralWebConfig;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.api.UnsafeOkHttpClient.getUnsafeOkHttpClient;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.consts.Pages.TRANSPORTIROVKA_PERENOSKI;

public class JSoupSteps extends TestWatcher {

    public static final String DESKTOP_ROBOT_USERAGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
    public static final String MOBILE_ROBOT_USERAGENT = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/W.X.Y.Z‡ Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
    public static final String DESKTOP_USERAGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36";
    public static final String MOBILE_USERAGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1";

    public static final String CANONICAL_LOCATOR = "head link[rel='canonical']";
    private static final String LD_JSON_LOCATOR = "head script[type='application/ld+json']";
    private String userAgent;
    private Map<String, String> cookies;
    private Connection.Response response;

    @Getter
    Document webPage;

    @Inject
    private GeneralWebConfig config;

    private UriBuilder uriBuilder;

    public JSoupSteps() {
        cookies = new HashMap<>();
    }

    @Step("Выполняем запрос, ждем ответа с кодом «200»")
    public void get() {
        await().atMost(20, SECONDS).ignoreExceptions()
                .pollInterval(2, SECONDS)
                .until(() -> {
                    response = getResponse(uriBuilder.build().toString());
                    return response.statusCode();
                }, equalTo(200));
        try {
            webPage = response.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Step("Получаем страницу по ссылке «{url}»")
    private Connection.Response getResponse(String url) throws IOException {
        return Jsoup.connect(url)
                .header("User-Agent", userAgent)
                .cookies(cookies)
                .sslSocketFactory(getUnsafeOkHttpClient().sslSocketFactory())
                .timeout(3000)
                .execute();
    }

    @Step("Ищем «{locator}»")
    public Elements select(String locator) {
        return webPage.select(locator);
    }

    @Step("Устанавливаем UserAgent десктопного робота")
    public JSoupSteps setDesktopRobotUserAgent() {
        userAgent = DESKTOP_ROBOT_USERAGENT;
        return this;
    }

    @Step("Устанавливаем UserAgent мобильного робота")
    public JSoupSteps setMobileRobotUserAgent() {
        userAgent = MOBILE_ROBOT_USERAGENT;
        return this;
    }

    @Step("Устанавливаем десктопный UserAgent")
    public JSoupSteps setDesktopUserAgent() {
        userAgent = DESKTOP_USERAGENT;
        return this;
    }

    @Step("Устанавливаем мобильный UserAgent")
    public JSoupSteps setMobileUserAgent() {
        userAgent = MOBILE_USERAGENT;
        return this;
    }

    @Step("Устанавливаем порт мокрицы в куки")
    public JSoupSteps setMockritsaImposter(String port) {
        cookies.put("mockritsa_imposter", port);
        return this;
    }

    public JSoupSteps testing() {
        uriBuilder = UriBuilder.fromUri(config.getTestingURI());
        return this;
    }

    public JSoupSteps realProduction() {
        uriBuilder = UriBuilder.fromUri(config.getRealProductionURI());
        return this;
    }

    public JSoupSteps path(String path) {
        uriBuilder.path(path);
        return this;
    }

    @Step("Добавляем параметр «{name}» = «{value}» к билдеру")
    public JSoupSteps queryParam(String name, String value) {
        uriBuilder.queryParam(name, value);
        return this;
    }

    public JSoupSteps uri(String resource) {
        uriBuilder.uri(resource);
        return this;
    }

    public String getActualOfferCardUrl() {
        testing().path(NOVOSIBIRSK).path(TOVARI_DLYA_ZHIVOTNIH).path(TRANSPORTIROVKA_PERENOSKI).get();
        return select("a[href*='/offer/']").attr("href");
    }

    public String getOfferCardUrl() {
        get();
        return select("a[href*='/offer/']").attr("href");
    }

    @Step("Получаем элемент LdJson разметки типа = «{type}»")
    public String getLdJsonMark(String type) {
        return getLdJsonElements(type).get(0).html();
    }

    @Step("Получаем элемент LdJson разметки типа «Logo»")
    public String getLdJsonMarkLogo() {
        return getLdJsonElements("Organization").stream().filter(
                        element -> {
                            JsonObject elementJson = new GsonBuilder().create().fromJson(element.html(), JsonObject.class);
                            return elementJson.has("logo");
                        })
                .collect(Collectors.toList())
                .get(0).html();
    }

    @Step("На странице нет разметки типа = «{type}»")
    public void noLdJsonMark(String type) {
        int size = getLdJsonElements(type).size();
        Assert.assertThat("На странице нет разметки типа = «{type}»", size, equalTo(0));
    }

    private List<Element> getLdJsonElements(String type) {
        Elements ldJsonElements = select(LD_JSON_LOCATOR);
        return ldJsonElements.stream().filter(
                        element -> {
                            JsonObject elementJson = new GsonBuilder().create().fromJson(element.html(), JsonObject.class);
                            return elementJson.get("@type").getAsString().equals(type);
                        })
                .collect(Collectors.toList());
    }

    public String getItempropContent(String itemprop) {
        return select(format("meta[itemprop='%s']", itemprop)).attr("content");
    }

    public String toString() {
        return uriBuilder.build().toString();
    }

}