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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.page.MapPage.CHOOSE_LAYER;

@DisplayName("Карта. Общее. Тепловые карты")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HeatMapsTest {

    private static final String TRANSPORT_VALUE = "transport";

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
    @DisplayName("Видим кнопку и тайтл легенды тепловой карты при переходе по урлу")
    public void shouldSeeHeatMapByUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(UrlSteps.LAYER_URL_PARAM, TRANSPORT_VALUE).open();
        basePageSteps.onMapPage().mapButton("Транспортная доступность").should(isDisplayed());
        basePageSteps.onMapPage().heatMapLegendTitle().should(hasText("Доступность на общественном транспорте"));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скрываем тепловую карту")
    public void shouldSeePriceSellLayer() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(UrlSteps.LAYER_URL_PARAM, "price-sell").open();
        basePageSteps.onMapPage().mapButton("Цена продажи").waitUntil(isDisplayed());
        basePageSteps.onMapPage().heatMapCloser().click();
        basePageSteps.onMapPage().mapButton(CHOOSE_LAYER).should(isDisplayed());
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).shouldNotDiffWithWebDriverUrl();
    }
}
