package ru.yandex.realty.filters.map.commercial;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.utils.UtilsWeb.getNormalArea;

@DisplayName("Карта. Фильтры поиска по коммерческой недвижимости")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersAreaCommercialTest {

    private static final String AREA_FROM = "Площадь от";
    private static final String AREA_TO = "до";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь от»")
    public void shouldSeeAreaFromInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(OFIS).path(KARTA).open();
        String areaFrom = String.valueOf(getNormalArea());
        basePageSteps.onMapPage().filters().area().input(AREA_FROM).sendKeys(areaFrom + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("areaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь до»")
    public void shouldSeeAreaToInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(OFIS).path(KARTA).open();
        String areaFrom = String.valueOf(getNormalArea());
        basePageSteps.onMapPage().filters().area().input(AREA_TO).sendKeys(areaFrom + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("areaMax", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Link("https://st.yandex-team.ru/REALTYFRONT-9233")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaFromInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA)
                .path(KARTA).open();
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().filters().lotArea().input(AREA_FROM).sendKeys(areaFrom + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("lotAreaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Link("https://st.yandex-team.ru/REALTYFRONT-9233")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaToInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA)
                .path(KARTA).open();
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().filters().lotArea().input(AREA_TO).sendKeys(areaFrom + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("lotAreaMax", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

}