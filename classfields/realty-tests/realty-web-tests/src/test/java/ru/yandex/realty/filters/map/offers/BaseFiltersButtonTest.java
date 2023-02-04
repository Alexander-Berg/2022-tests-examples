package ru.yandex.realty.filters.map.offers;

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

import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KUPIT_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.POSUTOCHO_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.SNYAT_BUTTON;

@DisplayName("Карта. Базовые фильтры поиска по объявлениям")
@Feature(MAINFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersButtonTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Снять»")
    public void shouldSeeBuyInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().button(KUPIT_BUTTON).click();
        basePageSteps.onMapPage().filters().selectPopup().item(SNYAT_BUTTON).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Купить»")
    public void shouldSeeBuyRentInUrl() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().button(SNYAT_BUTTON).click();
        basePageSteps.onMapPage().filters().selectPopup().item(KUPIT_BUTTON).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Посуточно»")
    public void shouldSeeShortRentInUrl() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().button(SNYAT_BUTTON).click();
        basePageSteps.onMapPage().filters().selectPopup().item(POSUTOCHO_BUTTON).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).path(KARTA).queryParam("rentTime", "SHORT")
                .shouldNotDiffWithWebDriverUrl();
    }
}
