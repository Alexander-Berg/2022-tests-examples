package ru.auto.tests.mobile.footer;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FOOTER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;

@DisplayName("Футер - ссылка «Полная версия»")
@Feature(FOOTER)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterFullVersionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameter(1)
    public String desktopUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"%s/moskva/", "%s/moskva/?nomobile=true"},
                {"%s/moskva/motorcycle/all/?year_to=1890", "%s/moskva/motorcycle/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/scooters/all/?year_to=1890", "%s/moskva/scooters/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/atv/all/?year_to=1890", "%s/moskva/atv/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/snowmobile/all/?year_to=1890", "%s/moskva/snowmobile/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/lcv/all/?year_to=1890", "%s/moskva/lcv/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/truck/all/?year_to=1890", "%s/moskva/truck/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/artic/all/?year_to=1890", "%s/moskva/artic/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/bus/all/?year_to=1890", "%s/moskva/bus/all/?nomobile=true&year_to=1890"},
                {"%s/moskva/trailer/all/?year_to=1890", "%s/moskva/trailer/all/?nomobile=true&year_to=1890"},
                {"%s/catalog/cars/", "%s/catalog/cars/?nomobile=true"},
                {"%s/catalog/cars/marks/", "%s/moskva/cars/all/?nomobile=true"},
                {"%s/catalog/cars/ac/", "%s/catalog/cars/ac/?nomobile=true"},
                {"%s/catalog/cars/ac/models/", "%s/moskva/cars/ac/all/?nomobile=true"},
                {"%s/catalog/cars/ac/cobra/", "%s/catalog/cars/ac/cobra/?nomobile=true"},
                {"%s/catalog/cars/ac/cobra/20331876/20465600/",
                        "%s/catalog/cars/ac/cobra/20331876/20465600/?nomobile=true"},
                {"%s/moskva/cars/am_general/all/", "%s/moskva/cars/am_general/all/?nomobile=true"},
                {"%s/searches/", "%s/like/searches/?nomobile=true"},
                {"%s/moskva/dilery/cars/new/?dealer_org_type=1",
                        "%s/moskva/dilery/cars/new/?dealer_org_type=1&nomobile=true"},
                {"%s/moskva/dilery/cars/new/?official_dealer=false",
                        "%s/moskva/dilery/cars/new/?official_dealer=false&nomobile=true"},
                {"%s/moskva/dilery/cars/alfa_romeo/new/?mark-model-nameplate=ALFA_ROMEO&dealer_org_type=1",
                        "%s/moskva/dilery/cars/alfa_romeo/new/?dealer_org_type=1&nomobile=true"},
                {"%s/moskva/dilery/cars/alfa_romeo/new/?official_dealer=false&mark-model-nameplate=ALFA_ROMEO",
                        "%s/moskva/dilery/cars/alfa_romeo/new/?official_dealer=false&nomobile=true"},
                {"%s/diler-oficialniy/cars/all/avtograd_vladimir_kia/kia/?from=dealer-listing-list",
                        "%s/diler-oficialniy/cars/all/avtograd_vladimir_kia/kia/?from=dealer-listing-list&nomobile=true"}
        });
    }

    @Before
    public void before() {
        urlSteps.fromUri(format(url, urlSteps.getConfig().getTestingURI())).open();
        basePageSteps.onBasePage().footer().hover();
        waitSomething(3, TimeUnit.SECONDS);
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка «Полная версия»")
    public void shouldClickDesktopUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onBasePage().footer().button("Полная версия"));
        urlSteps.fromUri(format(desktopUrl, urlSteps.getConfig().getDesktopURI())).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue("nomobile", "1");
    }
}
