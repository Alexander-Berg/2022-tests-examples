package ru.auto.tests.desktop.stats;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - отображение фильтра")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FilterScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameter(1)
    public String filterText;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/audi/a3/", "Audi\nA3\nПоколение\nКузов\nКомплектация"},
                {"/audi/a3/20785010/", "Audi\nA3\nIII (8V) Рестайлинг\nКузов\nКомплектация"},
                {"/audi/a3/20785010/20785079/", "Audi\nA3\nIII (8V) Рестайлинг\nХэтчбек 3 дв.\nКомплектация"},
                {"/audi/a3/20785010/20785079/20785079__20794251/",
                        "Audi\nA3\nIII (8V) Рестайлинг\nХэтчбек 3 дв.\n1.0 MT 115 л.c."}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(STATS).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Фильтр")
    public void shouldSeeFilter() {
        basePageSteps.onStatsPage().mmmFilter().should(hasText(filterText));
    }
}