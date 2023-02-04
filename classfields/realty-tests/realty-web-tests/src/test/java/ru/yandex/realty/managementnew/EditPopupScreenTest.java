package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.utils.AccountType.AGENCY;
import static ru.yandex.realty.utils.AccountType.AGENT;
import static ru.yandex.realty.utils.AccountType.OWNER;


@DisplayName("Проверка попапа редактирования контактной информации. Скриншоты")
@Feature(MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EditPopupScreenTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;


    @Parameterized.Parameter
    public AccountType accountType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {OWNER},
                {AGENT},
                {AGENCY}
        });
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Должнен появляться попап")
    @Category({Regression.class, Screenshot.class, Testing.class})
    public void shouldSeeSamePopups() {
        api.createVos2Account(account, accountType);
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        Screenshot testing = compareSteps
                .getElementScreenshot(managementSteps.onManagementNewPage().settingsContent().waitUntil(isDisplayed()));

        urlSteps.production().path(MANAGEMENT_NEW_SETTINGS).open();
        Screenshot production = compareSteps
                .getElementScreenshot(managementSteps.onManagementNewPage().settingsContent().waitUntil(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
