package ru.yandex.realty.ipoteka.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;
import static ru.yandex.realty.element.base.GeoSelectorPopup.RegionSelectorPopup.SAVE;

/**
 * @author kantemirov
 */
@DisplayName("Страница Ипотеки. Меняем регион")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ChangeRegionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем регион проверяем что заголовок поменялся")
    public void shouldSeeNewRegion() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onBasePage().headerMain().regionSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onIpotekaCalculatorPage().regionSelectorPopup().input().waitUntil(isDisplayed())
                .sendKeys("Санкт-Петербург и ЛО");
        basePageSteps.onIpotekaCalculatorPage().regionSelectorPopup().suggestItem("Санкт-Петербург и ЛО")
                .waitUntil(isDisplayed()).click();
        basePageSteps.onIpotekaCalculatorPage().regionSelectorPopup().button(SAVE).click();
        basePageSteps.onIpotekaCalculatorPage().paranja().waitUntil(not(isDisplayed()));
        assertThat(basePageSteps.onIpotekaCalculatorPage().headerOffers().waitUntil(isDisplayed()).getText())
                .contains("в Санкт-Петербурге и ЛО");
    }
}