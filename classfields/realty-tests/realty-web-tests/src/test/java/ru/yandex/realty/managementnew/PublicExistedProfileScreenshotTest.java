package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;

@Link("https://st.yandex-team.ru/VERTISTEST-1477")
@DisplayName("Публичный профиль")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class PublicExistedProfileScreenshotTest {

    private static final String ENABLED_PROFILE_LOGIN = "autotestprofileon";
    private static final String ENABLED_PROFILE_PASSWORD = "Mytests2021!";
    private static final String DISABLED_PROFILE_LOGIN = "autotestprofileoff";
    private static final String DISABLED_PROFILE_PASSWORD = "Qwerty12345#";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 3000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест на заполненный включенный профиль")
    public void shouldSeeEnabledProfile() {
        passportSteps.login(ENABLED_PROFILE_LOGIN, ENABLED_PROFILE_PASSWORD);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        Screenshot testing = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Description("НУЖЕН ДРУГОЙ ПОЛЬЗОВАТЕЛЬ")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншотный тест на заполненный выключенный профиль")
    public void shouldSeeDisabledProfile() {
        passportSteps.login(DISABLED_PROFILE_LOGIN, DISABLED_PROFILE_PASSWORD);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        Screenshot testing = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().publicProfile());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
