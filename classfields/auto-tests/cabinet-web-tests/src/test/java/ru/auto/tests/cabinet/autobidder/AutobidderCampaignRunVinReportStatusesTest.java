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
import ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.CAMPAIGN_ACTIVATED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.RUN_CAMPAIGN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Filters.CHECKS_BY_VIN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Row.ACTIVE_CAMPAIGN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Row.SUSPENDED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.PAUSED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getBaseCampaign;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.mockPromoCampaigns;
import static ru.auto.tests.desktop.mock.MockSearchCars.DEALER_AUCTION_CARS_OFFERS_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffers;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_OFFER;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_MARKET_INDICATOR;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID_ACTIVATE;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign.campaign;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Filters.filters;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.MarketIndicator.marketIndicator;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.PromoCampaign.promoCampaign;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.PromoCampaignParams.promoCampaignParams;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story("Запуск кампании")
@DisplayName("Запуск кампании со всеми типами проверки по VIN")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutobidderCampaignRunVinReportStatusesTest {

    private final Campaign campaign = getBaseCampaign();

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
    public String vinReportStatusName;

    @Parameterized.Parameter(1)
    public String vinReportStatus;

    @Parameterized.Parameters(name = "{index}: Проверка по VIN {0}")
    public static String[][] getParameters() {
        return new String[][]{
                {"Проверено", "CHECKED"},
                {"Серые отчёты", "GRAY_REPORTS"},
                {"Красные отчёты", "RED_REPORTS"},
                {"Нет отчёта", "NO_REPORTS"},
                {"С историей", "WITH_HISTORY"},
                {"Без истории", "WITHOUT_HISTORY"},
                {"С расхождением пробега", "WITH_MISMATCHED_MILEAGE"}
        };
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
                stub("cabinet/UserOffersCarsMarkModelsUsed").withPredicateType(DEEP_EQUALS)
                        .withRequestQuery(
                                query().setSection("used").setCategory("CARS")),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        campaign.setStatus(PAUSED)).getBody()),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE).getBody()),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_MARKET_INDICATOR)
                        .withResponseBody(getJsonObject(
                                campaign().setMarketIndicator(marketIndicator())))
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запускаем кампанию со всеми типами проверки по VIN")
    public void shouldRunCampaignWithDifferentVinReportStatuses() {
        campaign.setIsPristine(false)
                .setFilters(
                        filters().setVinReportStatuses(asList(vinReportStatus)))
                .setDescription(vinReportStatusName);

        steps.onAutobidderPage().rows().get(0).status().waitUntil(hasText(SUSPENDED));
        steps.onAutobidderPage().rows().get(0).click();

        mockRule.setStubs(
                stub().withPutDeepEquals(format(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID, campaign.getId()))
                        .withRequestBody(getJsonObject(
                                promoCampaignParams().setPromoCampaignParams(campaign)))
                        .withResponseBody(getJsonObject(
                                promoCampaign().setCampaign(campaign))),

                stub().withPutDeepEquals(format(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID_ACTIVATE, campaign.getId()))
                        .withStatusSuccessResponse()
        ).update();

        steps.onAutobidderPage().extendedRow().budgetBlock().hover();
        steps.onAutobidderPage().extendedRow().filters().selectItem(CHECKS_BY_VIN, vinReportStatusName);

        steps.onAutobidderPage().extendedRow().button(RUN_CAMPAIGN).click();

        steps.onAutobidderPage().notifier(CAMPAIGN_ACTIVATED).should(isDisplayed());
        steps.onAutobidderPage().rows().get(0).status().should(hasText(ACTIVE_CAMPAIGN));
    }

}
