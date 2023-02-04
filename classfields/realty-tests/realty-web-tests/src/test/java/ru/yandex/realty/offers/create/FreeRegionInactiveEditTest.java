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
import ru.auto.test.api.realty.OfferType;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.REGION;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.EDIT_OFFER;
import static ru.yandex.realty.element.offers.PublishBlock.ACTIVE;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.page.OfferAddPage.CONTINUE_FREE;
import static ru.yandex.realty.page.OfferAddPage.FASTER_SALE;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PROMOTION;
import static ru.yandex.realty.page.OfferAddPage.PUBLISH_WITH_OPTIONS_FOR;
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
public class FreeRegionInactiveEditTest {

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
    private CompareSteps compareSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void openManagementAddPage() {
        apiSteps.createVos2Account(account, OWNER);
        offerId = offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(OfferType.APARTMENT_SELL)
                        .withLocation(getLocationForRegion(REGION)
                                .withApartment(getRandomApartment()))).withInactive().create().getId();
        compareSteps.resize(1280, 10000);
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).open();
    }

    @Test
    @DisplayName("Редактирование не активного оффера в регионе без услуг. Редирект в ЛК")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeFreeRegionInactiveEditOfferWithoutServiceRedirectLk() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(SAVE_AND_CONTINUE)).click();

        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().payButton().waitUntil(hasText(CONTINUE_FREE));
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(UrlSteps.FROM_FORM_VALUE, ACTIVATION_VALUE).queryParam(TRAP_VALUE, TRUE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
        apiSteps.waitSearcherOfferStatusInactive(offerId);
    }

    @Test
    @DisplayName("Редактирование не активного оффера в регионе без услуг. Редирект в ЛК")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeFreeRegionInactiveEditOfferWithServiceRedirectLk() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(SAVE_AND_CONTINUE)).click();

        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().selectPaySelector(PROMOTION);
        offerAddSteps.onOfferAddPage().publishTrap().payButton()
                .waitUntil(hasText(findPattern(PUBLISH_WITH_OPTIONS_FOR)));
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        walletSteps.onWalletPage().addCardFormIFrame().waitUntil(isDisplayed());
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(UrlSteps.FROM_FORM_VALUE, ACTIVATION_VALUE).queryParam(TRAP_VALUE, TRUE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
        apiSteps.waitSearcherOfferStatusInactive(offerId);
    }

    @Test
    @DisplayName("Редактирование не активного оффера в регионе. С блока Турбо редирект в форму оплаты")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeFreeRegionInactiveEditOfferWithServiceRedirectToPay() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(SAVE_AND_CONTINUE)).click();

        offerAddSteps.onOfferAddPage().publishTrap().sellTab(FASTER_SALE).waitUntil(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishTrap().payButton()
                .waitUntil(hasText(findPattern(PUBLISH_WITH_OPTIONS_FOR)));
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        walletSteps.onWalletPage().addCardFormIFrame().waitUntil(isDisplayed());
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(UrlSteps.FROM_FORM_VALUE, ACTIVATION_VALUE).queryParam(TRAP_VALUE, TRUE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
        apiSteps.waitSearcherOfferStatusInactive(offerId);
    }
}