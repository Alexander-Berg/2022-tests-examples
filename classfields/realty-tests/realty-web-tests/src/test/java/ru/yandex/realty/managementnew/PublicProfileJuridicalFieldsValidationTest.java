package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.management.PublicProfile.ADD_LOGO_WARN;
import static ru.yandex.realty.element.management.PublicProfile.FILL_ADDRESS_WARN;
import static ru.yandex.realty.element.management.PublicProfile.FILL_DESCRIPTION_WARN;
import static ru.yandex.realty.element.management.PublicProfile.FILL_FIELD;
import static ru.yandex.realty.element.management.PublicProfile.FILL_FIELDS;
import static ru.yandex.realty.element.management.PublicProfile.FILL_FOUND_DATE_WARN;
import static ru.yandex.realty.element.management.PublicProfile.FILL_WORK_DAYS_WARN;
import static ru.yandex.realty.element.management.PublicProfile.FILL_WORK_TIME_WARN;
import static ru.yandex.realty.element.management.SettingsContent.SAVE_CHANGES;

@Link("https://st.yandex-team.ru/VERTISTEST-1477")
@DisplayName("Публичный профиль")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PublicProfileJuridicalFieldsValidationTest {

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

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);
    }

    @Test
    @Tag(JURICS)
    @Owner(KANTEMIROV)
    @DisplayName("Сохраняем без полей -> видим все предупреждения")
    public void shouldSeeAllWarnMessage() {
        basePageSteps.resize(1200, 1600);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().enableProfile();
        managementSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(ADD_LOGO_WARN)
                .should(isDisplayed());
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_FOUND_DATE_WARN)
                .should(isDisplayed());
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_ADDRESS_WARN)
                .should(isDisplayed());
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_WORK_DAYS_WARN)
                .should(isDisplayed());
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_WORK_TIME_WARN)
                .should(isDisplayed());
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_DESCRIPTION_WARN)
                .should(isDisplayed());
    }

    @Test
    @Tag(JURICS)
    @Owner(KANTEMIROV)
    @DisplayName("Видим другие предупреждения при малом разрешении")
    public void shouldSeeOtherMessages() {
        basePageSteps.resize(1000, 1600);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        basePageSteps.scrollToElement(managementSteps.onManagementNewPage().settingsContent().publicProfile());
        managementSteps.onManagementNewPage().settingsContent().publicProfile().enableProfile();
        managementSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_FOUND_DATE_WARN)
                .should(not(isDisplayed()));
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_WORK_TIME_WARN)
                .should(not(isDisplayed()));
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_FIELD)
                .should(isDisplayed());
        managementSteps.onManagementNewPage().settingsContent().publicProfile().message(FILL_FIELDS)
                .should(isDisplayed());
    }


}
