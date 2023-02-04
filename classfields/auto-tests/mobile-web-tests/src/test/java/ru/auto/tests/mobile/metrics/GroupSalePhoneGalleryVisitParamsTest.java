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
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Метрики - параметры визитов - телефон в галерее на групповой карточке c запиненным оффером")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GroupSalePhoneGalleryVisitParamsTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";

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
    public String visitParams;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"1", "{\"cars\":{\"card\":{\"show-phone\":{\"client\":{\"gallery\":{\"new\":{}}}}}}}"},
                {"2", "{\"remarketing\":{\"cars\":{\"phoneview\":{\"mark\":{\"kia\":{\"seller\":{\"client\":{}},\"status\":{\"new\":{}}}},\"model\":{\"optima\":{\"seller\":{\"client\":{}},\"status\":{\"new\":{}}}}}}}}"},
                {"3", "{\"__ym\":{\"ecommerce\":[{\"purchase\":{\"actionField\":{\""},
                {"4", "revenue\":150},\"products\":[{\"id\":\"kia-optima-1076842087\",\"name\":\"Optima\",\"price\":1474900,\"brand\":\"Kia\",\"category\":\"cars\"}]}}]}}"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsNewDealer"),
                stub("desktop/OfferCarsPhones")
        ).create();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Параметры визитов при клике на «Показать телефон» в галерее")
    public void shouldSendMetrics() {
        basePageSteps.onCardPage().gallery().getItem(0).click();
        basePageSteps.onCardPage().fullScreenGallery().callButton().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(visitParams)));
    }
}
