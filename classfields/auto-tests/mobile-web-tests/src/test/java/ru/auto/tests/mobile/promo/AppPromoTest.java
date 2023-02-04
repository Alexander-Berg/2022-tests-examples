package ru.auto.tests.mobile.promo;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо приложения")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class AppPromoTest {

    private static final String PROMO_COOKIE = "promo-header-counter";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        cookieSteps.setCookieForBaseDomain("noads", "1");
        cookieSteps.deleteCookie(PROMO_COOKIE);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение промо")
    public void shouldSeeAppPromo() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().appPromo().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке установки приложения")
    public void shouldClickInstallButton() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().appPromo().installButton().waitUntil(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Пропустить»")
    public void shouldClickSkipButton() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().appPromo().skipButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().appPromo().waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().header().waitUntil(isDisplayed());
        basePageSteps.onMainPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Промо не должно отображаться в листинге")
    public void shouldNotSeeAppPromo() {
        mockRule.with("desktop/SearchCarsAll").update();

        urlSteps.testing().path(CARS).path(ALL).open();
        basePageSteps.onMainPage().appPromo().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Промо не должно отображаться после 5 показов")
    public void shouldNotSeeAppPromoAfter5Shows() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().appPromo().waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue(PROMO_COOKIE, "1");
        basePageSteps.refresh();
        basePageSteps.onMainPage().appPromo().waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue(PROMO_COOKIE, "2");
        basePageSteps.refresh();
        basePageSteps.onMainPage().appPromo().waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue(PROMO_COOKIE, "3");
        basePageSteps.refresh();
        basePageSteps.onMainPage().appPromo().waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue(PROMO_COOKIE, "4");
        basePageSteps.refresh();
        basePageSteps.onMainPage().appPromo().waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue(PROMO_COOKIE, "5");
        basePageSteps.refresh();
        basePageSteps.onMainPage().appPromo().should(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(PROMO_COOKIE, "99");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Промо не должно отображаться с параметром nosplash=1")
    public void shouldNotSeeAppPromoWithParameter() {
        urlSteps.testing().addParam("nosplash", "1").open();
        basePageSteps.onMainPage().appPromo().should(not(isDisplayed()));
    }
}
