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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Фильтры. Скриншот")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapFiltersScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот фильтров")
    public void shouldSeeFiltersScreenshot() {
        urlSteps.open();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().filters());
        urlSteps.setProductionHost().open();

        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().filters());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот фильтров с длинной строкой")
    public void shouldSeeFiltersLongLineScreenshot() {
        urlSteps.queryParam("houseType", "HOUSE")
                .queryParam("houseType", "PARTHOUSE")
                .queryParam("houseType", "TOWNHOUSE")
                .queryParam("houseType", "DUPLEX").open();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().filters());
        urlSteps.setProductionHost().open();

        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().filters());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
