package ru.yandex.realty.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.PointStatisticSearchTemplate.pointStatisticSearchTemplate;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEWBUILDING_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.PRICE_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.SEARCH_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;

@DisplayName("Подписки. Переходы")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PassToSubscriptionPageTest {

    private static final String SUBSCRIPTION = "Подписки";
    private static final String SUBSCRIBE = "Подписаться";

    private String email;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        api.createVos2Account(account, AccountType.OWNER);
        email = api.getUserEmail(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подписываемся с карточки новостройки -> видим переход на страницу подписок на соответсвующий таб")
    public void shouldSeeNewbuildingPass() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMock().open();

        newBuildingSteps.onNewBuildingSitePage().subscriptionForm().button("Здорово! Хочу подписаться").click();
        newBuildingSteps.onNewBuildingSitePage().subscriptionForm().link(SUBSCRIPTION).click();
        api.shouldSeeLastSubscriptionByUid(account.getId()).isNotNull();
        newBuildingSteps.switchToNextTab();
        urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, NEWBUILDING_TAB_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подписываемся с листинга -> видим переход на страницу подписок на соответсвующий таб")
    public void shouldSeeSearchPass() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate()
                .offers(asList(mockOffer(SELL_APARTMENT))).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().subscriptionForm().button("Подписаться").click();
        basePageSteps.onOffersSearchPage().subscriptionSuccess().link(SUBSCRIPTION).click();
        api.shouldSeeLastSubscriptionByUid(account.getId()).isNotNull();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подписываемся с листинга без офферов -> видим переход на страницу подписок на соответсвующий таб")
    public void shouldSeeSearchWithoutOffersPass() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate()
                .offers(asList()).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().emptySerp().button(SUBSCRIBE).click();
        basePageSteps.onOffersSearchPage().subscriptionPopup().input().waitUntil(hasValue(email));
        basePageSteps.onOffersSearchPage().subscriptionPopup().button(SUBSCRIBE).clickWhile(not(isDisplayed()));
        basePageSteps.onOffersSearchPage().subscriptionPopup().link(SUBSCRIPTION).click();
        api.shouldSeeLastSubscriptionByUid(account.getId()).isNotNull();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подписываемся с листинга на изменение цены -> видим переход на страницу подписок на соответсвующий таб")
    public void shouldSeePricePass() {
        MockOffer offer = mockOffer(SELL_APARTMENT).setIncreasedPrice();
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().offer(FIRST).arrowPrice().hover();
        basePageSteps.onOffersSearchPage().priceSubscriptionPopup().input().should(hasValue(email));
        basePageSteps.onOffersSearchPage().priceSubscriptionPopup().button(SUBSCRIBE).click();
        basePageSteps.onOffersSearchPage().priceSubscriptionPopup().link(SUBSCRIPTION).click();

        api.shouldSeeLastSubscriptionByUid(account.getId()).isNotNull();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, PRICE_TAB_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подписываемся с карты на изменение цены -> видим переход на страницу подписок на соответсвующий таб")
    public void shouldSeeMapPricePass() {
        MockOffer offer = mockOffer(SELL_APARTMENT).setIncreasedPrice();
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .createWithDefaults();
        basePageSteps.resize(1920, 2000);
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).arrowPrice().hover();
        basePageSteps.onMapPage().priceSubscriptionPopup().input().should(hasValue(email));
        basePageSteps.onMapPage().priceSubscriptionPopup().button(SUBSCRIBE).click();
        basePageSteps.onMapPage().priceSubscriptionPopup().link(SUBSCRIPTION).click();

        api.shouldSeeLastSubscriptionByUid(account.getId()).isNotNull();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, PRICE_TAB_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
