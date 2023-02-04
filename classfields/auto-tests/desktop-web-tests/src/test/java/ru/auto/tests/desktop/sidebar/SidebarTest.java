package ru.auto.tests.desktop.sidebar;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар")
@Feature(SIDEBAR)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SidebarTest {

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
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/cars/all/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAll").post();

        urlSteps.fromUri(format("%s%s", urlSteps.getConfig().getTestingURI(), url)).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение/скрытие сайдбара")
    @Owner(DSVICHIHIN)
    public void shouldShowAndHideSidebar() {
        basePageSteps.onBasePage().sidebar().should(not(isDisplayed()));
        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, 768);
        basePageSteps.onBasePage().sidebar().waitUntil(isDisplayed());
        basePageSteps.setWindowSize(1024, 768);
        basePageSteps.onBasePage().sidebar().waitUntil(not(isDisplayed()));
    }
}