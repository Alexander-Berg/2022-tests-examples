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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.test.api.realty.OfferComercialType.RETAIL;
import static ru.auto.test.api.realty.OfferComercialType.SHOPPING_CENTER;
import static ru.auto.test.api.realty.RoomType.SEPARATE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.OfferAdd.BUILDING_TYPE;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_PREMISES;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_REALTY;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.DESTINATION;
import static ru.yandex.realty.consts.OfferAdd.ENTRANCE;
import static ru.yandex.realty.consts.OfferAdd.ROOMS_INDOORS;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.OfferAdd.SHOPPING_MALL;
import static ru.yandex.realty.consts.OfferAdd.VENT;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 26.07.17.
 */
@DisplayName("Добавление оффера для продать коммерческая")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateSellCommercialOfficeTest {

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
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(COMMERCIAL_REALTY);
        offerAddSteps.onOfferAddPage().featureField(DESTINATION).selectButton(COMMERCIAL_PREMISES);
        offerAddSteps.fillRequiredFieldsForCommercial();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем основное назначение")
    @Category({Regression.class, Testing.class})
    public void shouldSeeDestination() {

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasCommercialType(RETAIL.toString());

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(DESTINATION).button(COMMERCIAL_PREMISES).should(isChecked());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем тип здания")
    @Category({Regression.class, Testing.class})
    public void shouldSeeType() {
        offerAddSteps.onOfferAddPage().featureField(BUILDING_TYPE).selectButton(SHOPPING_MALL);

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific())
                .hasCommercialBuildingType(SHOPPING_CENTER.toString());

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(COMMERCIAL_TYPE).button(SHOPPING_MALL).should(isChecked());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Вводим количество комнат")
    @Category({Regression.class, Testing.class})
    public void shouldSeeNumberOfRooms() {
        long value = Utils.getRandomShortLong();

        offerAddSteps.onOfferAddPage().featureField(ROOMS_INDOORS).input().sendKeys(String.valueOf(value));

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasRoomsNumber(value);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(ROOMS_INDOORS).input().should(hasValue(String.valueOf(value)));
    }

    @Test
    @DisplayName("Вводим информацию про «Вход в помещение»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeEntrance() {
        offerAddSteps.onOfferAddPage().featureField(ENTRANCE).button("Отдельный").click();

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasEntranceType(SEPARATE.toString());

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(ENTRANCE).button("Отдельный").should(isChecked());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вводим информацию про вентиляцию")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVent() {
        offerAddSteps.onOfferAddPage().selectCheckBox(VENT);

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasVentilation(true);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().checkBox(VENT).should(isChecked());
    }
}
