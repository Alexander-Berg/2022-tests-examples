package ru.auto.tests.mobilereviews.sidebar;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;

@DisplayName("Сайдбар - ссылка «Полная версия»")
@Feature(SIDEBAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SidebarFullVersionTest {

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

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).open();
        basePageSteps.onReviewsMainPage().header().sidebarButton().click();
    }

    @Ignore
    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка «Полная версия»")
    public void shouldClickDesktopUrl() {
        basePageSteps.onReviewsMainPage().sidebar().button("Полная версия").hover().click();
        urlSteps.fromUri(format("https://%s/reviews/?nomobile=", urlSteps.getConfig().getBaseDomain()))
                .shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue("nomobile", "null");
    }
}
