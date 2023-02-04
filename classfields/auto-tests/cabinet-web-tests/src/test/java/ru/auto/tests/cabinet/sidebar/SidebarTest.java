package ru.auto.tests.cabinet.sidebar;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Боковое меню")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SidebarTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DesktopSidebarGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Боковое меню в развернутом состоянии")
    public void shouldSeeUnfoldedSidebar() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().sidebar().waitUntil(isDisplayed()));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().sidebar().waitUntil(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Боковое меню в свернутом состоянии")
    public void shouldSeeCollapsedSidebar() {
        steps.collapseSidebar();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetOffersPage().sidebar());

        cookieSteps.deleteCookie("is_sidebar_collapsed");
        urlSteps.setProduction().open();
        steps.collapseSidebar();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetOffersPage().sidebar());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Разворачиваем боковое меню")
    public void shouldSeeUncollapsedSidebar() {
        steps.collapseSidebar();
        unfoldSidebar();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Боковое меню при скролле")
    public void shouldSeeSidebarOnBottomPage() {
        steps.onCabinetOffersPage().sidebar().should(isDisplayed());
        steps.scroll(5000);
        steps.onCabinetOffersPage().sidebar().should(isDisplayed());
    }

    @Step("Разворачиваем боковое меню")
    private void unfoldSidebar() {
        steps.onCabinetOffersPage().sidebar().item("Свернуть").click();
        steps.onCabinetOffersPage().sidebar().waitUntil(hasClass(not(containsString("Sidebar Sidebar_collapsed"))));
    }
}
