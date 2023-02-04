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
import ru.auto.tests.desktop.mock.MockDealerPriceGroup;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.PRICE_REPORT;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.ABSENT_OFFERS;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.NO_DISCOUNTS;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.UNIQUE_OFFERS;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.dealerPriceGroup;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroups.dealerPriceGroups;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_GROUPS;
import static ru.auto.tests.desktop.page.cabinet.CabinetPriceReportPage.CARS_THAT_COMPETITORS_DONT_HAVE;
import static ru.auto.tests.desktop.page.cabinet.CabinetPriceReportPage.CARS_THAT_MAY_NOT_HAVE_DISCOUNTS;
import static ru.auto.tests.desktop.page.cabinet.CabinetPriceReportPage.CARS_THAT_YOU_DONT_HAVE;
import static ru.auto.tests.desktop.page.cabinet.CabinetPriceReportPage.ORDINARY_OFFERS;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_PRICE_REPORT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.PRICE_REPORT)
@Story("Таблица отчетов")
@DisplayName("Таблица отчетов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PriceReportTableTest {

    private static final String BASE_PRICE_GROUP_TEXT = "BMW 2 серии F44\nСедан Gran Coupe\u2009/\u2009218i 1.5 AMT " +
            "(140 л.с.)\u2009/\u2009Бензин\u2009/\u2009роботизированная\u2009/\u2009передний\n218i M Sport\n2022\n1" +
            "\u2009/\u20090\n3 220 397\n3 220 498\n3 220 499\n1\u2009/\u20090\n3 230 397\n3 230 498\n3 230 499";

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
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/ApiAccessClientManagerRole"),
                stub("cabinet/CommonCustomerGetAgency")
        );

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PRICE_REPORT).addParam(CLIENT_ID, "21413");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отчёт без пресета, отображение")
    public void shouldSeeNoPreset() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        getBasePriceGroup().setPreset("")
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().should(hasSize(1));
        steps.onPriceReportPage().tableRows().get(0).should(hasText(BASE_PRICE_GROUP_TEXT));
        steps.onPriceReportPage().infoCell().should(hasText(ORDINARY_OFFERS));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тип пресета «UNIQUE_OFFERS», отображение")
    public void shouldSeeUniqueOffersPresetText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        getBasePriceGroup().setPreset("UNIQUE_OFFERS")
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().should(hasSize(1));
        steps.onPriceReportPage().tableRows().get(0).should(hasText(BASE_PRICE_GROUP_TEXT));
        steps.onPriceReportPage().infoCell().should(hasText(CARS_THAT_COMPETITORS_DONT_HAVE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тип пресета «ABSENT_OFFERS», отображение")
    public void shouldSeeAbsentOfferPresetText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        getBasePriceGroup().setPreset("ABSENT_OFFERS")
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().should(hasSize(1));
        steps.onPriceReportPage().tableRows().get(0).should(hasText(BASE_PRICE_GROUP_TEXT));
        steps.onPriceReportPage().infoCell().should(hasText(CARS_THAT_YOU_DONT_HAVE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тип пресета «NO_DISCOUNTS», отображение")
    public void shouldSeeNoDiscountsPresetText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        getBasePriceGroup().setPreset("NO_DISCOUNTS")
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().should(hasSize(1));
        steps.onPriceReportPage().tableRows().get(0).should(hasText(BASE_PRICE_GROUP_TEXT));
        steps.onPriceReportPage().infoCell().should(hasText(CARS_THAT_MAY_NOT_HAVE_DISCOUNTS));
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отчёты с разными пресетами, проверка кол-ва отображенных")
    public void shouldSeeSeveralReports() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        getBasePriceGroup().setPreset(""),
                                        getBasePriceGroup().setPreset(""),
                                        getBasePriceGroup().setPreset(UNIQUE_OFFERS),
                                        getBasePriceGroup().setPreset(NO_DISCOUNTS),
                                        getBasePriceGroup().setPreset(NO_DISCOUNTS),
                                        getBasePriceGroup().setPreset(ABSENT_OFFERS)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().should(hasSize(6));
    }

    private MockDealerPriceGroup getBasePriceGroup() {
        return dealerPriceGroup()
                .setMark("BMW")
                .setModel("2 серии")
                .setSuperGen("F44")
                .setConfiguration("Седан Gran Coupe")
                .setTechParam("218i 1.5 AMT (140 л.с.)")
                .setEngineType("GASOLINE")
                .setTransmission("ROBOT")
                .setDrive("FORWARD_CONTROL")
                .setComplectation("218i M Sport")
                .setDealerInStock(1)
                .setDealerOnOrder(0)
                .setDealerMinPriceDiscount(3220397)
                .setDealerMinPrice(3220498)
                .setDealerMaxPrice(3220499)
                .setCompetitorInStock(1)
                .setCompetitorOnOrder(0)
                .setCompetitorMinPriceDiscount(3230397)
                .setCompetitorMinPrice(3230498)
                .setCompetitorMaxPrice(3230499);
    }

}
