package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.page.BasePage.MY_CABINET;


@DisplayName("Главная. Ссылки. Автторизация")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HeaderMainAuthorizedTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(VICDEV)
    @DisplayName("Ссылка «Личный кабинет»")
    public void shouldSeeManagementLink() {
        api.createVos2Account(account, AccountType.OWNER);

        urlSteps.testing().open();
        user.onBasePage().headerMain().link(MY_CABINET).should(isDisplayed());
    }
}
