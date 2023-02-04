package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.element.base.HeaderMain.REFILL_BUTTON;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Частные лица. Пополнение счета «из хедера»")
@Issue("VERTISTEST-818")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class RefillWalletPopupCompareTest {

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
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Жмем кнопку пополнения кошелька, сравниваем скриншоты попапов")
    public void shouldSeeRefillPopup() {
        basePageSteps.onBasePage().headerMain().buttonWithTitle(REFILL_BUTTON).click();
        basePageSteps.onBasePage().refillWalletPopup().h2().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().refillWalletPopup());

        urlSteps.setProductionHost().open();
        basePageSteps.onBasePage().headerMain().buttonWithTitle(REFILL_BUTTON).click();
        basePageSteps.onBasePage().refillWalletPopup().h2().click();
        Screenshot production = compareSteps.getElementScreenshot(basePageSteps.onBasePage().refillWalletPopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
