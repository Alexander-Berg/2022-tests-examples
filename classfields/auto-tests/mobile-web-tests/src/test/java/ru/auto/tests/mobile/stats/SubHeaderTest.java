package ru.auto.tests.mobile.stats;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Статистика - подшапка")
@Feature(AutoruFeatures.STATS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SubHeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String tabTitle;

    @Parameterized.Parameter(2)
    public String tabUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/audi/", "Объявления", "/moskva/cars/audi/all/"},
                {"/audi/", "Каталог", "/catalog/cars/audi/"},
                {"/audi/", "Видео", "/video/cars/audi/"},
                {"/audi/", "Статистика цен", "/stats/cars/audi/"},
                {"/audi/a3/", "Объявления", "/moskva/cars/audi/a3/all/"},
                {"/audi/a3/", "Каталог", "/catalog/cars/audi/a3/"},
                {"/audi/a3/", "Видео", "/video/cars/audi/a3/"},
                {"/audi/a3/", "Статистика цен", "/stats/cars/audi/a3/"},
                {"/audi/a3/21837610/", "Объявления", "/moskva/cars/audi/a3/21837610/all/"},
                {"/audi/a3/21837610/", "Каталог", "/catalog/cars/audi/a3/21837610/"},
                {"/audi/a3/21837610/", "Видео", "/video/cars/audi/a3/21837610/"},
                {"/audi/a3/21837610/", "Статистика цен", "/stats/cars/audi/a3/21837610/"},
                {"/audi/a3/21837610/21837711/", "Объявления", "/moskva/cars/audi/a3/21837610/21837711/all/"},
                {"/audi/a3/21837610/21837711/", "Каталог", "/catalog/cars/audi/a3/21837610/21837711/"},
                {"/audi/a3/21837610/21837711/", "Видео", "/video/cars/audi/a3/21837610/?configuration_id=21837711"},
                {"/audi/a3/21837610/21837711/", "Статистика цен", "/stats/cars/audi/a3/21837610/21837711/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(STATS).path(CARS).path(startUrl).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке в подшапке")
    public void shouldClickUrl() {
        basePageSteps.onStatsPage().subHeader().url(tabTitle).should(isDisplayed()).click();
        urlSteps.fromUri(format("https://%s%s", urlSteps.getConfig().getBaseDomain(), tabUrl)).shouldNotSeeDiff();
    }
}
