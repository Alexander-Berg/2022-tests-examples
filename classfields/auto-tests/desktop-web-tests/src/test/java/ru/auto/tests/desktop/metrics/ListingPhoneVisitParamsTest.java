package ru.auto.tests.desktop.metrics;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.QueryParams.AUTORU_BILLING_SERVICE_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.EXTENDED;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Метрики - параметры визитов - телефон в листинге")
@Feature(METRICS)
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingPhoneVisitParamsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public SeleniumMockSteps browserMockSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String visitParams;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String searchMock;

    @Parameterized.Parameter(4)
    public String phonesMock;

    @Parameterized.Parameters(name = "name = {index}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "{\"cars\":{\"listing\":{\"show-phone\":{\"client\":{\"phonePopup\":{\"new\":{}}}}}}}",
                        "desktop/SearchCarsBreadcrumbsEmpty", "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "{\"remarketing\":{\"cars\":{\"phoneview\":{\"mark\":{\"mercedes\":{\"seller\":{\"client\":{}},\"status\":{\"new\":{}}}},\"model\":{\"e-klasse\":{\"seller\":{\"client\":{}},\"status\":{\"new\":{}}}}}}}}",
                        "desktop/SearchCarsBreadcrumbsEmpty", "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "{\"__ym\":{\"ecommerce\":[{\"purchase\":{\"actionField\":{\"",
                        "desktop/SearchCarsBreadcrumbsEmpty", "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "revenue\":150},\"products\":[{\"id\":\"mercedes-e_klasse-1076842087\",\"name\":\"E-Класс\",\"price\":4498200,\"brand\":\"Mercedes-Benz\",\"category\":\"cars\"}]}}]}}",
                        "desktop/SearchCarsBreadcrumbsEmpty", "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},

                {TRUCK, "{\"trucks\":{\"listing\":{\"show-phone\":{\"client\":{\"phonePopup\":{\"used\":{}}}}}}}",
                        "desktop/SearchTrucksBreadcrumbs", "desktop/SearchTrucksExtended", "desktop/OfferTrucksPhones"},

                {MOTORCYCLE, "{\"MOTORCYCLE\":{\"listing\":{\"show-phone\":{\"client\":{\"phonePopup\":{\"used\":{}}}}}}}",
                        "desktop/SearchMotoBreadcrumbs", "desktop/SearchMotoExtended", "desktop/OfferMotoPhones"},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub(breadcrumbsMock),
                stub(searchMock),
                stub(phonesMock)
        ).create();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики")
    public void shouldSendMetrics() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL)
                .addParam(AUTORU_BILLING_SERVICE_TYPE, EXTENDED).open();

        basePageSteps.onListingPage().getSale(0).showPhonesButton().hover().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(visitParams)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики, тип листинга «Карусель»")
    public void shouldSendMetricsCarousel() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL)
                .addParam(AUTORU_BILLING_SERVICE_TYPE, EXTENDED).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).showPhonesButton().hover().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(visitParams)));
    }

}
