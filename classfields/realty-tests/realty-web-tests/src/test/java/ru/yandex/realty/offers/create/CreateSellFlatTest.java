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
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.BALCONY;
import static ru.yandex.realty.consts.OfferAdd.BUILT_YEAR;
import static ru.yandex.realty.consts.OfferAdd.HEIGHT_CEILING;
import static ru.yandex.realty.consts.OfferAdd.HOUSE_TYPE;
import static ru.yandex.realty.consts.OfferAdd.PARKING_TYPE;
import static ru.yandex.realty.consts.OfferAdd.REPAIR;
import static ru.yandex.realty.consts.OfferAdd.STATUS;
import static ru.yandex.realty.consts.OfferAdd.TOILET;
import static ru.yandex.realty.consts.OfferAdd.WINDOWS_LOOK_AT;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.element.offers.FeatureField.SECOND;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.model.offer.BalconyType.LOGGIA;
import static ru.yandex.realty.model.offer.BathroomUnitType.TWO_AND_MORE;
import static ru.yandex.realty.model.offer.BuildingType.PANEL;
import static ru.yandex.realty.model.offer.ParkingType.UNDERGROUND;
import static ru.yandex.realty.model.offer.Renovation.DESIGNER_RENOVATION;
import static ru.yandex.realty.model.offer.WindowView.YARD;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 19.07.17.
 */
@DisplayName("Форма добавления объявления. Добавление оффера продать квартиру")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateSellFlatTest {

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
        urlSteps.shouldUrl(containsString("add"));
        offerAddSteps.fillRequiredFieldsForSellFlat();
    }

    @Test
    @DisplayName("Кликаем кнопку «Панель»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeePanelHouse() {
        offerAddSteps.onOfferAddPage().featureField(HOUSE_TYPE).selectButton("Панель");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasBuildingType("PANEL");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(HOUSE_TYPE).button("Панель").should(isChecked());
    }

    @Test
    @DisplayName("Вводим год постройки")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeBuiltYear() {
        long value = 1999 + Utils.getRandomShortLong();

        offerAddSteps.onOfferAddPage().featureField(BUILT_YEAR).inputList().waitUntil(hasSize(greaterThan(0)))
                .get(0).clearSign().clickIf(WebElementMatchers.isDisplayed());
        offerAddSteps.onOfferAddPage().featureField(BUILT_YEAR).input().sendKeys(String.valueOf(value));

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasBuiltYear(value);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(BUILT_YEAR).input().should(hasValue(String.valueOf(value)));
    }

    @Test
    @DisplayName("Кликаем кнопку «Подземная» в поле «Парковка»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeParkingType() {
        offerAddSteps.onOfferAddPage().featureField(PARKING_TYPE).selectButton("Подземная");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasParkingType("UNDERGROUND");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(PARKING_TYPE).button("Подземная").should(isChecked());
    }

    @Test
    @DisplayName("Указывем информацию про балкон")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeBalcony() {
        offerAddSteps.onOfferAddPage().featureField(BALCONY).selectButton("Лоджия");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasBalcony("LOGGIA");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(BALCONY).button("Лоджия").should(isChecked());
    }

    @Test
    @DisplayName("Указывем информацию про туалет")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeToilet() {
        offerAddSteps.onOfferAddPage().featureField(TOILET).selectButton("Более одного");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasBathroomUnit("TWO_AND_MORE");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(TOILET).button("Более одного").should(isChecked());
    }

    @Test
    @DisplayName("Указывем информацию про высоту потолков")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeCeilingHeight() {
        long value = Utils.getRandomShortLong() + 1;

        offerAddSteps.onOfferAddPage().featureField(HEIGHT_CEILING).input().sendKeys(String.valueOf(value));

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasCeilingHeight((double) value);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(HEIGHT_CEILING).input().should(hasValue(String.valueOf(value)));
    }

    @Test
    @DisplayName("Указывем информацию про ремонт")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeRepair() {
        offerAddSteps.onOfferAddPage().featureField(REPAIR).selectButton("Дизайнерский");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasRenovation("DESIGNER_RENOVATION");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(REPAIR).button("Дизайнерский").should(isChecked());
    }

    @Test
    @DisplayName("Указывем информацию вид из окна")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeWindowView() {
        offerAddSteps.onOfferAddPage().featureField(WINDOWS_LOOK_AT).selectCheckBox("Во двор");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasWindowView("YARD");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(WINDOWS_LOOK_AT).checkBox("Во двор").should(isChecked());
    }

    @Test
    @DisplayName("Указывем информацию о статусе недвижимости")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeRealtyStatus() {
        offerAddSteps.onOfferAddPage().featureField(STATUS).selectButton("Жилой фонд");

        offerAddSteps.publish().waitPublish();
        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasPropertyType("LIVING");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(STATUS).button("Жилой фонд").should(isChecked());
    }

    @Test
    @DisplayName("Указывем информацию жилой площади")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeRoomsArea() {
        long value = Utils.getRandomShortLong();

        offerAddSteps.onOfferAddPage().featureField(AREA).input(SECOND).sendKeys(String.valueOf(value));

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasLivingSpaceValue(value);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(AREA).input(SECOND).should(hasValue(String.valueOf(value)));
    }
}