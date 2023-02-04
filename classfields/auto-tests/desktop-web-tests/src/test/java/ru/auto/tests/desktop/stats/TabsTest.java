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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@DisplayName("Статистика - вкладки")
@Feature(AutoruFeatures.STATS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String tabTitle;

    @Parameterized.Parameter(1)
    public String tabHref;

    @Parameterized.Parameter(2)
    public String tabUrl;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();
    }

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Объявления", "/moskva/cars/audi/a3/20785010/20785079/all/",
                        "/moskva/cars/audi/a3/20785010/20785079/all/"},
                {"Дилеры", "/moskva/dilery/cars/all/",
                        "/moskva/dilery/cars/all/"},
                {"Каталог", "/catalog/cars/audi/a3/20785010/20785079/",
                        "/catalog/cars/audi/a3/20785010/20785079/"},
                {"Отзывы", "/reviews/cars/audi/a3/20785010/",
                        "/reviews/cars/audi/a3/20785010/"},
                {"Видео", "/video/cars/audi/a3/20785010/",
                        "/video/cars/audi/a3/20785010/"}
        });
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Вкладки")
    public void shouldClickTab() {
        urlSteps.testing().path(STATS).path(CARS).path("/audi/a3/20785010/20785079/").open();
        basePageSteps.onStatsPage().subHeader().button(tabTitle).should(hasAttribute("href",
                urlSteps.testing().path(tabHref).toString())).click();
        urlSteps.testing().path(tabUrl).shouldNotSeeDiff();
    }
}