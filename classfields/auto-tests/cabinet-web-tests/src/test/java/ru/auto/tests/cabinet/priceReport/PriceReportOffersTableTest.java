package ru.auto.tests.cabinet.priceReport;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.PRICE_REPORT;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.dealerPriceGroup;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroups.dealerPriceGroups;
import static ru.auto.tests.desktop.mock.MockDealerPriceListing.dealerPriceListing;
import static ru.auto.tests.desktop.mock.MockDealerPriceOffer.dealerPriceOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_GROUPS;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_LISTING;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_PRICE_REPORT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.PRICE_REPORT)
@Story("Таблица офферов отчёта")
@DisplayName("Таблица офферов отчёта")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PriceReportOffersTableTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_PRICE_REPORT, DATE_IN_PAST);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup().setPreset("")
                                ).build()),

                stub().withGetDeepEquals(DEALER_REPORT_PRICE_LISTING)
                        .withResponseBody(
                                dealerPriceListing().setOffers(
                                        dealerPriceOffer(),
                                        dealerPriceOffer(),
                                        dealerPriceOffer()
                                ).build()),

                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/ApiAccessClientManagerRole"),
                stub("cabinet/CommonCustomerGetAgency")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PRICE_REPORT).addParam(CLIENT_ID, "21413").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Список офферов изначально скрыт")
    public void shouldSeeOffersInitialClosed() {
        steps.onPriceReportPage().tableRows().get(0).offersTable().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во офферов в отчете")
    public void shouldSeeOffersCount() {
        steps.onPriceReportPage().tableRows().get(0).click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().should(hasSize(3));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем таблицу с офферами")
    public void shouldCloseOffersTable() {
        steps.onPriceReportPage().tableRows().get(0).click();
        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().waitUntil(hasSize(3));
        steps.onPriceReportPage().tableRows().get(0).offersTable().closeIcon().click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таблица с офферами отображается для пресета «NO_DISCOUNTS»")
    public void shouldSeeOffersTableForNoDiscounts() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup().setPreset("NO_DISCOUNTS")
                                ).build()));

        urlSteps.refresh();

        steps.onPriceReportPage().tableRows().get(0).click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().should(hasSize(3));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таблица с офферами не отображается для пресета «ABSENT_OFFERS»")
    public void shouldSeeNoOffersTableForAbsentOffers() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup().setPreset("ABSENT_OFFERS")
                                ).build()));

        urlSteps.refresh();

        steps.onPriceReportPage().tableRows().get(0).click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().should(not(isDisplayed()));
    }

}
