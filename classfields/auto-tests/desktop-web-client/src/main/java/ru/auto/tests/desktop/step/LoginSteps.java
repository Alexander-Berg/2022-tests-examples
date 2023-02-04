package ru.auto.tests.desktop.step;

import javax.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;

import static ru.auto.tests.desktop.step.CookieSteps.AUTORU_SID;
import static ru.auto.tests.desktop.step.CookieSteps.AUTORU_UID;

public class LoginSteps extends WebDriverSteps {

    @Inject
    private PublicApiAdaptor publicApiAdaptor;

    @Inject
    public CookieSteps cookieSteps;

    @Step("Авторизуемся")
    public AutoApiLoginResponse loginAs(Account account) {
        AutoApiLoginResponse loginInfo = publicApiAdaptor.login(account);

        cookieSteps.setCookieForBaseDomain(AUTORU_SID, loginInfo.getSession().getId());
        cookieSteps.setCookieForBaseDomain(AUTORU_UID, loginInfo.getSession().getDeviceUid());

        refresh();

        return loginInfo;
    }

}
