package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Выбор гео-радиуса")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GeoRadiusCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String geoRadius;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"0"},
                {"100"},
                {"300"},
                {"500"},
                {"1000"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Установка гео-радиуса")
    public void shouldSetGeoRadius() {
        basePageSteps.onMainPage().filters().button("+ 200 км").click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onMainPage().geoRadiusPopup().geoRadius(format("%s км", geoRadius)).click();

        basePageSteps.onMainPage().geoRadiusPopup().waitUntil(not(isDisplayed()));
        urlSteps.ignoreParam("cookiesync").shouldNotSeeDiff();
        basePageSteps.onMainPage().filters().button(format("+ %s км", geoRadius)).waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue("gradius", geoRadius);
    }
}
