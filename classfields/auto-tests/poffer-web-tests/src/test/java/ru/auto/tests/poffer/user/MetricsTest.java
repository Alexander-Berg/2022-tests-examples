package ru.auto.tests.poffer.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.is;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;

@DisplayName("Частник - метрики")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class MetricsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики")
    public void shouldSeeMetrics() {
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"SHOW\":{}}}}");
        pofferSteps.selectMark();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"MARK\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectModel();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"MODEL\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectYear();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"YEAR\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectBodyType();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"BODY_TYPE\":{\"SUCCESS\":{\"COUPE\":{}}}}}}}");
        pofferSteps.selectGeneration();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"SUPER_GEN\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectEngineType();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"ENGINE_TYPE\":{\"SUCCESS\":{\"GASOLINE\":{}}}}}}}");
        pofferSteps.selectGearType();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"GEAR_TYPE\":{\"SUCCESS\":{\"FORWARD_CONTROL\":{}}}}}}}");
        pofferSteps.selectTransmission();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"TRANSMISSION\":{\"SUCCESS\":{\"ROBOT\":{}}}}}}}");
        pofferSteps.selectTechParam();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"TECH_PARAM\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectColor();
        shouldSeePostData("{\"FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"COLOR\":{\"SUCCESS\":{}}}}}}");
    }

    @Step("Проверяем метрики")
    private void shouldSeePostData(String postData) {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(postData)));
    }
}
