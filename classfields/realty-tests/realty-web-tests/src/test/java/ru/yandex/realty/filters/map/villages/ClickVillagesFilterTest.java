package ru.yandex.realty.filters.map.villages;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
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
import ru.yandex.realty.utils.RealtyUtils;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Фильтр поиска по коттеджным поселкам.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ClickVillagesFilterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «На карте» в основных фильтрах")
    public void shouldSeeOnMapClick() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onMapPage().filters().link("На карте").click();
        urlSteps.path(KARTA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «На карте» в дополнительных фильтрах")
    public void shouldSeeExtraOnMapClick() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().link("Показать на карте").click();
        urlSteps.path(KARTA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Description("ПОИСК ЗАДВАИВАЕТСЯ ПОЧЕМУ-ТО")
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем «Мои поиски» в дополнительных фильтрах")
    public void shouldSeeMySearchesClick() {
        int priceMin = RealtyUtils.getRandomPrice();
        basePageSteps.addSearchHistoryItem(priceMin);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onMapPage().mySearches().click();
        basePageSteps.onMapPage().searchHistoryList().should(hasSize(1));
    }
}
