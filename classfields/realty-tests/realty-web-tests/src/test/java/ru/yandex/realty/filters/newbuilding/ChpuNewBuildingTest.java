package ru.yandex.realty.filters.newbuilding;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.HIGHWAY;
import static ru.yandex.realty.element.saleads.WithNewBuildingFilters.ADDRESS_INPUT;


@DisplayName("Фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ChpuNewBuildingTest {

    public static final String TEST_DIRECTION = "Новорижское шоссе";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).open();
        basePageSteps.resize(1920, 3000);
        basePageSteps.onNewBuildingPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем 2 параметра: Новостройки + открытая парковка + военная ипотека")
    public void shouldSee2ParamsNewBuildingInUrl() {
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingPage().extendFilters().checkBox("Открытая"));
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Открытая");
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Военная ипотека");
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/s-parkovkoy-i-voennaya-ipoteka/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Шоссе + 2 параметра: Новостройки + горьковское шоссе + Кирпичный + Военная ипотека")
    public void shouldSee2ParamsWithDirectionNewBuildingInUrl() {
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingPage().extendFilters().checkBox("Монолитный"));
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Монолитный");
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Военная ипотека");
        basePageSteps.onNewBuildingPage().closeExtFilter();
        basePageSteps.onNewBuildingPage().filters().geoButtons().spanLink(HIGHWAY).click();
        basePageSteps.onBasePage().geoSelectorPopup().selectCheckBox(TEST_DIRECTION);
        basePageSteps.onBasePage().geoSelectorPopup().submitButton().click();
        urlSteps.path("monolit-i-voennaya-ipoteka/").queryParam("direction", "20").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("метро окская + 2 параметра: Новостройки + метро окская + бизнесс класс + кирпич")
    public void shouldSee2ParamsWithMetroNewBuildingInUrl() {
        basePageSteps.onNewBuildingPage().extendFilters().input(ADDRESS_INPUT, "метро Некрасовка");
        basePageSteps.onNewBuildingPage().extendFilters().suggest().get(0).click();
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingPage().extendFilters().checkBox("Кирпичный"));
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Кирпичный");
        basePageSteps.onNewBuildingPage().extendFilters().select("Класс жилья", "Бизнес");
        basePageSteps.onNewBuildingPage().extendFilters().reduceFiltersButton().click();
        urlSteps.path("/metro-nekrasovka/").path("/kirpich-i-zhk-biznes/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Много параметров которые не формируют ЧПУ: Новостройки + Кирпичный + Скидки + Подземная парковка")
    public void shouldSee3ParamsNewBuildingInUrl() {
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingPage().extendFilters().checkBox("Кирпичный"));
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Кирпичный");
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Подземная");
        basePageSteps.onNewBuildingPage().extendFilters().selectCheckBox("Скидки");
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("buildingType", "BRICK").queryParam("parkingType", "UNDERGROUND")
                .queryParam("hasSpecialProposal", "YES").shouldNotDiffWithWebDriverUrl();
    }
}
