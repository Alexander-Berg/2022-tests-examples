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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPromotion;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */

@DisplayName("ЛК. Услуги. Продление продвижения")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PromotionRenewalOfferTest {

    private static final String PROMOTION = "Продвижение";
    private static final String RENEW_FOR = "Продлить за";

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
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Продвижение. Не видим линк продления без подключения")
    public void shouldNotSeeRenewalLinkWithoutService() {
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel()
                .service(PROMOTION).button(RENEW_FOR).should(not(exists()));
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Продвижение. Видим линк продления после подключения")
    public void shouldSeeRenewalLinkAfterBuyService() {
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel().service(PROMOTION).button("Подключить")
                .click();
        walletSteps.payAndWaitSuccess();
        apiSteps.waitFirstOffer(account, hasPromotion());
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel().service(PROMOTION)
                .renewalButton().should(isDisplayed());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Продвижение. Видим авточекбокс после подключения")
    public void shouldSeeRenewalCheckboxCheckedAfterBuyService() {
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel().service(PROMOTION).button("Подключить")
                .click();
        walletSteps.payAndWaitSuccess();
        apiSteps.waitFirstOffer(account, hasPromotion());
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel().service(PROMOTION)
                .autoRenew().should(isChecked());
    }

}
