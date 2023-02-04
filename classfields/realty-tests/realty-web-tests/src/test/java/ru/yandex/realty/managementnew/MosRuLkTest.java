package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.rules.MockRuleConfigurable.forResponse;

@DisplayName("Тесты на блок mos.ru")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MosRuLkTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Parameterized.Parameter
    public String message;

    @Parameterized.Parameter(1)
    public String badgeText;

    @Parameterized.Parameter(2)
    public int offerNumber;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Собственник не найден в данных из ЕГРН", "Продаёт собственник", 0},
                {"Не указан номер квартиры", "Продаёт собственник", 2},
                {"Проверка успешна", "Сдаёт собственник", 3},
                {"Не смогли проверить вашу учётную запись", "Сдаёт собственник", 4},
        });
    }

    @Before
    public void before() {
        String uid = apiSteps.createVos2Account(account, AccountType.OWNER).getId();
        mockRuleConfigurable.create(forResponse("mock/managementnew/MosRuLkTest/shouldSeeMosRuLabel.json")
                .replaceAll("#UID#", uid)).setMockritsaCookie();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим шильдик собственника")
    public void shouldSeeMosRuLabel() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.moveCursor(managementSteps.onManagementNewPage().offer(offerNumber).badge(badgeText));
        managementSteps.onManagementNewPage().openedPopup().should(hasText(containsString(message)));
    }
}
