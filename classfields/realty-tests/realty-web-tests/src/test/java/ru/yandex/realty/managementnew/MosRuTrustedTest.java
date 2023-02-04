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

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.mock.GetUserUidTemplate.getUserUidTemplate;

@DisplayName("Тесты на блок mos.ru")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MosRuTrustedTest {

    public static final String TRUSTED_MESSAGE = "Учётная запись mos.ru привязана";

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
    public String type;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static List<String> getData() {
        return asList("OWNER", "PRIVATE_AGENT");
    }

    @Before
    public void before() {
        String id = apiSteps.createVos2Account(account, AccountType.OWNER).getId();
        mockRuleConfigurable.getUserUid(id, getUserUidTemplate().setId(id).setExtendedUserType(type)
                .setMosRuStatus("TRUSTED").build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим сообщение о привязанном аккаунте mos.ru на форме добавления")
    public void shouldSeeMosRuOnOfferAddPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        managementSteps.onOfferAddPage().dealType().selectButton(SELL);
        managementSteps.onOfferAddPage().offerType().selectButton(FLAT);
        managementSteps.onOfferAddPage().mosruBLock().waitUntil(isDisplayed());
        managementSteps.onOfferAddPage().mosruBLock().should(hasText(containsString(TRUSTED_MESSAGE)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим сообщение о привязанном аккаунте mos.ru на странице контактов")
    public void shouldSeeMosRuOnContactsPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().mosruBLock().waitUntil(isDisplayed());
        managementSteps.onManagementNewPage().settingsContent().mosruBLock()
                .should(hasText(containsString(TRUSTED_MESSAGE)));
    }
}
