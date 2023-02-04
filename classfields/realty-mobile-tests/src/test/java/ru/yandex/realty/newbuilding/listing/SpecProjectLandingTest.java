package ru.yandex.realty.newbuilding.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;

@DisplayName("Лендинг Самолета.")
@Feature(NEWBUILDING)
@Link("https://st.yandex-team.ru/VERTISTEST-1948")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SpecProjectLandingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(SAMOLET).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест")
    public void shouldSamoletScreenshotM() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingPage().pageBody());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
