package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.element.management.SettingsContent.SAVE_CHANGES;

@Link("https://st.yandex-team.ru/VERTISTEST-1477")
@DisplayName("Публичный профиль")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PublicProfileScreenshotTest {

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
    private CompareSteps compareSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест на пустой выключенный профиль")
    public void shouldSeeEmptyDisabledProfile() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        Screenshot testing = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест профиль с ошибками заполнения")
    public void shouldSeeEmptyWithWarningsProfile() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().enableProfile();
        managementSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        Screenshot testing = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        urlSteps.setProductionHost().open();
        managementSteps.onManagementNewPage().settingsContent().publicProfile().enableProfile();
        managementSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        Screenshot production = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
