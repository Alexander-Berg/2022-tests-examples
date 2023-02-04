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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.MANAGEMENT;
import static ru.yandex.realty.utils.AccountType.AGENT;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Показ контактов юзера со страницы личного кабинета")
@Feature(MANAGEMENT)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShowContactsTest {

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

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public AccountType accountType;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {OWNER},
                {AGENT},
        });
    }

    @Before
    public void before() {
        apiSteps.createVos2Account(account, accountType);
        compareSteps.resize(1920, 3000);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Жмем на «Контактная информация»")
    public void compareContactsPopupWithProduction() {

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        basePageSteps.onBasePage().headerMain().userAccount().waitUntil(isDisplayed()).click();
        basePageSteps.onBasePage().userNewPopup().link("Настройки").waitUntil(isDisplayed()).click();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(managementSteps.onManagementNewPage()
                .settingsContent().waitUntil(isDisplayed()));

        urlSteps.production().path(Pages.MANAGEMENT_NEW).open();
        basePageSteps.onBasePage().headerMain().userAccount().waitUntil(isDisplayed()).click();
        basePageSteps.onBasePage().userNewPopup().link("Настройки").waitUntil(isDisplayed()).click();
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(managementSteps.onManagementNewPage()
                .settingsContent().waitUntil(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
