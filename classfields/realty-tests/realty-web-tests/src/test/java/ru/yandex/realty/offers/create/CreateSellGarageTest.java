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
import static ru.auto.test.api.realty.OfferGarageType.METAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.OfferAdd.MATERIAL;
import static ru.yandex.realty.consts.OfferAdd.NAME_GSK;
import static ru.yandex.realty.consts.OfferAdd.PARKING_KIND;
import static ru.yandex.realty.consts.OfferAdd.STATUS;
import static ru.yandex.realty.consts.OfferAdd.TYPE;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.model.offer.GarageType.PARKING_PLACE;
import static ru.yandex.realty.model.offer.OwnershipType.COOPERATIVE;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 25.07.17.
 */
@DisplayName("Добавление оффера продать гараж")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateSellGarageTest {

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
    public void openManagementAddPage() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellGarage();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Нажимаем на кнопку «Машиноместо»")
    @Category({Regression.class, Testing.class})
    public void shouldSeeNewButton() {
        offerAddSteps.onOfferAddPage().featureField(TYPE).selectButton("Машиноместо");

        offerAddSteps.onOfferAddPage().featureField(PARKING_KIND).button("Наземная парковка").should(isDisplayed());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Вводим тип гаража")
    @Category({Regression.class, Testing.class})
    public void shouldSeeType() {
        offerAddSteps.onOfferAddPage().featureField(TYPE).selectButton("Машиноместо");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific().getGarage()).hasType("PARKING_PLACE");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(TYPE).button("Машиноместо").should(isChecked());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Вводим материал - металл")
    @Category({Regression.class, Testing.class})
    public void shouldSeeMaterial() {
        offerAddSteps.onOfferAddPage().featureField(MATERIAL).selectButton("Металлический");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasBuildingType(METAL.name());

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(MATERIAL).button("Металлический").should(isChecked());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Вводим статус")
    @Category({Regression.class, Testing.class})
    public void shouldSeeStatus() {
        offerAddSteps.onOfferAddPage().featureField(STATUS).selectButton("Кооператив");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific().getGarage()).hasOwnership("COOPERATIVE");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(STATUS).button("Кооператив").should(isChecked());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Вводим название ГСК")
    @Category({Regression.class, Testing.class})
    public void shouldSeeNameOfGsk() {
        String gsk = "default";
        offerAddSteps.onOfferAddPage().featureField(NAME_GSK).input().sendKeys(gsk);

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific().getGarage()).hasName(gsk);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(NAME_GSK).input().should(hasValue(gsk));
    }

}
