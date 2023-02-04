package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.SECONDARY;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.EDIT_OFFER;
import static ru.yandex.realty.element.offers.PublishBlock.ACTIVE;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.page.OfferAddPage.ACTIVATE_FOR;
import static ru.yandex.realty.page.OfferAddPage.ACTIVATE_WITH_OPTIONS_FOR;
import static ru.yandex.realty.page.OfferAddPage.FASTER_SALE;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PROMOTION;
import static ru.yandex.realty.page.OfferAddPage.RISING;
import static ru.yandex.realty.page.OfferAddPage.SAVE_AND_CONTINUE;
import static ru.yandex.realty.step.UrlSteps.ACTIVATION_VALUE;
import static ru.yandex.realty.step.UrlSteps.TRAP_VALUE;
import static ru.yandex.realty.step.UrlSteps.TRUE_VALUE;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.getRandomApartment;

@DisplayName("Форма добавления объявления.")
@Feature(OFFERS)
@Story(EDIT_OFFER)
@Issue("VERTISTEST-1396")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class QuotaRegionInactiveEditTest {

    private String offerId;

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
    private WalletSteps walletSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void openManagementAddPage() {
        apiSteps.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(SECONDARY))).create();
        offerId = offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(SECONDARY).withApartment(getRandomApartment())))
                .withInactive().create().getId();
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).open();
    }

    @Test
    @DisplayName("Редактирование не активного оффера в платном регионе без услуг. Редирект в ЛК")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeQuotaRegionInactiveEditOfferWithoutServiceRedirectLk() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(SAVE_AND_CONTINUE)).click();

        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().payButton().waitUntil(hasText(findPattern(ACTIVATE_FOR)));
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(UrlSteps.FROM_FORM_VALUE, ACTIVATION_VALUE).queryParam(TRAP_VALUE, TRUE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
        apiSteps.waitSearcherOfferStatusInactive(offerId);
    }

    @Test
    @DisplayName("Редактирование не активного оффера в платном регионе без услуг. Редирект в ЛК")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeQuotaRegionInactiveEditOfferWithServiceRedirectLk() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(SAVE_AND_CONTINUE)).click();

        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().selectPaySelector(PROMOTION);
        offerAddSteps.onOfferAddPage().publishTrap().payButton()
                .waitUntil(hasText(findPattern(ACTIVATE_WITH_OPTIONS_FOR)));
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        walletSteps.onWalletPage().cardsPopup().waitUntil(isDisplayed());
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(UrlSteps.FROM_FORM_VALUE, ACTIVATION_VALUE).queryParam(TRAP_VALUE, TRUE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
        apiSteps.waitSearcherOfferStatusInactive(offerId);
    }

    @Test
    @DisplayName("Редактирование не активного оффера в платном регионе. С блока Турбо редирект в форму оплаты")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeQuotaRegionInactiveEditOfferWithServiceRedirectToPay() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(SAVE_AND_CONTINUE)).click();

        offerAddSteps.onOfferAddPage().publishTrap().sellTab(FASTER_SALE).waitUntil(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishTrap().payButton()
                .waitUntil(hasText(findPattern(ACTIVATE_WITH_OPTIONS_FOR)));
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        walletSteps.onWalletPage().cardsPopup().waitUntil(isDisplayed());
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(UrlSteps.FROM_FORM_VALUE, ACTIVATION_VALUE).queryParam(TRAP_VALUE, TRUE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
        apiSteps.waitSearcherOfferStatusInactive(offerId);
    }
}