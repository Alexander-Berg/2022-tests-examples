package ru.auto.tests.mobilereviews.redirects;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.NOMOBILE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Переход с мобильной на полную версию сайта")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class NoMobileTest {

    private final static String NOMOBILE_COOKIE_NAME = "nomobile";
    private final static String NOMOBILE_COOKIE_VALUE = "1";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ru.auto.tests.desktop.mobile.step.BasePageSteps mobileSteps;

    @Inject
    private ru.auto.tests.desktop.step.BasePageSteps desktopSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).open();
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class})
    @DisplayName("Отображение баннера перехода на мобильную версию")
    public void shouldSeeNoMobileBanner() {
        mobileSteps.onReviewsMainPage().header().sidebarButton().hover().click();
        mobileSteps.onReviewsMainPage().button("Полная версия").hover().click();

        desktopSteps.onReviewsMainPage().noMobileBanner().waitUntil(isDisplayed());
        urlSteps.testing().path(REVIEWS).path(SLASH).addParam(NOMOBILE, "true").shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(NOMOBILE_COOKIE_NAME, NOMOBILE_COOKIE_VALUE);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEXANDERREX)
    @DisplayName("Баннер «Перейти на мобильную версию Авто.ру?» - кнопка «Да»")
    public void shouldClickYesButtonOnNoMobileBanner() {
        urlSteps.testing().path(REVIEWS).path(SLASH).addParam(NOMOBILE, "true").open();
        desktopSteps.onReviewsMainPage().noMobileBanner().should(hasText("Перейти на мобильную версию Авто.ру?\nДа\nНет"));
        desktopSteps.onReviewsMainPage().noMobileBanner().button("Да").click();

        urlSteps.testing().path(REVIEWS).shouldNotSeeDiff();
        cookieSteps.shouldNotSeeCookie(NOMOBILE_COOKIE_NAME);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEXANDERREX)
    @DisplayName("Баннер «Перейти на мобильную версию Авто.ру?» - кнопка «Нет»")
    public void shouldClickNoButtonOnNoMobileBanner() {
        urlSteps.testing().path(REVIEWS).path(SLASH).addParam(NOMOBILE, "true").open();
        desktopSteps.onReviewsMainPage().noMobileBanner().should(hasText("Перейти на мобильную версию Авто.ру?\nДа\nНет"));
        desktopSteps.onReviewsMainPage().noMobileBanner().button("Нет").click();
        desktopSteps.onReviewsMainPage().noMobileBanner().waitUntil(not(isDisplayed()));

        urlSteps.replaceQuery("nomobile=true").shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(NOMOBILE_COOKIE_NAME, NOMOBILE_COOKIE_VALUE);
    }
}
