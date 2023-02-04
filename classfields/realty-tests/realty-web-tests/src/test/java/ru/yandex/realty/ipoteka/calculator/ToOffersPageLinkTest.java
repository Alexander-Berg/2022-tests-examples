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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.MOSCOW_RGID;

/**
 * @author kantemirov
 */
@DisplayName("Страница Ипотеки. Переходим на страницу офферов")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ToOffersPageLinkTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по офферу на карте предложений")
    public void shouldSeePassToOffer() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).queryParam(UrlSteps.RGID, MOSCOW_RGID).open();
        basePageSteps.moveCursorAndClick(
                basePageSteps.onIpotekaCalculatorPage().placemarks().waitUntil(hasSize(greaterThan(0))).get(FIRST));
        basePageSteps.onIpotekaCalculatorPage().balloon().link().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToTab(1);
        urlSteps.shouldCurrentUrlContains(urlSteps.testing().path(OFFER).path("/").toString());
    }
}