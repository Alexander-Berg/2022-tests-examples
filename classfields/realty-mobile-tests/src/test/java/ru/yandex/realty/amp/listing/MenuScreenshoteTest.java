package ru.yandex.realty.amp.listing;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;

@Link("VERTISTEST-1618")
@Feature(AMP_FEATURE)
@DisplayName("amp. Скриншот меню")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class MenuScreenshoteTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот меню")
    public void shouldSeeClosedMenuAmp() {
        compareSteps.resize(450, 2000);
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().ampMenu());
        urlSteps.setProductionHost().open();
        basePageSteps.onBasePage().menuButton().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().ampMenu());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
