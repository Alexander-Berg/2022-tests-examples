package ru.yandex.realty.managementnew.requisites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mock.GetMoneyPersonTemplate;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS_BILLING;
import static ru.yandex.realty.element.management.SettingsContent.EMAIL_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.PHONE_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.SAVE_CHANGES;
import static ru.yandex.realty.element.management.SettingsContent.TITLE_NAME_SECTION;
import static ru.yandex.realty.mock.GetMoneyPersonTemplate.getMoneyPersonTemplate;
import static ru.yandex.realty.page.ManagementNewPage.ADD_PAYER;

@DisplayName("Страница реквизитов")
@Feature(MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class ScreenshotPageTest {

    private GetMoneyPersonTemplate responseMock;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 3000);
        responseMock = getMoneyPersonTemplate();
        apiSteps.createRealty3JuridicalAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест с реквизитами")
    public void shouldSeePageScreenshotWithBilling() {
        mockRuleConfigurable.getMoneyPerson(account.getId(), responseMock.build()).createWithDefaults();
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS_BILLING).open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().settingsContent());
        urlSteps.setProductionHost().open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().settingsContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест без реквизитов")
    public void shouldSeePageScreenshotWithoutBilling() {
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS_BILLING).open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
        clearFields();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().settingsContent());
        urlSteps.setProductionHost().open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
        clearFields();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().settingsContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест с ошибками реквизитов")
    public void shouldSeePageScreenshotWithErrorsBilling() {
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS_BILLING).open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
        clearFields();
        basePageSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().settingsContent());
        urlSteps.setProductionHost().open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
        clearFields();
        basePageSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().settingsContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Очищаем поля")
    private void clearFields() {
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(TITLE_NAME_SECTION).clearSign().click();
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(EMAIL_SECTION).clearSign().click();
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(PHONE_SECTION).clearSign().click();
    }
}
