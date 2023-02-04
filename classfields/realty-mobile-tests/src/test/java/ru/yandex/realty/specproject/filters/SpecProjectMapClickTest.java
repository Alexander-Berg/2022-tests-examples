package ru.yandex.realty.specproject.filters;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Спецпроект. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SpecProjectMapClickTest {

    private static final String MAP = "/map/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на пин на карте")
    public void shouldSeeMapOfferClick() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(MAP).open();
        basePageSteps.moveCursorAndClick(
                basePageSteps.onSpecProjectPage().mapPins().waitUntil(hasSize(greaterThan(0))).get(FIRST));
        basePageSteps.onSpecProjectPage().mapOfferPopup().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path("/novoe-vnukovo-2466997/")
                .queryParam(UrlSteps.FROM_SPECIAL, UrlSteps.SAMOLET_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }
}
