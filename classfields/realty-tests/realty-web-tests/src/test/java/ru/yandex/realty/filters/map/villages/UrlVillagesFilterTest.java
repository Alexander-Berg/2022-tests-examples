package ru.yandex.realty.filters.map.villages;

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

@DisplayName("Карта. Фильтр поиска по коттеджным поселкам.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class UrlVillagesFilterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбран «Материал стен» при переходе по урлу")
    public void shouldSeeWallsTypeChecked() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .queryParam("wallsType", "BRICK").open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().button("Кирпич").should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбран «Коммуникации» при переходе по урлу")
    public void shouldSeeCommunicationChecked() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI)
                .path(KARTA).queryParam("hasWaterSupply", "YES").open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().button("Вода").should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбран «Срок сдачи» при переходе по урлу")
    public void shouldSeeDeliveryDateChecked() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .queryParam("deliveryDate", "4_2022").open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().button("До 4 квартала 2022").should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбран «Класс посёлка» при переходе по урлу")
    public void shouldSeeVillageClassChecked() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .queryParam("villageClass", "BUSINESS").open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().button("Бизнес").should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбран «Удаленность от Москвы» при переходе по урлу")
    public void shouldSeeDirectionDistanceMaxChecked() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .queryParam("directionDistanceMax", "100").open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().button("До 100 км от Москвы").should(isDisplayed());
    }
}
