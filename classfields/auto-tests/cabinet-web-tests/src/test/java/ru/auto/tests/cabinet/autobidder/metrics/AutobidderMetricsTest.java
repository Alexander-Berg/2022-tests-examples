package ru.auto.tests.cabinet.autobidder.metrics;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.DAYS_FROM;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.MAX_CALL_PRICE;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.TARGETED_CALLS_PER_DAY;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.TO;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneCabinetMetricsRequest;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.ACTIVE;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getBaseCampaign;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.mockPromoCampaigns;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story(METRICS)
@DisplayName("Метрики при вводе значений в инпуты формы кампании")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AutobidderMetricsTest {

    private static final String DAYS_WITHOUT_CALLS_FROM = "{\"auction-used-autobidder\":{\"campaign\":{\"days_without_calls-from\":{}}}}";
    private static final String DAYS_WITHOUT_CALLS_TO = "{\"auction-used-autobidder\":{\"campaign\":{\"days_without_calls-to\":{}}}}";
    private static final String MAX_POSITION_FOR_PRICE = "{\"auction-used-autobidder\":{\"campaign\":{\"max_position_for_price\":{}}}}";
    private static final String MAX_OFFER_DAILY_CALLS = "{\"auction-used-autobidder\":{\"campaign\":{\"max_offer_daily_calls\":{}}}}";

    private final String randomValue = String.valueOf(getRandomBetween(1, 50));

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
                                        getBaseCampaign().setStatus(ACTIVE)).getBody())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER).open();

        steps.onAutobidderPage().rows().get(0).click();
        waitSomething(1, TimeUnit.SECONDS);
        steps.onAutobidderPage().extendedRow().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("«days_without_calls-from» при вводе в инпут «От» блока «Нет целевых звонков»")
    public void shouldSeeDaysWithoutCallsFromMetric() {
        steps.onAutobidderPage().extendedRow().noTargetedCallsBlock().input(DAYS_FROM, randomValue);

        browserMockSteps.assertWithWaiting(onlyOneCabinetMetricsRequest(hasSiteInfo(DAYS_WITHOUT_CALLS_FROM)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("«days_without_calls-to» при вводе в инпут «До» блока «Нет целевых звонков»")
    public void shouldSeeDaysWithoutCallsToMetric() {
        steps.onAutobidderPage().extendedRow().noTargetedCallsBlock().input(TO, randomValue);

        browserMockSteps.assertWithWaiting(onlyOneCabinetMetricsRequest(hasSiteInfo(DAYS_WITHOUT_CALLS_TO)));
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("«max_position_for_price» при вводе в инпут «Макс. стоимость звонка»")
    public void shouldSeeMaxPositionForPriceMetric() {
        steps.onAutobidderPage().extendedRow().budgetBlock().input(MAX_CALL_PRICE, randomValue);

        browserMockSteps.assertWithWaiting(onlyOneCabinetMetricsRequest(hasSiteInfo(MAX_POSITION_FOR_PRICE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("«max_offer_daily_calls» при вводе в инпут «Целевых звонков в день на объявление»")
    public void shouldMaxOfferDailyCallsMetric() {
        steps.onAutobidderPage().extendedRow().budgetBlock().input(TARGETED_CALLS_PER_DAY, randomValue);

        browserMockSteps.assertWithWaiting(onlyOneCabinetMetricsRequest(hasSiteInfo(MAX_OFFER_DAILY_CALLS)));
    }

}
