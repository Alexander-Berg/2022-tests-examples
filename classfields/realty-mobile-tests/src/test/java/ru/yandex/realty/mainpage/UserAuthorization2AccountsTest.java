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

import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.AUTH;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.mobile.element.main.UserPopup.ADD_USER;
import static ru.yandex.realty.utils.AccountType.OWNER;

@Issue("VERTISTEST-1352")
@Epic(MAIN)
@Feature(AUTH)
@DisplayName("Остаемся на главной после авторизации второго юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class UserAuthorization2AccountsTest {

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

    @Inject
    private Account account2;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Остаемся на главной после авторизации второго юзера")
    public void shouldSeeMainPageAfterSecondAuthorization() {
        api.createYandexAccount(account);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().header().userAvatar().click();
        basePageSteps.onBasePage().userPopup().link(ADD_USER).click();
        api.createVos2AccountWithoutLogin(account2, OWNER);
        basePageSteps.onPassportLoginPage().addAccount().click();
        basePageSteps.onPassportLoginPage().loginInPassport(account2);

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
