package ru.auto.tests.mag.mobile;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(HEADER)
@DisplayName("Шапка в журнале")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class MagHeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.subdomain(SUBDOMAIN_MAG).open();
        waitSomething(5, TimeUnit.SECONDS);
        urlSteps.refresh();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по логотипу")
    @Owner(DSVICHIHIN)
    public void shouldClickLogo() {
        basePageSteps.onMagPage().header().logo().should(isDisplayed()).click();
        urlSteps.mobileURI().path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Продать авто»")
    @Owner(DSVICHIHIN)
    public void shouldClickAddSaleButton() {
        basePageSteps.onMagPage().header().addSaleButton().should(isDisplayed()).click();
        urlSteps.mobileURI().path(PROMO).path(FROM_WEB_TO_APP).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открытие сайдбара")
    public void shouldOpenSidebar() {
        basePageSteps.onMagPage().header().sidebarButton().click();
        basePageSteps.onMagPage().sidebar().waitUntil(isDisplayed());
    }
}