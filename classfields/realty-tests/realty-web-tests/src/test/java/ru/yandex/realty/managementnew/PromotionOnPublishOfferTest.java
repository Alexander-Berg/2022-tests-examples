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
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.MANAGEMENT_NEW;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPromotion;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PUBLISH_WITH_OPTIONS_FOR;
import static ru.yandex.realty.step.OfferAddSteps.MOSCOW_LOCATION;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */

@DisplayName("Публикуем с продвижением")
@Feature(MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PromotionOnPublishOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private Account account;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        promocodesSteps.use2000Promo();
        urlSteps.setMoscowCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat(MOSCOW_LOCATION);
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishBlock().payButton()
                .waitUntil(hasText(findPattern(PUBLISH_WITH_OPTIONS_FOR))).click();
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Включаем «продвижение» после публикации с оплатой")
    public void shouldSeePromotionAfterPayment() {
        walletSteps.payAndWaitSuccess();

        apiSteps.waitFirstOffer(account, hasPromotion());
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Не включаем «продвижение» после отмены оплаты")
    public void shouldNotSeePromotionWithoutPay() {
        walletSteps.onWalletPage().promocodePopup().waitUntil(isDisplayed());
        walletSteps.onWalletPage().promocodePopup().closeButton().click();

        apiSteps.waitFirstOffer(account, not(hasPromotion()));
    }
}
