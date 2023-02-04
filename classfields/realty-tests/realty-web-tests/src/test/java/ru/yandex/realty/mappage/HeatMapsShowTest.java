package ru.yandex.realty.mappage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Общее. Тепловые карты")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HeatMapsShowTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA_I_MO);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем тепловую карту «Цена продажи»")
    public void shouldSeePriceSellLayer() {
        urlSteps.path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().selectHeatMap("Цена продажи");
        urlSteps.queryParam(UrlSteps.LAYER_URL_PARAM, "price-sell");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем тепловую карту «Цена аренды»")
    public void shouldSeePriceRentLayer() {
        urlSteps.path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMapPage().selectHeatMap("Цена аренды");
        urlSteps.queryParam(UrlSteps.LAYER_URL_PARAM, "price-rent");
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем тепловую карту «Прогноз окупаемости»")
    public void shouldSeeProfitabilityLayer() {
        urlSteps.path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().selectHeatMap("Прогноз окупаемости");
        urlSteps.queryParam(UrlSteps.LAYER_URL_PARAM, "profitability");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем тепловую карту «Школы и их рейтинг»")
    public void shouldSeeEducationLayer() {
        urlSteps.path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().selectHeatMap("Школы и их рейтинг");
        urlSteps.queryParam(UrlSteps.LAYER_URL_PARAM, "education");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем тепловую карту «Транспортная доступность»")
    public void shouldSeeTransportLayer() {
        urlSteps.path(SNYAT).path(GARAZH).path(KARTA).open();
        basePageSteps.onMapPage().selectHeatMap("Транспортная доступность");
        urlSteps.queryParam(UrlSteps.LAYER_URL_PARAM, "transport");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем тепловую карту «Инфраструктура»")
    public void shouldSeeInfrastructureLayer() {
        urlSteps.path(KUPIT).path(COMMERCIAL).path(KARTA).open();
        basePageSteps.onMapPage().selectHeatMap("Инфраструктура");
        urlSteps.queryParam(UrlSteps.LAYER_URL_PARAM, "infrastructure");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем тепловую карту «Доступность Яндекс.Драйва»")
    public void shouldSeeCarsharingLayer() {
        urlSteps.path(SNYAT).path(KOMNATA).path(KARTA).queryParam("rentTime", "SHORT").open();
        basePageSteps.onMapPage().selectHeatMap("Доступность Яндекс.Драйва");
        urlSteps.queryParam(UrlSteps.LAYER_URL_PARAM, "carsharing");
    }
}
