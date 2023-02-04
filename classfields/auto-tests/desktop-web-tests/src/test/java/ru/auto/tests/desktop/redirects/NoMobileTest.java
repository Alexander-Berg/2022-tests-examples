package ru.auto.tests.desktop.redirects;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Get-параметр и кука nomobile")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class NoMobileTest {

    private final static String NOMOBILE_COOKIE_NAME = "nomobile";
    private final static String NOMOBILE_COOKIE_VALUE = "1";
    private final static String NOADSMOBILE_COOKIE_NAME = "noad-mobile";
    private final static String NOADSMOBILE_COOKIE_VALUE = "1";

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
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Get-параметр nomobile")
    public void shouldOpenDesktopWithNoMobileParam() {
        urlSteps.fromUri(format("%s/?nomobile", urlSteps.getConfig().getTestingURI())).open();
        urlSteps.replaceQuery("nomobile").shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(NOMOBILE_COOKIE_NAME, NOMOBILE_COOKIE_VALUE);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Кука nomobile")
    public void shouldOpenDesktopWithNoMobileCookie() {
        cookieSteps.setCookieForBaseDomain(NOMOBILE_COOKIE_NAME, NOMOBILE_COOKIE_VALUE);
        urlSteps.testing().open();
        urlSteps.path("/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Баннер «Перейти на мобильную версию Авто.ру?» - кнопка «Да»")
    public void shouldClickYesButton() {
        urlSteps.fromUri(format("%s/?nomobile", urlSteps.getConfig().getTestingURI())).open();
        basePageSteps.onMainPage().noMobileBanner().should(hasText("Перейти на мобильную версию Авто.ру?\nДа\nНет"));
        basePageSteps.onMainPage().noMobileBanner().button("Да").click();
        urlSteps.fromUri(format("https://%s/", urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
        cookieSteps.shouldNotSeeCookie(NOMOBILE_COOKIE_NAME);
        cookieSteps.shouldNotSeeCookie(NOADSMOBILE_COOKIE_NAME);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Баннер «Перейти на мобильную версию Авто.ру?» - кнопка «Нет»")
    public void shouldClickNoButton() {
        urlSteps.fromUri(format("%s/?nomobile", urlSteps.getConfig().getTestingURI())).open();
        basePageSteps.onMainPage().noMobileBanner().button("Нет").click();
        basePageSteps.onMainPage().noMobileBanner().waitUntil(not(isDisplayed()));
        urlSteps.replaceQuery("nomobile").shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(NOMOBILE_COOKIE_NAME, NOMOBILE_COOKIE_VALUE);
        cookieSteps.shouldSeeCookieWithValue(NOADSMOBILE_COOKIE_NAME, NOADSMOBILE_COOKIE_VALUE);
    }
}
