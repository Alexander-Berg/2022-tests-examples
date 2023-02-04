package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import net.javacrumbs.jsonunit.core.Option;
import net.lightbody.bmp.core.har.HarEntry;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.browsermob.ProxyServerManager;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class AjaxProxySteps {

    public static final String SET_MARKETING_CAMPAIGNS_SETTINGS = "setMarketingCampaignsSettings";
    public static final String SET_CHAT_SETTINGS = "setChatSettings";
    public static final String GET_FEED_ERRORS = "getFeedErrors";
    public static final String GET_FEED_TASKS = "getFeedTasks";
    public static final String CARD_HIDE_OFFER = "cardHideOffer";
    public static final String CARD_DELETE_OFFER = "cardDeleteOffer";
    public static final String CARD_ACTIVATE_OFFER = "cardActivateOffer";
    public static final String HIDE_OFFERS = "hideOffers";
    public static final String DELETE_OFFERS = "deleteOffers";
    public static final String ACTIVATE_OFFERS = "activateOffers";
    public static final String UPDATE_USER = "updateUser";
    public static final String GET_USER_ACTIONS_STATISTICS = "getUserActionsStatistics";
    public static final String CREATE_COMPLAINT = "createComplaint";
    public static final String ROTATE_IMAGE = "rotateImage";
    public static final String ADD_TO_FAVORITES = "addToFavorites";
    public static final String DELETE_FROM_FAVORITES = "deleteFromFavorites";
    public static final String UPDATE_DRAFT = "updateDraft";
    public static final String GET_HOMEPAGE = "getHomepage";
    public static final String GET_OFFER_LISTING_SERVICE = "getOfferListingService";

    private Matcher<String> urlMatcher = containsString("/ajax/");
    private String handlerName;
    private Object expectedRequestText;
    private List<HarEntry> actualHarEntries;
    private int requestCount = 1;
    private String[] pathsToBeIgnored = {};


    @Inject
    @Getter
    private ProxyServerManager proxyServerManager;

    @Step("Ждём запрос")
    public AjaxProxySteps shouldExist() {
        await().pollDelay(3, SECONDS).atMost(10, SECONDS).pollInterval(3, SECONDS)
                .until(() -> getHarEntries().size(), greaterThan(0));
        actualHarEntries = getHarEntries();
        assertThat(format("Проверяем кол-во запросов к ручке «%s» = «%d»", handlerName, requestCount),
                actualHarEntries.size(), equalTo(requestCount));

        HarEntry lastHarEntry = actualHarEntries.stream()
                .sorted((a, b) ->
                        Long.compare(Long.valueOf(b.getRequest().getHeaders().stream().filter(header -> header.getName().equals("x-client-date")).findFirst().get().getValue()),
                                Long.valueOf(a.getRequest().getHeaders().stream().filter(header -> header.getName().equals("x-client-date")).findFirst().get().getValue())))
                .collect(Collectors.toList())
                .get(0);

        String actualRequestText = lastHarEntry.getRequest().getPostData().getText();
        assertThatJson(actualRequestText).when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(pathsToBeIgnored)
                .isEqualTo(expectedRequestText.toString());
        return this;
    }

    @Step("Отсутствует запрос")
    public AjaxProxySteps shouldNotExist() {
        await().pollDelay(3, SECONDS).atMost(10, SECONDS).pollInterval(3, SECONDS)
                .until(() -> getHarEntries().size(), is(0));
        return this;
    }

    private List<HarEntry> getHarEntries() {
//        proxyServerManager.getServer().getHar().getLog().getEntries().stream().forEach(harEntry -> System.out.println(new Gson().toJson(harEntry)));
        return proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                .filter(harEntry -> urlMatcher.matches(harEntry.getRequest().getUrl()))
                .filter(harEntry -> equalTo(harEntry.getResponse().getStatus()).matches(200))
                .collect(toList());
    }

    @Step("C запросом от ручки = «{handlerName}»")
    public AjaxProxySteps setAjaxHandler(String handlerName) {
        this.handlerName = handlerName;
        this.urlMatcher = containsString(format("/ajax/%s/", handlerName));
        return this;
    }

    @Step("С requestText = «{expectedRequestText}»")
    public AjaxProxySteps withRequestText(Object expectedRequestText) {
        this.expectedRequestText = expectedRequestText;
        return this;
    }

    @Step("С количеством запросов = «{requestCount}»")
    public AjaxProxySteps withRequestCount(int requestCount) {
        this.requestCount = requestCount;
        return this;
    }

    public AjaxProxySteps withPathsToBeIgnored(String... pathsToBeIgnored) {
        this.pathsToBeIgnored = pathsToBeIgnored;
        return this;
    }

    @Step("Очищаем HAR диаграмму")
    public AjaxProxySteps clearHar() {
        proxyServerManager.getServer().newHar();
        return this;
    }

}
