package ru.yandex.realty.adblockoffers;

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
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.categories.Adblock;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModuleWithAdBlock;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.ADBLOCK;
import static ru.yandex.realty.element.management.OfferServicesPanel.ADD_SERVICE_FREE;
import static ru.yandex.realty.element.management.OfferServicesPanel.PROMOTION;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPromotion;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kantemirov
 */
@DisplayName("Личный кабинет с AdBlock'ом.")
@Feature(ADBLOCK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithAdBlock.class)
public class BuyServiceTest {

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
    private PromocodesSteps promocodesSteps;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private Vos2Adaptor vos2Adaptor;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
        managementSteps.switchToTab(0);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).create();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Adblock.class, Testing.class})
    @DisplayName("Покупаем услуги для офферов")
    public void shouldBuyServicesWithAdBlock() {
        promocodesSteps.use2000Promo();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel().service(PROMOTION).button(ADD_SERVICE_FREE)
                .click();
        walletSteps.payAndWaitSuccess();
        vos2Adaptor.getUserOffers(account.getId()).getOffers()
                .forEach(offer -> vos2Adaptor.waitOffer(account.getId(), offer.getId(), hasPromotion()));
    }
}
