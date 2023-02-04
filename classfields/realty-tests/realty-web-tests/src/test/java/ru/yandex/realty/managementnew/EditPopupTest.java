package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.awaitility.Awaitility.given;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.utils.AccountType.AGENCY;
import static ru.yandex.realty.utils.AccountType.AGENT;
import static ru.yandex.realty.utils.AccountType.DEVELOPER;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Проверка попапа редактирования контактной информации")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EditPopupTest {

    private static final String ACCOUNT_TYPE = "Тип аккаунта";
    private static final String SAVE_CHANGES = "Сохранить изменения";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Parameterized.Parameter
    public String accountTypeName;

    @Parameterized.Parameter(1)
    public AccountType accountType;

    @Parameterized.Parameters(name = "{index} - Должнен поменяться тип аккаунта на {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Агент", AGENT},
                {"Агентство", AGENCY},
                {"Застройщик", DEVELOPER}
        });
    }

    @Before
    public void createUser() {
        api.createVos2Account(account, OWNER);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Screenshot.class, Testing.class})
    public void shouldSeeAgentWithNecessaryField() {
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().button(accountTypeName).click();
        managementSteps.onManagementNewPage().button(SAVE_CHANGES).click();
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().pollInSameThread()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> api.shouldSeeAccountInVos2(account).hasType(accountType.getValue()));
    }
}
