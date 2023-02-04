package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Гараж - прыщ в шапке")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageHeaderDotTest {

    private static final String VIN_CARD_ID = "/1146321503/";
    private static final String COOKIE_NAME = "navigation_dot_seen-garage-new-dot";
    private static final String COOKIE_VALUE = "true";

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

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отсутствие прыща на главной")
    public void shouldNotSeeDotOnMain() {
        basePageSteps.onGaragePage().header().line2().buttonWithDot("Гараж").click();
        urlSteps.testing().path(GARAGE).shouldNotSeeDiff();
        basePageSteps.onGaragePage().header().line2().buttonWithDot("Гараж").waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(COOKIE_NAME, COOKIE_VALUE);
        urlSteps.testing().open();
        basePageSteps.onGaragePage().header().line2().buttonWithDot("Гараж").waitUntil(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отсутствие прыща на карточке")
    public void shouldNotSeeDotOnCard() {
        mockRule.with("desktop/GarageUserCardsVinPost",
                "desktop/GarageUserCardVin").update();

        basePageSteps.onGaragePage().header().line2().buttonWithDot("Гараж").click();
        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGaragePage().header().line2().buttonWithDot("Гараж").waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(COOKIE_NAME, COOKIE_VALUE);
        urlSteps.testing().open();
        basePageSteps.onGaragePage().header().line2().buttonWithDot("Гараж").waitUntil(not(isDisplayed()));
    }
}