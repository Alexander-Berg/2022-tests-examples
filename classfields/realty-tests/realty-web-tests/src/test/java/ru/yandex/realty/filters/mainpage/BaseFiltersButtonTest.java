package ru.yandex.realty.filters.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KUPIT_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.NEWBUILDINGS_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.POSUTOCHO_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.SNYAT_BUTTON;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Базовые фильтры.")
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

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().deselectCheckBox("1");
        basePageSteps.onMainPage().filters().deselectCheckBox("2");
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Кнопка «купить»")
    public void shouldSeeBuyInUrl() {
        basePageSteps.onMainPage().filters().selectButton(SNYAT_BUTTON);
        basePageSteps.onMainPage().filters().selectButton(KUPIT_BUTTON);
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Кнопка «снять»")
    public void shouldSeeBuyRentInUrl() {
        basePageSteps.onMainPage().filters().selectButton(SNYAT_BUTTON);
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Кнопка «Посуточно»")
    public void shouldSeeShortRentInUrl() {
        basePageSteps.onMainPage().filters().selectButton(POSUTOCHO_BUTTON);
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA)
                .path("/posutochno/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Кнопка «Новостройки»")
    public void shouldSeeNewBuildingInUrl() {
        basePageSteps.onMainPage().filters().selectButton(NEWBUILDINGS_BUTTON);
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).queryParam("from", "index_nb_sites")
                .shouldNotDiffWithWebDriverUrl();
    }
}
