package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.consts.Location;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Location.MOSCOW;
import static ru.yandex.realty.consts.Location.MOSCOW_AND_MO;
import static ru.yandex.realty.consts.Location.SPB;
import static ru.yandex.realty.consts.Location.SPB_AND_LO;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;


@DisplayName("Главная. Геоселектор. Скриншоты. Локация")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GeoSelectorCompareLocationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public Location location;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {SPB},
                {MOSCOW},
                {MOSCOW_AND_MO},
                {SPB_AND_LO}
        });
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Production.class})
    @Owner(VICDEV)
    public void compareGeoSelectorPopupTest() {
        urlSteps.testing().path(location.getPath()).open();
        user.onBasePage().headerMain().regionSelector().click();
        user.moveCursorAndClick(user.onBasePage().regionSelectorPopup().regionName());
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(user.onBasePage().regionSelectorPopup());
        user.removePrestableCookie();

        urlSteps.production().path(location.getPath()).open();
        user.onBasePage().headerMain().regionSelector().click();
        user.moveCursorAndClick(user.onBasePage().regionSelectorPopup().regionName());
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(user.onBasePage().regionSelectorPopup());

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

}
