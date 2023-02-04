package ru.yandex.general.step;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.yandex.general.consts.BaseConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.BaseConstants.ClientType.DESKTOP;
import static ru.yandex.general.consts.BaseConstants.ClientType.TOUCH;
import static ru.yandex.general.consts.BaseConstants.ListingType.GRID;
import static ru.yandex.general.consts.BaseConstants.ListingType.LIST;
import static ru.yandex.general.matchers.HarEntryMatchers.hasPageRef;
import static ru.yandex.general.matchers.HarEntryMatchers.hasQueryString;
import static ru.yandex.general.matchers.HarEntryMatchers.hasRequestParamValue;

public class GoalsSteps {

    private static final String PAGE_URL = "page-url";

    private String[] pathsToBeIgnored = {};

    private int count;

    private Object body;

    private List<Matcher> harEntryMatchers = new ArrayList<>();

    public static List<Integer> IMP_DESKTOP_GRID_IDS = Arrays.asList(2, 6, 7, 8, 9);
    public static List<Integer> IMP_DESKTOP_LIST_IDS = Arrays.asList(11, 12, 13, 14, 15);
    public static List<Integer> IMP_TOUCH_GRID_IDS = Arrays.asList(2, 4, 5, 6, 7);
    public static List<Integer> IMP_TOUCH_LIST_IDS = Arrays.asList(8, 9, 10, 11, 12);

    @Inject
    @Getter
    private ProxyServerManager proxyServerManager;

    @Inject
    private UrlSteps urlSteps;

    public GoalsSteps withCurrentPageRef() {
        withPageRef(urlSteps.getCurrentUrl());
        return this;
    }

    @Step("С page-ref = «{pageRef}»")
    public GoalsSteps withPageRef(String pageRef) {
        harEntryMatchers.add(hasPageRef(pageRef));
        return this;
    }

    @Step("Цель типа = «{goalType}»")
    public GoalsSteps withGoalType(String goalType) {
        withPageUrl(format("goal://%s/%s", urlSteps.getBaseTestingUrl(), goalType));
        return this;
    }

    public GoalsSteps withCount(int count) {
        this.count = count;
        return this;
    }

    public GoalsSteps withPathsToBeIgnored(String... pathsToBeIgnored) {
        this.pathsToBeIgnored = pathsToBeIgnored;
        return this;
    }

    @Step("С page-url = «{pageUrl}»")
    public GoalsSteps withPageUrl(String pageUrl) {
        harEntryMatchers.add(hasQueryString(hasItem(param(PAGE_URL, pageUrl))));
        return this;
    }

    public GoalsSteps withEcommerceDetail() {
        harEntryMatchers.add(allOf(hasRequestParamValue(containsString("ecommerce")),
                hasRequestParamValue(containsString("detail"))));
        return this;
    }

    public GoalsSteps withEcommercePurchase() {
        harEntryMatchers.add(allOf(hasRequestParamValue(containsString("ecommerce")),
                hasRequestParamValue(containsString("purchase"))));
        return this;
    }

    @Step("С body = «{body}»")
    public GoalsSteps withBody(Object body) {
        harEntryMatchers.add(hasRequestParamValue(notNullValue()));
        this.body = body;
        return this;
    }

    public GoalsSteps shouldExist() {
        if (body == null)
            waitForRequest(harEntryMatchers, is(count));
        else {
            waitForRequest(harEntryMatchers, is(count)).forEach(harEntry -> {
                String requestText = harEntry.getRequest().getPostData()
                        .getParams().get(0).getValue();
                assertThatJson(requestText).whenIgnoringPaths(pathsToBeIgnored).isEqualTo(body.toString());
            });

        }
        return this;
    }

    private static Matcher<String> containsMetricsRequest() {
        return containsString("https://mc.yandex.ru/watch/72600925");
    }

    private static Matcher<String> containsDesktopAdsRequest() {
        return containsString("https://an.yandex.ru/meta/732240");
    }

    private static Matcher<String> containsTouchAdsRequest() {
        return containsString("https://an.yandex.ru/meta/732244");
    }

    @Step("Ждём запрос")
    private List<HarEntry> waitForRequest(List<Matcher> matchers, Matcher<Integer> expectedCount) {
        StringBuilder matchersAlias = new StringBuilder();
        matchers.forEach(matcher -> matchersAlias.append(matcher.toString() + "\n"));

        await().pollDelay(3, SECONDS).atMost(10, SECONDS).pollInterval(3, SECONDS)
                .alias(matchersAlias.toString() + "Количество: " + expectedCount)
                .until(() -> getHarEntries(containsMetricsRequest()).stream()
                        .filter(harEntry -> matchers.stream().allMatch(matcher -> matcher.matches(harEntry)))
                        .collect(toList()).size(), expectedCount);
//        getHarEntries(containsMetricsRequest()).stream().forEach(harEntry -> System.out.println(new Gson().toJson(harEntry)));

        return getHarEntries(containsMetricsRequest()).stream()
                .filter(harEntry -> matchers.stream().allMatch(matcher -> matcher.matches(harEntry))).collect(toList());
    }

    private List<HarEntry> getHarEntries(Matcher<String> urlMatcher) {
//        proxyServerManager.getServer().getHar().getLog().getEntries().stream().forEach(harEntry -> System.out.println(new Gson().toJson(harEntry)));
        return proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                .filter(harEntry -> urlMatcher.matches(harEntry.getRequest().getUrl()))
                .filter(harEntry -> equalTo(harEntry.getResponse().getStatus()).matches(200))
                .collect(toList());
    }

    public static HarNameValuePair param(String n, String v) {
        return new HarNameValuePair(n, v);
    }

    @Step("Очищаем HAR диаграмму")
    public GoalsSteps clearHar() {
        proxyServerManager.getServer().newHar();
        return this;
    }

    @Step("Проверяем отправку imp-id при каждом запросе рекламного сниппета")
    public GoalsSteps shouldSeeAdBannerRotation(BaseConstants.ClientType clientType, BaseConstants.ListingType listingType) {
        boolean gotDubles = false;
        List<Integer> actualImpIds = new ArrayList<>();
        List<Integer> approvedImpIds = new ArrayList<>();
        Matcher<String> urlMatcher = containsDesktopAdsRequest();

        if (clientType == TOUCH) urlMatcher = containsTouchAdsRequest();

        if (clientType == DESKTOP && listingType == GRID)
            approvedImpIds = IMP_DESKTOP_GRID_IDS;
        else if (clientType == DESKTOP && listingType == LIST)
            approvedImpIds = IMP_DESKTOP_LIST_IDS;
        else if (clientType == TOUCH && listingType == GRID)
            approvedImpIds = IMP_TOUCH_GRID_IDS;
        else if (clientType == TOUCH && listingType == LIST)
            approvedImpIds = IMP_TOUCH_LIST_IDS;


        getHarEntries(urlMatcher).stream().forEach(harEntry -> actualImpIds.add(Integer.valueOf(
                harEntry.getRequest().getQueryString().stream()
                        .filter(harNameValuePair -> harNameValuePair.getName().equals("imp-id"))
                        .collect(toList()).get(0).getValue())));

        for (int i = 1; i < actualImpIds.size(); i++) {
            if (actualImpIds.get(i) == actualImpIds.get(i - 1))
                gotDubles = true;
        }

        assertThat("impId баннеров чередуются", gotDubles, is(false));
        assertThat("Все impId баннеров из разрешенного списка", approvedImpIds.containsAll(actualImpIds), is(true));
        assertThat("Все impId из разрешенного списка задействованы", actualImpIds.containsAll(approvedImpIds), is(true));
        return this;
    }

}
