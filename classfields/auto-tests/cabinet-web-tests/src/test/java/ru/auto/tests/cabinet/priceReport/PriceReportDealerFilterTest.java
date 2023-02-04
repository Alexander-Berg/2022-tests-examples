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
import static ru.auto.tests.desktop.consts.Regions.DEFAULT_RADIUS;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.element.cabinet.priceReport.FilterMenu.MARKET;
import static ru.auto.tests.desktop.mock.MockAutoruDealer.autoruDealer;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.dealerPriceGroup;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroups.dealerPriceGroups;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersMarkModels.markModelsBMW;
import static ru.auto.tests.desktop.mock.Paths.AUTORU_DEALER;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_GROUPS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_MARK_MODELS;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_PRICE_REPORT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.PRICE_REPORT)
@Story("Фильтр по дилеру")
@DisplayName("Фильтр по дилеру")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PriceReportDealerFilterTest {

    private static final String POPUP_TEXT = "Рынок\nСредняя стоимость автомобилей\nВсе дилеры BMW\nАВТОDОМ BMW\n" +
            "Москва, ул. Зорге, 17\nРОЛЬФ Премиум BMW\nМосква, 85-й километр МКАД, 2Ак1\nРОЛЬФ Премиум Химки BMW\n" +
            "Россия, Москва и Московская область, Химки, Ленинградское шоссе, вл21\nБалтАвтоТрейд-М BMW Рублёвка\n" +
            "Москва, Рублевское ш., 64, к. 2";

    private static final String FIRST_DEALER_NAME = "АВТОDОМ BMW";
    private static final String FIRST_DEALER_ID = "20135656";

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
                                        dealerPriceGroup(),
                                        dealerPriceGroup(),
                                        dealerPriceGroup()
                                ).build()),

                stub().withGetDeepEquals(USER_OFFERS_CARS_MARK_MODELS)
                        .withResponseBody(
                                markModelsBMW().getBody()),

                stub().withGetDeepEquals(AUTORU_DEALER)
                        .withResponseBody(
                                autoruDealer().getBody()),

                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/ApiAccessClientManagerRole"),
                stub("cabinet/CommonCustomerGetAgency")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PRICE_REPORT).addParam(CLIENT_ID, "21413")
                .addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа фильтрации по дилеру")
    public void shouldSeeDealerFilterPopupText() {
        steps.onPriceReportPage().dealerFilterButton().waitUntil(hasText(MARKET)).click();

        steps.onPriceReportPage().dealerFilterPopup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливаем фильтр по дилеру")
    public void shouldChangeDealerInFilterPopup() {
        steps.onPriceReportPage().tableRows().should(hasSize(3));
        steps.onPriceReportPage().dealerFilterButton().click();
        steps.onPriceReportPage().dealerFilterPopup().waitUntil(isDisplayed());

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withRequestQuery(
                                query().setGeoRadius(DEFAULT_RADIUS)
                                        .setGeoId(MOSCOW_GEO_ID)
                                        .setCompetitor(FIRST_DEALER_ID))
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                ).build()));

        steps.onPriceReportPage().dealerFilterPopup().dealers().get(0).click();

        steps.onPriceReportPage().dealerFilterPopup().should(not(isDisplayed()));
        steps.onPriceReportPage().dealerFilterButton().should(hasText(FIRST_DEALER_NAME));
        steps.onPriceReportPage().tableRows().should(hasSize(1));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтр по дилеру")
    public void shouldRemoveDealerInFilterPopup() {
        steps.onPriceReportPage().dealerFilterButton().click();
        steps.onPriceReportPage().dealerFilterPopup().waitUntil(isDisplayed());
        steps.onPriceReportPage().dealerFilterPopup().dealers().get(0).click();

        steps.onPriceReportPage().dealerFilterButton().waitUntil(hasText(FIRST_DEALER_NAME)).click();
        steps.onPriceReportPage().dealerFilterPopup().market().click();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                ).build()));

        steps.onPriceReportPage().dealerFilterPopup().should(not(isDisplayed()));
        steps.onPriceReportPage().dealerFilterButton().should(hasText(MARKET));
        steps.onPriceReportPage().tableRows().should(hasSize(1));
    }

}
