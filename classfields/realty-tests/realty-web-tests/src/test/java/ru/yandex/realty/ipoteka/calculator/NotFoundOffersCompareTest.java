package ru.yandex.realty.ipoteka.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;

/**
 * @author kantemirov
 */
@DisplayName("Страница Ипотеки. Проверяем что нет предлоджений")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NotFoundOffersCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Сравниваем блок «Нет предложений»")
    public void shouldSeeNotFoundOffersBlock() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).queryParam("propertyCost", "100").open();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(
                basePageSteps.onIpotekaCalculatorPage().notFoundOffersBlock().waitUntil(isDisplayed()));
        basePageSteps.removePrestableCookie();

        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(
                basePageSteps.onIpotekaCalculatorPage().notFoundOffersBlock().waitUntil(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}