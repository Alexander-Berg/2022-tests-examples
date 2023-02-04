package ru.yandex.general.homepage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.element.Header.HELP;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(HOMEPAGE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с главной")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFromHomepageUserInfoPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной на паспорт по клику на имя юзера из попапа «Юзер инфо»")
    public void shouldSeeHomePageToPassportFromUserInfoNameClick() {
        basePageSteps.onListingPage().header().avatar().click();
        basePageSteps.onListingPage().userInfoPopup().spanLink("Vasily Pupkin").waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri("https://passport.yandex.ru/profile").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной на паспорт по клику на имя юзера из попапа «Юзер инфо» прилипшего хэдера")
    public void shouldSeeHomePageToPassportFromUserInfoNameClickFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().avatar().click();
        basePageSteps.onListingPage().userInfoPopup().spanLink("Vasily Pupkin").waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri("https://passport.yandex.ru/profile").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной по клику на «Помощь» из попапа «Юзер инфо»")
    public void shouldSeeHomePageToHelpFromUserInfoPopup() {
        basePageSteps.onListingPage().header().avatar().click();
        basePageSteps.onListingPage().userInfoPopup().link(HELP).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri("https://yandex.ru/support/o-desktop/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной по клику на «Помощь» из попапа «Юзер инфо» прилипшего хэдера")
    public void shouldSeeHomePageToHelpFromUserInfoPopupFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().avatar().click();
        basePageSteps.onListingPage().userInfoPopup().link(HELP).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri("https://yandex.ru/support/o-desktop/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной по клику на «Добавить профиль» из попапа «Юзер инфо»")
    public void shouldSeeHomePageToAddAccountFromUserInfoPopup() {
        basePageSteps.onListingPage().header().avatar().click();
        basePageSteps.onListingPage().userInfoPopup().link("Добавить профиль").waitUntil(isDisplayed()).click();

        urlSteps.fromUri(format("https://passport.yandex.ru/auth?mode=add-user&retpath=%s", urlSteps))
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной по клику на «Добавить профиль» из попапа «Юзер инфо» прилипшего хэдера")
    public void shouldSeeHomePageToAddAccountFromUserInfoPopupFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().avatar().click();
        basePageSteps.onListingPage().userInfoPopup().link("Добавить профиль").waitUntil(isDisplayed()).click();

        urlSteps.fromUri(format("https://passport.yandex.ru/auth?mode=add-user&retpath=%s", urlSteps))
                .shouldNotDiffWithWebDriverUrl();
    }

}
