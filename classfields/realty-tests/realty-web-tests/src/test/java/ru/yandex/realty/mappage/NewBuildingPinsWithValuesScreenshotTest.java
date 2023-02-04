package ru.yandex.realty.mappage;

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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Скриншотный тест с ценами на пинах")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NewBuildingPinsWithValuesScreenshotTest {


    private static final String REGION_WITH_ALREADY_DISPLAYED_PINS_WITH_VALUES =
            "/ekaterinburg/";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот карты с пинами-ценниками")
    public void shouldSeeValuesPinsScreenshot() {
        compareSteps.resize(1920, 2000);
        urlSteps.testing().path(REGION_WITH_ALREADY_DISPLAYED_PINS_WITH_VALUES).path(KUPIT).path(NOVOSTROJKA)
                .path(KARTA).open();
        basePageSteps.onMapPage().hasPinsWithCost();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().map());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().map());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
