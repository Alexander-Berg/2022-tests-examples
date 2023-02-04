package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Листинг - сохранение параметров при навигации")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingSaveParamsMotoTrucksTest {

    private static final String PARAM = "price_to";
    private static final String PARAM_VALUE = "5000000";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String mark;

    @Parameterized.Parameter(2)
    public String model;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {MOTORCYCLE, "honda", "cbr_600_f"},
                {TRUCK, "hyundai", "hd78"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(mark).path(model).path(ALL)
                .addParam(PARAM, PARAM_VALUE).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сохранение параметров при навигации")
    public void shouldSaveParams() {
        basePageSteps.onListingPage().filters().section("С пробегом").click();
        urlSteps.testing().path(MOSKVA).path(category).path(mark).path(model).path(USED).addParam(PARAM, PARAM_VALUE)
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().filters().section("Новые").click();
        urlSteps.testing().path(MOSKVA).path(category).path(mark).path(model).path(NEW).addParam(PARAM, PARAM_VALUE)
                .shouldNotSeeDiff();
    }
}
