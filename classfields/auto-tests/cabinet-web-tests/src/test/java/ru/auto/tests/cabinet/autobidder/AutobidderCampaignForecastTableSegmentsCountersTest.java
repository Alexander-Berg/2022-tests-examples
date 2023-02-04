package ru.auto.tests.cabinet.autobidder;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mock.beans.promoCampaign.MarketIndicator;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORECAST_TABLE;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.HIGH_ATTENTION;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.LOW_ATTENTION;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.MAXIMUM_ATTENTION;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.MIDDLE_ATTENTION;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.MINIMAL_ATTENTION;
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
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story(FORECAST_TABLE)
@DisplayName("Каунтер офферов в сегментах прогноза распределения объявлений")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutobidderCampaignForecastTableSegmentsCountersTest {

    private final String segmentCounter = String.valueOf(getRandomBetween(1, 50));

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

    @Parameterized.Parameter
    public String segmentName;

    @Parameterized.Parameter(1)
    public int segmentIndex;

    @Parameterized.Parameters(name = "name = {index}: Сегмент {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {MINIMAL_ATTENTION, 0},
                {LOW_ATTENTION, 1},
                {MIDDLE_ATTENTION, 2},
                {HIGH_ATTENTION, 3},
                {MAXIMUM_ATTENTION, 4},
        });
    }

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_AUTOBIDDER, DATE_IN_PAST);

        MarketIndicator marketIndicator = marketIndicator().setRandomOrderedSegments();
        marketIndicator.getOrderedSegments().get(segmentIndex).setOffersCount(segmentCounter);

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
                                campaign().setMarketIndicator(marketIndicator))),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE).getBody())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Каунтер офферов в сегментах прогноза распределения объявлений")
    public void shouldSeeOfferCounterInSegment() {
        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().attentionSegment(segmentName).counter().should(
                hasText(matchesRegex(format("%s объявлени[йяе]", segmentCounter))));
    }

}
