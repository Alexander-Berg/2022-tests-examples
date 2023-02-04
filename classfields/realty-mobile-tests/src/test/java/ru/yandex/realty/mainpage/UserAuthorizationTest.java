package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.config.RealtyWebConfig;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.UrlSteps;

import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import static java.net.URLEncoder.encode;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.AUTH;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.main.UserPopup.ADD_USER;
import static ru.yandex.realty.mobile.element.main.UserPopup.LOGOUT;
import static ru.yandex.realty.mobile.page.BasePage.LOGIN;
import static ru.yandex.realty.utils.AccountType.OWNER;

@Issue("VERTISTEST-1352")
@Epic(MAIN)
@Feature(AUTH)
@DisplayName("Авторизация")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class UserAuthorizationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private RealtyWebConfig config;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("URL по кнопке Войти")
    public void shouldSeeLoginURL() throws URISyntaxException, UnsupportedEncodingException {
        urlSteps.testing().path(MOSKVA).open();

        basePageSteps.onBasePage().link(LOGIN).should(hasHref(equalTo(UriBuilder.fromUri(config.
                getPassportTestURL().toURI()).uri(String.format("/auth?origin=realty_moscow&retpath=%s",
                encode(urlSteps.testing().path(MOSKVA).toString(), "UTF-8"))).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение попапа юзера")
    public void shouldSeeUserPopup() {
        api.createYandexAccount(account);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().header().userAvatar().click();

        basePageSteps.onBasePage().userPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка «Добавить пользователя»")
    public void shouldSeeAddUserURL() throws URISyntaxException, UnsupportedEncodingException {
        api.createYandexAccount(account);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().header().userAvatar().click();

        basePageSteps.onBasePage().userPopup().link(ADD_USER).should(hasHref(equalTo(UriBuilder.fromUri(config.
                getPassportTestURL().toURI()).uri(String.format("/auth?origin=realty_moscow&retpath=%s",
                encode(urlSteps.testing().path(MOSKVA).toString(), "UTF-8"))).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка «Выйти»")
    public void shouldSeeLogoutURL() throws URISyntaxException, UnsupportedEncodingException {
        api.createYandexAccount(account);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().header().userAvatar().click();

        basePageSteps.onBasePage().userPopup().link(LOGOUT).should(hasHref(containsString(UriBuilder.fromUri(config.
                getPassportTestURL().toURI()).uri(String.format("passport?action=logout&mode=embeddedauth&retpath=%s",
                encode(urlSteps.testing().path(MOSKVA).toString(), "UTF-8"))).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть «Войти» после разлогина")
    public void shouldSeeLoginAfterLogout() {
        api.createYandexAccount(account);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().header().userAvatar().click();
        basePageSteps.onBasePage().userPopup().link(LOGOUT).click();

        basePageSteps.onBasePage().link(LOGIN).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Остаемся на главной после авторизации")
    public void shouldSeeMainPageAfterAuthorization() {
        api.createVos2AccountWithoutLogin(account, OWNER);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().link(LOGIN).click();
        basePageSteps.onPassportLoginPage().loginInPassport(account);

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Остаемся на главной после разлогина")
    public void shouldSeeMainPageAfterLogout() {
        api.createYandexAccount(account);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().header().userAvatar().click();
        basePageSteps.onBasePage().userPopup().link(LOGOUT).click();

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
