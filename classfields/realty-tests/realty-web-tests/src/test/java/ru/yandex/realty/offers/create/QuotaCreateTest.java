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
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.SECONDARY;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.element.offers.PublishBlock.ACTIVE;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PROMOTION;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Форма добавления объявления.")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@Issue("VERTISTEST-1396")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class QuotaCreateTest {


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
    private ManagementSteps managementSteps;

    @Before
    public void openManagementAddPage() {
        apiSteps.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(OfferType.APARTMENT_SELL)
                        .withLocation(getLocationForRegion(SECONDARY))).withSearcherWait().create();
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        urlSteps.shouldUrl(containsString("add"));
        offerAddSteps.fillRequiredFieldsForSellFlat(OfferAddSteps.DEFAULT_LOCATION);
    }

    @Test
    @DisplayName("Создание платного оффера с фото и без васов - показывается попап")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeQuotaWithPhotoToLk() {
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).clickWhile(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishBlock().deSelectPaySelector(PROMOTION);
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.waitPublish();
        managementSteps.onWalletPage().cardsPopup().should(isDisplayed());
    }

    @Test
    @DisplayName("Создание платного оффера с фото и с турбо - показывается попап")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeQuotaWithPhotoWithTurboToLk() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.waitPublish();
        managementSteps.onWalletPage().cardsPopup().should(isDisplayed());
    }

    @Test
    @DisplayName("Создание платного оффера с фото и с премиумом - показывается попап")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeQuotaWithPhotoWithPremiumToLk() {
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).clickWhile(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.waitPublish();
        managementSteps.onWalletPage().cardsPopup().should(isDisplayed());
    }
}