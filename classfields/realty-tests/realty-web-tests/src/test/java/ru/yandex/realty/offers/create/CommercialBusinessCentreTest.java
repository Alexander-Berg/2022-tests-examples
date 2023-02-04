package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
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
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.BUILDING_TYPE;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_PREMISES;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_REALTY;
import static ru.yandex.realty.consts.OfferAdd.DESTINATION;
import static ru.yandex.realty.consts.OfferAdd.OFFICE_CLASS;
import static ru.yandex.realty.consts.OfferAdd.OFFICE_NAME;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.model.offer.OfficeClass.A_PLUS;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 27.07.17.
 */
@DisplayName("Поле «Бизнес-центр» в попапе на странице добавления оффера")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CommercialBusinessCentreTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Before
    public void openManagementPage() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(COMMERCIAL_REALTY);
        offerAddSteps.onOfferAddPage().featureField(DESTINATION).selectButton(COMMERCIAL_PREMISES);
        offerAddSteps.onOfferAddPage().featureField(BUILDING_TYPE).selectButton("Бизнес-центр");
        offerAddSteps.fillRequiredFieldsForCommercial();
    }

    @Test
    @DisplayName("Выбираем в попапе тип здания - бизнес-центр, видим класс БЦ")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldAppearOfficeClassBlock() {
        offerAddSteps.onOfferAddPage().featureField(OFFICE_CLASS).should(isDisplayed());
    }

    @Test
    @DisplayName("Выбираем в попапе тип здания - бизнес-центр, видим название БЦ")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldAppearOfficeNameBlock() {
        offerAddSteps.onOfferAddPage().featureField(OFFICE_NAME).should(isDisplayed());
    }

    @Test
    @DisplayName("Вводим класс БЦ")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeOfficeClass() {
        offerAddSteps.onOfferAddPage().featureField(OFFICE_CLASS).selectButton("A+");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasOfficeClass("A_PLUS");
    }

    @Test
    @DisplayName("Вводим название БЦ")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeOfficeName() {
        String officeName = "default";
        offerAddSteps.onOfferAddPage().featureField(OFFICE_NAME).input().sendKeys(officeName);

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasBuildingName(officeName);
    }
}
