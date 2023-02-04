package ru.auto.tests.cabinet.autobidder;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.ACTIVE;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getBaseCampaign;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.mockPromoCampaigns;
import static ru.auto.tests.desktop.mock.MockSearchCars.DEALER_AUCTION_CARS_OFFERS_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffers;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_OFFER;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_MARKET_INDICATOR;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign.campaign;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.MarketIndicator.marketIndicator;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.formatNumber;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story("Снипет оффера в таблице «Прогноз распределения»")
@DisplayName("Снипет оффера в таблице «Прогноз распределения»")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AutobidderCampaignForecastSnippetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_AUTOBIDDER, DATE_IN_PAST);

        mockRule.setStubs(
                stub("cabinet/SessionDirectDealerAristos"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGetClientAristos"),
                stub("cabinet/DesktopClientsGetAristos"),
                stub("cabinet/DealerTariff/AllTariffs"),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        getBaseCampaign().setStatus(ACTIVE)).getBody()),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_MARKET_INDICATOR)
                        .withResponseBody(getJsonObject(
                                campaign().setMarketIndicator(marketIndicator())))
        );

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Цена»")
    public void shouldSeeSnippetPrice() {
        int price = getRandomBetween(100000, 500000);

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setRurPrice(price).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).price().should(hasText(
                formatPrice(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Название»")
    public void shouldSeeSnippetTitle() {
        String markName = getRandomString();
        String modelName = getRandomString();
        String genName = getRandomString();
        int year = getRandomBetween(2010, 2015);

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setMarkName(markName)
                                        .setModelName(modelName)
                                        .setSuperGenName(genName)
                                        .setYear(year)
                                        .getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).title().should(hasText(
                format("%s %s %s, %d", markName, modelName, genName, year)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Пробег»")
    public void shouldSeeSnippetRun() {
        int run = getRandomBetween(10000, 150000);

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setMileage(run).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).run().should(hasText(
                format("%s км", formatNumber(run))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Модификация»")
    public void shouldSeeSnippetModification() {
        String techParam = getRandomString();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setTechparamNameplate(techParam).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).modification().should(hasText(
                techParam));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Дней на складе»")
    public void shouldSeeSnippetDaysInStock() {
        int daysInStock = getRandomShortInt();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setDaysInStock(daysInStock).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).daysInStock().should(hasText(
                String.valueOf(daysInStock)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Дней без звонков»")
    public void shouldSeeSnippetDaysWithoutCalls() {
        int daysWithoutCalls = getRandomShortInt();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setDaysWithoutCalls(daysWithoutCalls).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).daysWithoutCalls().should(hasText(
                String.valueOf(daysWithoutCalls)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Прогноз ставки»")
    public void shouldSeeSnippetForecast() {
        int bidForecast = getRandomShortInt();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setBidForecast(bidForecast * 100).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).forecast().should(hasText(
                formatPrice(bidForecast)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле «Не хватает до макс. интереса»")
    public void shouldSeeSnippetDeficit() {
        int deficit = getRandomShortInt();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setMaxInterestBidDiff(deficit * 100).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().snippets().get(0).deficit().should(hasText(
                formatPrice(deficit)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по снипету на карточку")
    public void shouldGoToOffer() {
        String id = getRandomOfferId();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE)
                                        .setId(id).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).click();
        steps.onAutobidderPage().extendedRow().snippets().get(0).title().click();

        steps.switchToNextTab();
        urlSteps.autoruDomain().path(CARS).path(USED).path(SALE).path("ford").path("ecosport")
                .path(id).path(SLASH).shouldNotSeeDiff();
    }

}
