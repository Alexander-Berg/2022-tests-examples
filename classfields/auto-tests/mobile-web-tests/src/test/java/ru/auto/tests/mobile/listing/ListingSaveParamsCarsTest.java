package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Листинг - сохранение параметров при навигации")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingSaveParamsCarsTest {

    private static final String MARK = "audi";
    private static final String MODEL = "80";
    private static final String GENERATION = "7892649";
    private static final String PARAM = "price_from";
    private static final String PARAM_VALUE = "1000000";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(ALL)
                .addParam(PARAM, PARAM_VALUE).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сохранение параметров при навигации")
    public void shouldSaveParams() {
        basePageSteps.onListingPage().filters().section("С пробегом").click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(USED)
                .addParam(PARAM, PARAM_VALUE).shouldNotSeeDiff();

        basePageSteps.onListingPage().filters().section("Новые").click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW)
                .addParam(PARAM, PARAM_VALUE).shouldNotSeeDiff();
    }
}
