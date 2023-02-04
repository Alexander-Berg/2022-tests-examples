package ru.auto.tests.cabinet.autobidder.metrics;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.ATTENTION_INFO_COLUMN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.DAYS_IN_WAREHOUSE_COLUMN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.DAYS_WITHOUT_CALLS_COLUMN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.DEFICIT_COLUMN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.FORECAST_COLUMN;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneCabinetMetricsRequest;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story(METRICS)
@DisplayName("Метрики тултипов")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutobidderForecastTableTooltipsMetricsTest {

    private static final String TOOLTIP_METRIC = "{\"auction-used-autobidder\":{\"sales\":{\"%s\":{}}}}";

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

    @Inject
    private SeleniumMockSteps browserMockSteps;

    @Parameterized.Parameter
    public String columnName;

    @Parameterized.Parameter(1)
    public String metricName;

    @Parameterized.Parameters(name = "name = {index}: Столбец {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {DAYS_IN_WAREHOUSE_COLUMN, "days"},
                {DAYS_WITHOUT_CALLS_COLUMN, "target_calls_days"},
                {ATTENTION_INFO_COLUMN, "interest"},
                {FORECAST_COLUMN, "bid_forecast"},
                {DEFICIT_COLUMN, "deficit"},
        });
    }

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
                                campaign().setMarketIndicator(marketIndicator()))),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE).getBody())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER).open();

        waitSomething(1, TimeUnit.SECONDS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрики на показ тултипов по ховеру иконки вопроса в названии столбца таблицы")
    public void shouldSeeTooltipMetrics() {
        steps.onAutobidderPage().rows().get(0).click();
        steps.onAutobidderPage().extendedRow().snippets().get(0).waitUntil(isDisplayed()).hover();
        steps.onAutobidderPage().extendedRow().headerColumnTooltipIcon(columnName).hover();

        browserMockSteps.assertWithWaiting(onlyOneCabinetMetricsRequest(hasSiteInfo(format(TOOLTIP_METRIC, metricName))));
    }

}
