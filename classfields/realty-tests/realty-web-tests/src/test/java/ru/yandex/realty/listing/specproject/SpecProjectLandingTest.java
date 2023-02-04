package ru.yandex.realty.listing.specproject;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;

@DisplayName("Лендинг Самолета.")
@Feature(NEWBUILDING)
@Link("https://st.yandex-team.ru/VERTISTEST-1948")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
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

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест")
    public void shouldSamoletScreenshot() {
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(SAMOLET).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onSamoletPage().pageBody());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onSamoletPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход из спецпроектного запина в листинге ЖК")
    public void shouldSeePassFromNewBuildingListing() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).open();
        basePageSteps.onNewBuildingPage().specProject().snippetElements()
                .waitUntil(hasSize(greaterThan(0))).get(0).link().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.fromUri("https://realty.test.vertis.yandex.ru/moskva_i_moskovskaya_oblast/kupit/novostrojka/" +
                "sputnik-386079/?from-special=samolet").shouldNotDiffWithWebDriverUrl();
    }
}
