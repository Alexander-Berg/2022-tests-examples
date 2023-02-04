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
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Частные лица. Наличие системных сообщений")
@Issue("VERTISTEST-818")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MessageRunningOutFundsCompareTest {

    private static final String NOTIFICATION = "В кошельке заканчиваются средства.";
    private static final String MIN_SUM_TO_SEE_NOTIFICATION = "10";


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

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private WalletSteps walletSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL)).create();
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(WALLET).open();
        walletSteps.addMoneyToWallet(MIN_SUM_TO_SEE_NOTIFICATION);
        walletSteps.payWithCardWithoutRemember();

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Должны видеть сообщение о том что не хватает средства для продления услуг»")
    public void shouldSeeSystemMessage() {
        Screenshot testing = compareSteps.getElementScreenshot(basePageSteps.onManagementNewPage()
                .notification(NOTIFICATION));

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshot(basePageSteps.onManagementNewPage()
                .notification(NOTIFICATION));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
