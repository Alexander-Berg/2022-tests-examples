package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;

@DisplayName("Метрики")
@Epic(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class MetricsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
        pofferSteps.onPofferPage().popup().closeIcon().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики")
    public void shouldSeeMetrics() {
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"SHOW\":{}}}}");
        pofferSteps.selectMark();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"MARK\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectModel();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"MODEL\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectYear();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"YEAR\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectBodyType();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"BODY_TYPE\":{\"SUCCESS\":{\"COUPE\":{}}}}}}}");
        pofferSteps.selectGeneration();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"SUPER_GEN\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectEngineType();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"ENGINE_TYPE\":{\"SUCCESS\":{\"GASOLINE\":{}}}}}}}");
        pofferSteps.selectGearType();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"GEAR_TYPE\":{\"SUCCESS\":{\"FORWARD_CONTROL\":{}}}}}}}");
        pofferSteps.selectTransmission();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"TRANSMISSION\":{\"SUCCESS\":{\"ROBOT\":{}}}}}}}");
        pofferSteps.selectTechParam();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"TECH_PARAM\":{\"SUCCESS\":{}}}}}}");
        pofferSteps.selectColor();
        shouldSeePostData("{\"M_FORM_ADD_NEW\":{\"CARS\":{\"TECH\":{\"COLOR\":{\"SUCCESS\":{}}}}}}");
    }

    @Step("Проверяем метрики")
    private void shouldSeePostData(String postData) {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(postData)));
    }

}
