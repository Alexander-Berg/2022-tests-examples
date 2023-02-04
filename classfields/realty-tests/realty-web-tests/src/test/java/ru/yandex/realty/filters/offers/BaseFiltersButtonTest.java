package ru.yandex.realty.filters.offers;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KUPIT_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.POSUTOCHO_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.SNYAT_BUTTON;

@DisplayName("Базовые фильтры поиска по объявлениям")
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
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Снять»")
    public void shouldSeeBuyInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().filters().button(KUPIT_BUTTON).click();
        basePageSteps.onOffersSearchPage().filters().selectPopup().item(SNYAT_BUTTON).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Купить»")
    public void shouldSeeBuyRentInUrl() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().filters().button(SNYAT_BUTTON).click();
        basePageSteps.onOffersSearchPage().filters().selectPopup().item(KUPIT_BUTTON).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Кнопка «Посуточно»")
    public void shouldSeeShortRentInUrl() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().filters().button(SNYAT_BUTTON).click();
        basePageSteps.onOffersSearchPage().filters().selectPopup().item(POSUTOCHO_BUTTON).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).path("/posutochno/")
                .shouldNotDiffWithWebDriverUrl();
    }
}
