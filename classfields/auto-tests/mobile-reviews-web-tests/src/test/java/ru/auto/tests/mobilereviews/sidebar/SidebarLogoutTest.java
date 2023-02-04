package ru.auto.tests.mobilereviews.sidebar;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.TestData.USER_2_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.mobile.element.Sidebar.SIGNIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар - выход")
@Feature(SIDEBAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SidebarLogoutTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private LoginSteps loginSteps;

    @Before
    public void before() throws IOException {
        urlSteps.testing().path(REVIEWS).open();
        loginSteps.loginAs(USER_2_PROVIDER.get());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке выхода")
    public void shouldClickLogoutButton() {
        basePageSteps.onMainPage().header().sidebarButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().sidebar().logoutButton().hover().click();
        basePageSteps.onMainPage().sidebar().waitUntil(not(isDisplayed()));

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().header().sidebarButton().click();
        basePageSteps.onMainPage().sidebar().button(SIGNIN).should(isDisplayed());
    }
}
