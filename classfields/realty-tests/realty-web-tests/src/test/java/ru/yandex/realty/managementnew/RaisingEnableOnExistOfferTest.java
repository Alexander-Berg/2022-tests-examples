package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasRaising;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */

@DisplayName("Поднятие существующего оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class RaisingEnableOnExistOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private Account account;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        promocodesSteps.use2000Promo();
        offerBuildingSteps.addNewOffer(account).create();

        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.refreshUntil(() -> managementSteps.onManagementNewPage().offer(FIRST).servicesPanel(),
                isDisplayed());
        managementSteps.onManagementNewPage().offer(0).button("Поднять").click();
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Должны видеть «поднятие» после публикации с оплатой")
    public void shouldSeeRaisingAfterPayment() {
        walletSteps.payAndWaitSuccess();
        apiSteps.waitFirstOffer(account, hasRaising());
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Не должны видеть «поднятие» после отмены оплаты")
    public void shouldNotSeeRaisingWithoutPay() {
        walletSteps.onWalletPage().promocodePopup().waitUntil(isDisplayed());
        walletSteps.onWalletPage().promocodePopup().closeButton().click();
        apiSteps.waitFirstOffer(account, not(hasRaising()));
    }
}
