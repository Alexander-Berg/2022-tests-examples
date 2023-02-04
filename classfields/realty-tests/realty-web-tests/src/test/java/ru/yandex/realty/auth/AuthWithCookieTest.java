package ru.yandex.realty.auth;

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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.RealtyFeatures.AUTH;

/**
 * Created by vicdev on 26.04.17.
 */

@DisplayName("Авторизация с куками")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AuthWithCookieTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ManagementSteps user;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Before
    public void openManagementPage() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(VICDEV)
    public void shouldSeeEmptyAccountForAgency() {
        api.createVos2Account(account, AccountType.AGENCY);
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        user.shouldSeeEmptyAuthAccount();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(VICDEV)
    public void shouldSeeEmptyAccountForAgent() {
        api.createVos2Account(account, AccountType.AGENT);
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        user.shouldSeeEmptyAuthAccount();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(VICDEV)
    public void shouldSeeEmptyAccountForOwner() {
        api.createVos2Account(account, AccountType.OWNER);
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        user.shouldSeeEmptyAuthAccount();
    }
}
