package ru.yandex.realty.listing.newbuilding;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;

@DisplayName("Листинг новостроек. Спецпроект")
@Feature(NEWBUILDING)
@Link("https://st.yandex-team.ru/VERTISTEST-1948")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SpecProjectTest {

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
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();
        basePageSteps.onNewBuildingPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот спецпроекта")
    public void shouldSeeSpecProjectScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingPage().specProject());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingPage().specProject());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
