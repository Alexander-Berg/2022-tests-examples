package ru.auto.tests.amp.sidebar;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар под незарегом")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class SidebarUnauthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "mobile/SearchCarsAll",
                "desktop/SessionUnauth").post();

        urlSteps.testing().path(AMP).path(CARS).path(ALL).open();
        basePageSteps.onListingPage().header().sidebarButton().should(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение сайдбара")
    public void shouldSeeSidebar() {
        basePageSteps.onListingPage().sidebar().waitUntil(isDisplayed());
    }
}