package ru.yandex.realty.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferInfo;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE_DIRECT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.element.wallet.PromocodePaymentPopup.QUIT;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPremium;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPromotion;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasRaising;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasTurboSale;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PREMIUM;
import static ru.yandex.realty.page.OfferAddPage.PROMOTION;
import static ru.yandex.realty.page.OfferAddPage.RISING;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Форма редактирования. Оплата")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class ServicesFromEditPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
        promocodesSteps.use2000Promo();
        String id = offerBuildingSteps.addNewOffer(account).withSearcherWait().create().getId();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(id).open();
        offerAddSteps.setFlat("1");
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);
    }

    @Test
    @DisplayName("Видим поднятие при оплате со страницы редактирования")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldRiseFromEditPage() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().paySelector(RISING).waitUntil(isDisplayed());
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        payAndCheck(hasRaising());
    }

    @Test
    @DisplayName("Видим продвижение при оплате со страницы редактирования")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldPromoteFromEditPage() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().selectPaySelector(PROMOTION);
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        payAndCheck(hasPromotion());
    }

    @Test
    @DisplayName("Видим премиум при оплате со страницы редактирования")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldPremiumFromEditPage() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().selectPaySelector(PREMIUM);
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        payAndCheck(hasPremium());
    }

    @Test
    @DisplayName("Видим турбо при оплате со страницы редактирования")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldTurboFromEditPage() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        payAndCheck(hasTurboSale());
    }

    @Test
    @DisplayName("Видим все опции при оплате со страницы-заглушки")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeAllServicesFromStubPage() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().selectPaySelector(PREMIUM);
        offerAddSteps.onOfferAddPage().publishTrap().selectPaySelector(PROMOTION);
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        payAndCheck(allOf(hasRaising(), hasPremium(), hasPromotion()));
    }

    @Test
    @DisplayName("Нет опций при бесплаттном клике")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldNotSeeAllServicesFromStubPage() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        apiSteps.waitFirstOffer(account, allOf(not(hasRaising()), not(hasPremium()), not(hasPromotion())));
    }

    @Test
    @DisplayName("Нет опций при отмене оплаты")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldNotSeeAllServicesFromStubPageAndCancel() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        apiSteps.waitFirstOffer(account, allOf(not(hasRaising()), not(hasPremium()), not(hasPromotion())));
        walletSteps.onWalletPage().promocodePopup().closeButton().click();
        walletSteps.onWalletPage().promocodePopup().spanLink(QUIT).clickIf(isDisplayed());
    }

    @Step("Платим и проверяем наличие услуги")
    private void payAndCheck(Matcher<OfferInfo> matcher) {
        walletSteps.payAndWaitSuccess();
        apiSteps.waitFirstOffer(account, matcher);
    }
}
