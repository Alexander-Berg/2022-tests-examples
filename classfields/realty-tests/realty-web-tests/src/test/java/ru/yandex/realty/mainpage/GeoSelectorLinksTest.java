package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.consts.Location;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.OMSK;
import static ru.yandex.realty.consts.Location.MOSCOW_AND_MO;
import static ru.yandex.realty.consts.Location.SPB_AND_LO;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.element.base.GeoSelectorPopup.RegionSelectorPopup.SAVE;

@DisplayName("Главная. Геоселектор.")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GeoSelectorLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public Location location;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {MOSCOW_AND_MO},
                {SPB_AND_LO}
        });
    }

    @Test
    @Owner(VICDEV)
    @Issue("REALTY-14518")
    public void shouldSeeChoosenLocation() {
        urlSteps.testing().path(OMSK).open();
        user.onBasePage().headerMain().regionSelector().click();
        user.onBasePage().regionSelectorPopup().breadCrumb("Россия").click();
        user.onBasePage().regionSelectorPopup().button(location.getName()).click();
        user.onBasePage().regionSelectorPopup().button(SAVE).click();
        urlSteps.testing().path(location.getPath()).shouldNotDiffWithWebDriverUrl();
    }


}
