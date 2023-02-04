package ru.yandex.realty.mainpage;

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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Сслыки")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MainPageNewBuildingLinksNotChpuTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «До 4 квартала 2022»")
    public void shouldSeeTypeInUrl() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().mainBlockNewBuilding().link("До 4 квартала 2022").waitUntil(isDisplayed()).click();
        urlSteps.path(KUPIT).path(NOVOSTROJKA).path("/sdacha-2022/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Со скидками»")
    public void shouldSeeTypeInUrl1() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().mainBlockNewBuilding().link("Со скидками").waitUntil(isDisplayed()).click();
        urlSteps.path(KUPIT).path(NOVOSTROJKA).queryParam("hasSpecialProposal", "YES").shouldNotDiffWithWebDriverUrl();
    }
}
