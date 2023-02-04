package ru.yandex.realty.filters.map.villages;

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
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.DISTRICT;
import static ru.yandex.realty.element.saleads.FiltersBlock.HIGHWAY;

@DisplayName("Карта. Фильтр поиска по коттеджным поселкам.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class GeoVillagesFilterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по названию")
    public void shouldSeeVillageId() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().filters().input("Адрес, посёлок").sendKeys("Дорино");
        basePageSteps.onMapPage().filters().suggest().get(0).click();
        urlSteps.queryParam("villageId", "1774859").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по урлу с названием коттеджного поселка")
    public void shouldSeeVillageBadge() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .queryParam("villageId", "1774859").open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges("Дорино").should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по шоссе")
    public void shouldSeeHighway() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().filters().geoButtons().spanLink(HIGHWAY).click();
        basePageSteps.onMapPage().geoSelectorPopup().tab(HIGHWAY).click();
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox("Ярославское шоссе");
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.onMapPage().filters().submitButton().click();
        urlSteps.queryParam("directionCode", "yaroslavskoe").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим бэйдж с шоссе")
    public void shouldSeeHighwayBadge() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .queryParam("directionCode", "yaroslavskoe").open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges("Ярославское шоссе").should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по двум шоссе")
    public void shouldSeeTwoHighways() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().filters().geoButtons().spanLink(HIGHWAY).click();
        basePageSteps.onMapPage().geoSelectorPopup().tab(HIGHWAY).click();
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox("Ярославское шоссе");
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox("Осташковское шоссе");
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        urlSteps.queryParam("direction", "1").queryParam("direction", "2").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск району")
    public void shouldSeeSubLocality() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().filters().geoButtons().spanLink(DISTRICT).click();
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox("Всеволожский");
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        urlSteps.queryParam("subLocality", "407445").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск району")
    public void shouldSeeSubLocalityBadge() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .queryParam("subLocality", "407445").open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges("Всеволожский").should(isDisplayed());
    }
}
