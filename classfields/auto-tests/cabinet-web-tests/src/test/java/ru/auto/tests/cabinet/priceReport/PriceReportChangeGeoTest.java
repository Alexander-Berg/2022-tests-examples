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
import static ru.auto.tests.desktop.consts.Regions.MOSCOW;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.consts.Regions.SPB;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.PRICE_REPORT)
@Story("Гео-попап")
@DisplayName("Гео-попап")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PriceReportChangeGeoTest {

    private static final String NEW_RADIUS = "500";


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
                        .withRequestQuery(
                                query().setGeoRadius(DEFAULT_RADIUS)
                                        .setGeoId(MOSCOW_GEO_ID))
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
                stub("cabinet/CommonCustomerGetAgency"),
                stub("desktop/GeoSuggestSpb")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PRICE_REPORT).addParam(CLIENT_ID, "21413")
                .addXRealIP(MOSCOW_IP).open();
        steps.onPriceReportPage().tableRows().waitUntil(hasSize(3));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем город в гео-селекторе, проверяем подгрузку отчетов")
    public void shouldChangeCity() {
        steps.onPriceReportPage().geoSelectButton().click();
        steps.onPriceReportPage().geoSelectPopup().selectedRegion(MOSCOW).click();
        steps.onPriceReportPage().geoSelectPopup().regionInput().sendKeys(SPB);
        steps.onPriceReportPage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withRequestQuery(
                                query().setGeoRadius(DEFAULT_RADIUS)
                                        .setGeoId(SPB_GEO_ID))
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                ).build()));

        steps.onPriceReportPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        steps.onPriceReportPage().geoSelectPopup().waitUntil(not(isDisplayed()));

        steps.onPriceReportPage().tableRows().should(hasSize(1));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем радиус в гео-селекторе, проверяем подгрузку отчетов")
    public void shouldChangeRadius() {
        steps.onPriceReportPage().geoSelectButton().click();
        steps.onPriceReportPage().geoSelectPopup().radiusButton(NEW_RADIUS).click();


        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withRequestQuery(
                                query().setGeoRadius(NEW_RADIUS)
                                        .setGeoId(MOSCOW_GEO_ID))
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                ).build()));

        steps.onPriceReportPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        steps.onPriceReportPage().geoSelectPopup().waitUntil(not(isDisplayed()));

        steps.onPriceReportPage().tableRows().should(hasSize(1));
    }

}
