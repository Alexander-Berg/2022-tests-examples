package ru.auto.tests.mobile.metrics;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Метрики - параметры визитов - телефон в галерее")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SalePhoneGalleryVisitParamsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
    public String testNum;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameter(2)
    public String state;

    @Parameterized.Parameter(3)
    public String saleMock;

    @Parameterized.Parameter(4)
    public String visitParams;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"1", CARS, USED, "desktop/OfferCarsUsedUser", "{\"cars\":{\"card\":{\"show-phone\":{\"user\":{\"gallery\":{}}}}}}"},
                {"2", CARS, USED, "desktop/OfferCarsUsedUser", "{\"remarketing\":{\"cars\":{\"phoneview\":{\"mark\":{\"land-rover\":{\"seller\":{\"user\":{}},\"status\":{\"used\":{}}}},\"model\":{\"discovery\":{\"seller\":{\"user\":{}},\"status\":{\"used\":{}}}}}}}}"},
                {"3", CARS, USED, "desktop/OfferCarsUsedUser", "{\"__ym\":{\"ecommerce\":[{\"purchase\":{\"actionField\":{\""},
                {"4", CARS, USED, "desktop/OfferCarsUsedUser", "revenue\":50},\"products\":[{\"id\":\"land_rover-discovery-1076842087\",\"name\":\"Discovery\",\"price\":700000,\"brand\":\"Land Rover\",\"category\":\"cars\"}]}}]}}"},

                {"5", MOTORCYCLE, USED, "desktop/OfferMotoUsedUser", "{\"motorcycle\":{\"card\":{\"show-phone\":{\"user\":{\"gallery\":{}}}}}}"},

                {"6", TRUCK, USED, "desktop/OfferTrucksUsedUser", "{\"truck\":{\"card\":{\"show-phone\":{\"user\":{\"gallery\":{}}}}}}"},

                //Dealer
                {"7", CARS, USED, "desktop/OfferCarsUsedDealer", "{\"cars\":{\"card\":{\"show-phone\":{\"client\":{\"gallery\":{\"used\":{}}}}}}}"},
                {"8", CARS, USED, "desktop/OfferCarsUsedDealer", "{\"remarketing\":{\"cars\":{\"phoneview\":{\"mark\":{\"skoda\":{\"seller\":{\"client\":{}},\"status\":{\"used\":{}}}},\"model\":{\"fabia\":{\"seller\":{\"client\":{}},\"status\":{\"used\":{}}}}}}}}"},
                {"9", CARS, USED, "desktop/OfferCarsUsedDealer", "{\"__ym\":{\"ecommerce\":[{\"purchase\":{\"actionField\":{\""},
                {"10", CARS, USED, "desktop/OfferCarsUsedDealer", "revenue\":500},\"products\":[{\"id\":\"skoda-fabia-1076842087\",\"name\":\"Fabia\",\"price\":375000,\"brand\":\"Skoda\",\"category\":\"cars\"}]}}]}}"},

                {"11", MOTORCYCLE, USED, "desktop/OfferMotoUsedDealer", "{\"motorcycle\":{\"card\":{\"show-phone\":{\"client\":{\"gallery\":{\"used\":{}}}}}}}"},

                {"12", MOTORCYCLE, NEW, "desktop/OfferMotoNew", "{\"motorcycle\":{\"card\":{\"show-phone\":{\"client\":{\"gallery\":{\"new\":{}}}}}}}"},

                {"13", TRUCK, USED, "desktop/OfferTrucksUsedDealer", "{\"truck\":{\"card\":{\"show-phone\":{\"client\":{\"gallery\":{\"used\":{}}}}}}}"},

                {"14", TRUCK, NEW, "desktop/OfferTrucksNew", "{\"truck\":{\"card\":{\"show-phone\":{\"client\":{\"gallery\":{\"new\":{}}}}}}}"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub(saleMock),
                stub("desktop/OfferCarsPhones"),
                stub("desktop/OfferMotoPhones"),
                stub("desktop/OfferTrucksPhones")
        ).create();

        urlSteps.testing().path(category).path(state).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Параметры визитов при клике на «Показать телефон» в галерее")
    public void shouldSendGalleryVisitParams() {
        basePageSteps.onCardPage().gallery().getItem(0).click();
        basePageSteps.onCardPage().fullScreenGallery().callButton().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(visitParams)));
    }
}
