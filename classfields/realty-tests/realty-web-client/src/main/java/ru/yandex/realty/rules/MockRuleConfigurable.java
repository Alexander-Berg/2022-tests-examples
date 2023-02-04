package ru.yandex.realty.rules;

import com.google.inject.Inject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.qameta.allure.Step;
import lombok.Getter;
import org.junit.rules.ExternalResource;
import org.mbtest.javabank.Client;
import ru.auto.tests.commons.mountebank.fluent.ImposterBuilder;
import ru.auto.tests.commons.mountebank.http.responses.ModeType;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.realty.config.RealtyWebConfig;

import java.util.Optional;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOfferStatCallbackTemplate;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

public class MockRuleConfigurable extends ExternalResource {

    private static final String COOKIE_DOMAIN = ".yandex.ru";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final int OK_200 = 200;
    public static final String PATH_TO_MOCK_POINT_STATISTIC_SEARCH_TEMPLATE = "mock/pointStatisticSearchTemplate.json";
    public static final String PATH_TO_SITE_LIKE_SEARCH_TEMPLATE = "mock/siteLikeSearch.json";
    public static final String BLOG_POST_TEMPLATE = "mock/blogPost.json";
    public static final String PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE =
            "mock/offerWithSiteSearchCountTemplate.json";
    public static final String PATH_TO_SITE_OFFER_STAT = "mock/site/siteOfferStat.json";
    public static final String PATH_TO_SITE_PLAN_SEARCH = "mock/site/sitePlanSearch.json";
    public static final String NB_ID = "2002000";
    public static final String PATH_TO_FASTLINKS = "mock/fastLinksNg.json";
    public static final String PATH_TO_MORTGAGE_PROGRAM_CALCULATOR = "mock/mortgageProgramCalculator.json";
    public static final String PATH_TO_MORTGAGE_PROGRAM_SERACH = "mock/mortgageProgramSearch.json";
    public static final String PATH_LINKS_RAILWAY = "mock/linksRailway.json";

    @Getter
    private String port;

    @Inject
    public RealtyWebConfig config;

    @Inject
    public WebDriverSteps webDriverSteps;

    @Inject
    private ImposterBuilder imposterBuilder;

    @Override
    protected void after() {
        deleteMock();
    }

    /**
     * https://github.com/YandexClassifieds/vertis-mockritsa
     */

    @Step("Создаём мок")
    public MockRuleConfigurable create(String imposter) {
        HttpResponse<JsonNode> response;

        try {
            response = Unirest.post(config.getMockritsaURL() + "/imposters")
                    .body(imposter).asJson();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

        port = response.getBody().getObject().get("port").toString();
        return this;
    }

    @Step("Сеттим мокричную куку")
    public void setMockritsaCookie() {
        webDriverSteps.setCookie("mockritsa_imposter", port, COOKIE_DOMAIN);
    }

    @Step("Билдим импостер")
    public MockRuleConfigurable create() {
        create(imposterBuilder.build().toString());
        return this;
    }

    public void createWithDefaults() {
        withDefaults();
        create();
        setMockritsaCookie();
    }

    @Step("Добавляем мок для «card.json»")
    public MockRuleConfigurable cardStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/card.json").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для /2.0/offers/{offerId}/phones")
    public MockRuleConfigurable offerPhonesStub(String offerId, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/offers/%s/phones", offerId)).method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/offers/{offerId}/phones» - ответ 500")
    public MockRuleConfigurable offerPhonesStub500(String offerId) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/offers/%s/phones", offerId)).method(GET).end().end()
                .response().is()
                .statusCode(500).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/newbuilding/{id}/contacts»")
    public MockRuleConfigurable newBuildingContacts(String toResponse, int id) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/newbuilding/%s/contacts", id))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/newbuilding/{id}/contacts» - ответ 500")
    public MockRuleConfigurable newBuildingContactsStub500(int id) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/newbuilding/%s/contacts", id))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .statusCode(500).end().end().end();
        return this;
    }

    //ответ такой же как newBuildingContacts
    @Step("Добавляем мок для «/2.0/village/{id}/contacts»")
    public MockRuleConfigurable villageContactsStub(String toResponse, String id) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/village/%s/contacts", id))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/village/{id}/contacts» - ответ 500")
    public MockRuleConfigurable villageContactsStub500(String id) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/newbuilding/%s/contacts", id))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .statusCode(500).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/1.0/user/{userId}/offers»")
    public MockRuleConfigurable userOffersStub(String toResponse, String userId) {
        imposterBuilder.stub().predicate().equals().path(format("/1.0/user/%s/offers", userId)).method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }


    @Step("Добавляем мок для «siteWithOffersStat.json»")
    public MockRuleConfigurable siteWithOffersStatStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/1.0/siteWithOffersStat.json").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/1.0/newbuilding/siteLikeSearch»")
    public MockRuleConfigurable siteLikeSearchStub() {
        imposterBuilder.stub().predicate().equals().path("/1.0/newbuilding/siteLikeSearch").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(PATH_TO_SITE_LIKE_SEARCH_TEMPLATE))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/1.0/blog/posts»")
    public MockRuleConfigurable blogPostsStub() {
        imposterBuilder.stub().predicate().equals().path("/1.0/blog/posts").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(BLOG_POST_TEMPLATE))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «cardWithViews.json»")
    public MockRuleConfigurable cardWithViewsStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/1.0/cardWithViews.json").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/offer/{offerId}/similar»")
    public MockRuleConfigurable similarStub(String similarResponse, String offerId) {
        imposterBuilder.stub().predicate().equals().path(format("/1.0/offer/%s/similar", offerId)).method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(similarResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «offerWithSiteSearch.json»")
    public MockRuleConfigurable offerWithSiteSearchStub(String offerWithSiteSearchResponse) {
        imposterBuilder.stub().predicate().equals().path("/1.0/offerWithSiteSearch.json").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(offerWithSiteSearchResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «offerWithSiteSearch.json»")
    public MockRuleConfigurable offerWithSiteSearchCountStub(String offerWithSiteSearchResponse) {
        imposterBuilder.stub().predicate().equals().path("/1.0/offerWithSiteSearch.json").method(GET).end()
                .end().predicate().contains().query("countOnly", "true").end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(offerWithSiteSearchResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/developer/{developerId}»")
    public MockRuleConfigurable developerStub(String developerId, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/developer/%s", developerId)).method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «favorites.json»")
    public MockRuleConfigurable favoritesStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/1.0/favorites.json").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «getOffersByIdV15.json»")
    public MockRuleConfigurable getOffersByIdStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/1.0/getOffersByIdV15.json").method(POST).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/village/{id}/card»")
    public MockRuleConfigurable villageCardStub(String toResponse, String id) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/village/%s/card", id))
                .method(GET).end().end()
                .response()
                .is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/village/search»")
    public MockRuleConfigurable villageSearchStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/2.0/village/search").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    //ручка пинов на карте
    @Step("Добавляем мок для «pointStatisticSearch.json»")
    public MockRuleConfigurable pointStatisticSearchStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/1.0/pointStatisticSearch.json").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(PATH_TO_MOCK_POINT_STATISTIC_SEARCH_TEMPLATE))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/newbuilding/simplePointSearch»")
    public MockRuleConfigurable newbuildingSimplePointSearchStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/2.0/newbuilding/simplePointSearch").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/village/pointSearch»")
    public MockRuleConfigurable villagePointSearch(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/2.0/village/pointSearch").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/paid-report/user/uid:{user}»")
    public MockRuleConfigurable paidReportStub(String user, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/paid-report/user/uid:%s", user))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(toResponse))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/user/uid:{user}»")
    public MockRuleConfigurable getUserUid(String user, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/user/uid:%s", user))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/1.0/money/person/{uid}»")
    public MockRuleConfigurable getMoneyPerson(String uid, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/1.0/money/person/%s", uid))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/1.0/user/{uid}/feeds»")
    public MockRuleConfigurable getUserFeeds(String uid, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/1.0/user/%s/feeds", uid))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/site/{id}/offerStat»")
    public MockRuleConfigurable getSiteOfferStat(String id, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/site/%s/offerStat", id))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(toResponse))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/site/{id}/planSearch»")
    public MockRuleConfigurable getSitePlanSearch(String id, String toResponse) {
        imposterBuilder.stub().predicate().equals().path(format("/2.0/site/%s/planSearch", id))
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(toResponse))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/1.0/user/{uid}/feeds/{partnerId}/offers/export»")
    public MockRuleConfigurable getUserFeedsOffersExport(String uid, String partnerId, String toResponse) {
        imposterBuilder.stub().predicate().equals()
                .path(format("/1.0/user/%s/feeds/%s/offers/export", uid, partnerId))
                .method(GET).end().end()
                .response().is()
                .header(CONTENT_TYPE, "text/xml; charset=UTF-8")
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Мок для новостройки с планами фильтрами и 1 квартирой")
    public MockRuleConfigurable mockNewBuilding(String id) {
        return siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(parseInt(id)).build())
                .getSiteOfferStat(id, PATH_TO_SITE_OFFER_STAT)
                .getSitePlanSearch(id, PATH_TO_SITE_PLAN_SEARCH)
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(mockOffer(SELL_APARTMENT))).build());
    }

    @Step("Мок для новостройки обратным звонком")
    public MockRuleConfigurable mockNewBuildingCallback(String id) {
        return siteWithOffersStatStub(mockSiteWithOfferStatCallbackTemplate().setNewbuildingId(parseInt(id)).build())
                .getSiteOfferStat(id, PATH_TO_SITE_OFFER_STAT)
                .getSitePlanSearch(id, PATH_TO_SITE_PLAN_SEARCH)
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(mockOffer(SELL_APARTMENT))).build());
    }

    @Step("Мок для новостройки с планами фильтрами и 1 квартирой")
    public MockRuleConfigurable mockNewBuilding() {
        return mockNewBuilding(NB_ID);
    }

    @Step("Добавляем мок для «/2.0/fastlinks-ng»")
    public MockRuleConfigurable getFastLinksNgStub() {
        imposterBuilder.stub().predicate().contains().path("/2.0/fastlinks-ng")
                .method(POST).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(PATH_TO_FASTLINKS))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/mortgage/program/calculator»")
    public MockRuleConfigurable mortgageProgramCalculator() {
        imposterBuilder.stub().predicate().equals().path("/2.0/mortgage/program/calculator").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(PATH_TO_MORTGAGE_PROGRAM_CALCULATOR))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/mortgage/program/search»")
    public MockRuleConfigurable mortgageProgramSearch() {
        imposterBuilder.stub().predicate().equals().path("/2.0/mortgage/program/search").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(PATH_TO_MORTGAGE_PROGRAM_SERACH))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Добавляем мок для «/2.0/links/741965/railway/75512»")
    public MockRuleConfigurable linksRailway() {
        imposterBuilder.stub().predicate().equals().path("/2.0/links/741965/railway/75512").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(getResourceAsString(PATH_LINKS_RAILWAY))
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Проксируем все запросы с заголовком x-original-host:{from} -> {to}")
    public MockRuleConfigurable transparent(String from, String to) {
        imposterBuilder.stub().predicate().equals().header("x-original-host", from).end().end()
                .response().proxy().to(to).mode(ModeType.PROXY_TRANSPARENT).end().end().end();
        return this;
    }

    public MockRuleConfigurable withDefaults() {
        transparent("realty-searcher-api.vrts-slb.test.vertis.yandex.net",
                "http://realty-searcher-api.vrts-slb.test.vertis.yandex.net");
        transparent("realty-gateway-api.vrts-slb.test.vertis.yandex.net", "http://realty-gateway-api.vrts-slb.test.vertis.yandex.net");
        transparent("back-rt-01-sas.test.vertis.yandex.net", "http://back-rt-01-sas.test.vertis.yandex.net");
        return this;
    }

    public static String forResponse(String response) {
        return getResourceAsString(response);
    }

    @Step("Удаляем мок")
    private void deleteMock() {
        Client client = new Client(config.getMockritsaURL());
        Optional.ofNullable(port).ifPresent(port -> client.deleteImposter(parseInt(port)));
    }
}
