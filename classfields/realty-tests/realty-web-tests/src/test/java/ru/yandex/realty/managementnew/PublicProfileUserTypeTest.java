package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;

@Link("https://st.yandex-team.ru/VERTISTEST-1477")
@DisplayName("Публичный профиль")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PublicProfileUserTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Test
    @Tag(JURICS)
    @Owner(KANTEMIROV)
    @DisplayName("Для агента есть публичный профиль")
    public void shouldSeeAgentOwnerProfile() {
        apiSteps.createRealty3JuridicalAccountWithType(account, "AGENT");
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().should(isDisplayed());
    }

    @Test
    @Tag(JURICS)
    @Owner(KANTEMIROV)
    @DisplayName("Для агентства юрика есть публичный профиль")
    public void shouldSeeAgencyJuridicalProfile() {
        apiSteps.createRealty3JuridicalAccount(account);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Для физика нет публичного профиля")
    public void shouldNotSeeOwnerProfile() {
        apiSteps.createVos2Account(account, AccountType.OWNER);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().should(not(isDisplayed()));
    }
}
