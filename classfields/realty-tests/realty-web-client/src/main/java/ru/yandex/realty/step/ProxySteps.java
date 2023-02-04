package ru.yandex.realty.step;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.yandex.realty.beans.CodeResponse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.PHONE;
import static ru.yandex.realty.consts.OfferAdd.SMS_CODE;
import static ru.yandex.realty.lambdas.WatchException.watchException;
import static ru.yandex.realty.page.OfferAddPage.CONFIRM;

/**
 * Created by vicdev on 26.06.17.
 */
public class ProxySteps {

    public static final String AN_YANDEX_RU_META = "https://an.yandex.ru/meta/";
    public static final String MC_YANDEX_RU_WATCH = "https://mc.yandex.ru/watch/";
    public static final String AD_MAIL_RU = "https://ad.mail.ru";
    public static final String ADS_ADFOX_RU = "https://ads.adfox.ru/";
    public static final String IMP_ID = "imp-id";

    @Inject
    @Getter
    public ProxyServerManager proxyServerManager;

    @Inject
    private WebDriverManager webDriverManager;

    @Inject
    private ApiSteps api;

    @Inject
    private OfferAddSteps offerAddSteps;
    @Step("Ищем в har.log нужные параметры url: {matcherUrl}, количество: {expectedCount}")
    public void shouldSeeRequestInLog(Matcher<String> matcherUrl, Matcher<Integer> expectedCount) {
        watchException(() -> given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias(String.format("Должно быть «%s» запрсов с «%s»", expectedCount, matcherUrl))
                .pollInterval(1, SECONDS).atMost(30000, MILLISECONDS)
                .until(() -> proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                        .filter(e -> matcherUrl.matches(e.getRequest().getUrl()))
                        .filter(e -> (equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_OK)
                                || equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_MOVED_TEMPORARILY)))
                        .collect(Collectors.toList()).size(), expectedCount));
    }

    @Step("Ищем в har.log нужные параметры url: {matcherUrl}, query: {matcherQueryName}={matcherQueryValue}, " +
            "количество: {expectedCount}")
    public void shouldSeeRequestWithQueryInLog(Matcher<String> matcherUrl, Matcher matcherQueryName,
                                               Matcher matcherQueryValue, Matcher<Integer> expectedCount) {
        watchException(() -> given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(30000, MILLISECONDS)
                .until(() -> getFilteredEntriesByQuery(matcherUrl, matcherQueryName, matcherQueryValue)
                        .filter(e -> equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_OK))
                        .collect(Collectors.toList()).size(), expectedCount));
    }

    @Step("Ищем в har.log список нужных параметров для url: «{urlMatcher}» " +
            "фильтр по «{matcherQueryName}={matcherQueryValue}», содержит {harNameValuePairs}")
    public void shouldSeeRequestsWithQueriesInLog(Matcher urlMatcher, Matcher matcherQueryName, Matcher matcherQueryValue,
                                                  List<HarNameValuePair> harNameValuePairs) {
        watchException(() -> given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias(String.format("Не было запросов с «%s=%s» «%s» ",
                        matcherQueryName, matcherQueryValue, harNameValuePairs))
                .ignoreExceptions()
                .pollInterval(1, SECONDS).atMost(10000, MILLISECONDS)
                .untilAsserted(() -> assertThat(getFilteredEntriesByQuery(urlMatcher, matcherQueryName,
                        matcherQueryValue)
                        .map(e -> e.getRequest().getQueryString())
                        .collect(Collectors.toList()).stream().findFirst().get()).containsAll(harNameValuePairs)));
    }

    @Step("Не должно быть запросов для рекламных мест кроме заданных: {list}")
    public void shouldNotSeeQueryExcept(List<String> list) {
        watchException(() -> given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .await().pollDelay(10, SECONDS).atMost(15000, MILLISECONDS)
                .untilAsserted(() -> getFilteredEntriesByQuery(containsString(AN_YANDEX_RU_META), equalTo(IMP_ID),
                        anything())
                        .map(e -> e.getRequest().getQueryString().stream()
                                .filter(q -> q.getName().contains(IMP_ID))
                                .findFirst().get().getValue()).forEach(s -> assertThat(s).isIn(list))));
    }

    @Step("Чистим har")
    public void clearHar() {
        webDriverManager.getDriver().get("about:blank");
        proxyServerManager.getServer().clearBlacklist();
        proxyServerManager.getServer().newHar();
    }

    @Step("Чистим har пока не будет запросов")
    public void clearHarUntilThereAreNoHarEntries() {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .await().atMost(20000, MILLISECONDS)
                .until(() -> {
                    proxyServerManager.getServer().clearBlacklist();
                    proxyServerManager.getServer().newHar();
                    waitSomething(1, SECONDS);
                    return proxyServerManager.getServer().getHar().getLog().getEntries().isEmpty();
                });
    }

    public Stream<HarEntry> getFilteredEntriesByQuery(Matcher url, Matcher queryNameMatcher,
                                                      Matcher queryValueMatcher) {
        return proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                .filter(e -> url.matches(e.getRequest().getUrl()))
                .filter(e -> equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_OK))
                .filter(e -> e.getRequest().getQueryString().stream()
                        .anyMatch(q -> queryNameMatcher.matches(q.getName())
                                && queryValueMatcher.matches(q.getValue())));
    }

    @Step("Вводим номер телефона и подтверждаем его у пользователя с uid={0}")
    public String fillAndConfirmPhone() {
        proxyServerManager.getServer().clearBlacklist();
        proxyServerManager.getServer().newHar();
        getProxyServerManager().getServer().enableHarCaptureTypes(getAllContentCaptureTypes());
        String phone = Utils.getRandomPhone();
        offerAddSteps.onOfferAddPage().contactInfo().input(PHONE, removeStart(phone, "7"));
        offerAddSteps.onOfferAddPage().contactInfo().featureField(PHONE).button(CONFIRM).click();
        String trackId = getTrackId();
        String code = api.getConfirmationCode(trackId);
        offerAddSteps.onOfferAddPage().featureField(SMS_CODE).input().sendKeys(code);
        offerAddSteps.onOfferAddPage().featureField(SMS_CODE).button(CONFIRM).click();
        offerAddSteps.onOfferAddPage().phoneField().button("Добавить еще номер").should(isDisplayed());
        return phone;
    }

    public String getTrackId() {
        String json = given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions().await()
                .until(() -> getProxyServerManager().getServer().getHar().getLog().getEntries().stream()
                        .filter(e -> e.getRequest().getUrl().contains("management-new/gate/add/get_code/"))
                        .filter(e -> e.getResponse().getContent().getText().contains("trackId"))
                        .findFirst().get().getResponse().getContent().getText(), notNullValue());
        return new GsonBuilder().create().fromJson(json, CodeResponse.class).getResponse().getTrackId();
    }
}
