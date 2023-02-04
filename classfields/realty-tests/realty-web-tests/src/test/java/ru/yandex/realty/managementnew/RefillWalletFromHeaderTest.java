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
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Частные лица. Пополнение счета «из хедера»")
@Issue("VERTISTEST-818")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class RefillWalletFromHeaderTest {

    private static final String REFILL_AMOUNT = "501";

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
    private WalletSteps walletSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account).create();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        basePageSteps.onBasePage().headerMain().buttonWithTitle("Пополнить").click();
        basePageSteps.clearInputByBackSpace(() -> basePageSteps.onBasePage().refillWalletPopup().input());
        basePageSteps.onBasePage().refillWalletPopup().input().sendKeys(REFILL_AMOUNT);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Пополняем кошелек, проверяем что сумма отображается")
    public void shouldSeeRefillWallet() {
        basePageSteps.onBasePage().refillWalletPopup().button("Продолжить").click();
        walletSteps.payWithCardWithoutRemember();
        basePageSteps.refresh();
        basePageSteps.onBasePage().headerMain().buttonWithTitle("Пополнить").should(hasText(REFILL_AMOUNT + " \u20BD"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Вводим сумму пополнения, закрываем попап, проверяем что баланс остался прежним")
    public void shouldNotSeeRefillWalletAfterClosePopup() {
        basePageSteps.onBasePage().refillWalletPopup().closeButton().click();
        basePageSteps.onBasePage().refillWalletPopup().exit().click();
        basePageSteps.onBasePage().headerMain().buttonWithTitle("Пополнить").should(hasText("0 \u20BD"));
    }
}
