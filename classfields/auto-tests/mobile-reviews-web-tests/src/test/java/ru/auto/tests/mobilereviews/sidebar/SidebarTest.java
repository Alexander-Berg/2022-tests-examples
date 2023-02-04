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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.mobile.element.Sidebar.SIGNIN;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар под незарегом")
@Feature(SIDEBAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class SidebarTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).open();
        basePageSteps.onReviewsMainPage().header().sidebarButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Скрытие сайдбара")
    public void shouldCloseSidebar() {
        basePageSteps.onMainPage().sidebar().closeButton().click();

        basePageSteps.onMainPage().sidebar().waitUntil(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Войти»")
    public void shouldClickLoginUrl() {
        basePageSteps.onMainPage().sidebar().button(SIGNIN).click();

        urlSteps.fromUri(format("https://auth.%s/login/", urlSteps.getConfig().getBaseDomain()))
                .addParam("r", encode(format("https://%s/reviews/", urlSteps.getConfig().getBaseDomain())))
                .shouldNotSeeDiff();
        basePageSteps.onAuthPage().authForm().should(isDisplayed());
    }
}
