package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.OfferAdd.APART_HOUSE;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.BOX;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_REALTY;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE_DIRECT;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.FLOOR;
import static ru.yandex.realty.consts.OfferAdd.FOR_OFFICE;
import static ru.yandex.realty.consts.OfferAdd.GARAGE;
import static ru.yandex.realty.consts.OfferAdd.HOUSE;
import static ru.yandex.realty.consts.OfferAdd.HOUSE_TYPE;
import static ru.yandex.realty.consts.OfferAdd.PRIVATE;
import static ru.yandex.realty.consts.OfferAdd.PURPOSE;
import static ru.yandex.realty.consts.OfferAdd.ROOM;
import static ru.yandex.realty.consts.OfferAdd.ROOMS_TOTAL;
import static ru.yandex.realty.consts.OfferAdd.ROOM_IN_DEAL;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.OfferAdd.STATUS;
import static ru.yandex.realty.consts.OfferAdd.TYPE;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.element.offers.FeatureField.SECOND;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by vicdev on 01.06.17.
 */

@DisplayName("Форма добавления объявления. Добавление оффера для всех категорий, где заполнены необходимые поля")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Story(CREATE_OFFER)
public class CreateDefaultOfferTest {

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
    public void createAccount() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @DisplayName("Заполняем дефолтные поля для продажи квартиры")
    @Description("Проверяем, что объявление создается, все поля прокидываются")
    @Owner(IVANVAN)
    public void shouldCreateDefaultOfferForSellFlat() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);

        long price = Utils.getRandomShortLong();
        long floor = Utils.getRandomShortLong();
        long area = Utils.getRandomShortLong() + 10;

        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.setFlat("1");
        offerAddSteps.onOfferAddPage().featureField(ROOMS_TOTAL).selectButton("2");

        fillFloor(floor);

        offerAddSteps.onOfferAddPage().featureField(AREA).input().sendKeys(String.valueOf(area));
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);

        offerAddSteps.publish();
        offerAddSteps.waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasPrice(price).hasFloors(floor)
                .hasFloorsTotal(floor + 1).hasAreaValue(area);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();

        offerAddSteps.onOfferAddPage().locationControls().suggest().should(hasValue(DEFAULT_LOCATION));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(FIRST).should(hasValue(String.valueOf(floor)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(SECOND).should(hasValue(String.valueOf(floor + 1)));
        offerAddSteps.onOfferAddPage().featureField(AREA).input().should(hasValue(String.valueOf(area)));
        offerAddSteps.onOfferAddPage().priceField().priceInput().should(hasValue(String.valueOf(price)));
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @DisplayName("Заполняем дефолтные поля для 'продать - комнату'")
    @Description("Проверяем, что объявление создается, все поля прокидываются")
    @Owner(IVANVAN)
    public void shouldCreateDefaultOfferForSellRoom() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(ROOM);
        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);

        long price = Utils.getRandomShortLong();
        long floor = Utils.getRandomShortLong();
        long area = Utils.getRandomShortLong() + 10;

        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);

        fillFloor(floor);

        offerAddSteps.onOfferAddPage().featureField(ROOMS_TOTAL).selectButton("2");
        offerAddSteps.onOfferAddPage().featureField(ROOM_IN_DEAL).selectButton("1");

        offerAddSteps.onOfferAddPage().featureField(AREA).input().sendKeys(String.valueOf(area));
        offerAddSteps.onOfferAddPage().featureField(AREA).input(SECOND).sendKeys(String.valueOf(area));
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);

        offerAddSteps.publish();
        offerAddSteps.waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasPrice(price).hasFloors(floor)
                .hasFloorsTotal(floor + 1).hasAreaValue(area);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().locationControls().suggest().should(hasValue(DEFAULT_LOCATION));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(FIRST).should(hasValue(String.valueOf(floor)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(SECOND).should(hasValue(String.valueOf(floor + 1)));
        offerAddSteps.onOfferAddPage().featureField(AREA).input().should(hasValue(String.valueOf(area)));
        offerAddSteps.onOfferAddPage().priceField().priceInput().should(hasValue(String.valueOf(price)));
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @DisplayName("Заполняем дефолтные поля для 'продать - дом'")
    @Description("Проверяем, что объявление создается, все поля прокидываются")
    @Owner(IVANVAN)
    public void shouldCreateDefaultOfferForSellHouse() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(HOUSE);
        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.onOfferAddPage().featureField(HOUSE_TYPE).selectButton(APART_HOUSE);
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);

        long price = Utils.getRandomShortLong();
        long area = Utils.getRandomShortLong();

        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().featureField(AREA).input().sendKeys(String.valueOf(area));
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasPrice(price).hasAreaValue(area);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().locationControls().suggest().should(hasValue(DEFAULT_LOCATION));
        offerAddSteps.onOfferAddPage().featureField(AREA).input().should(hasValue(String.valueOf(area)));
        offerAddSteps.onOfferAddPage().priceField().priceInput().should(hasValue(String.valueOf(price)));
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @DisplayName("Заполняем дефолтные поля для 'продать - гараж'")
    @Description("Проверяем, что объявление создается, все поля прокидываются")
    @Owner(IVANVAN)
    public void shouldCreateDefaultOfferForSellGarage() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(GARAGE);
        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL);

        long price = Utils.getRandomShortLong();

        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
        offerAddSteps.onOfferAddPage().featureField(TYPE).selectButton(BOX);
        offerAddSteps.onOfferAddPage().featureField(STATUS).selectButton(PRIVATE);


        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasPrice(price);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().locationControls().suggest().should(hasValue(DEFAULT_LOCATION));
        offerAddSteps.onOfferAddPage().priceField().priceInput().should(hasValue(String.valueOf(price)));
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @DisplayName("Заполняем дефолтные поля для 'продать - офис'")
    @Description("Проверяем, что объявление создается, все поля прокидываются")
    @Owner(IVANVAN)
    public void shouldCreateDefaultOfferForSellOffice() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(COMMERCIAL_REALTY);
        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL);
        offerAddSteps.onOfferAddPage().featureField(PURPOSE).selectButton(FOR_OFFICE);

        long price = Utils.getRandomShortLong();
        long area = Utils.getRandomShortLong();
        long floor = Utils.getRandomShortLong();

        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().featureField(AREA).input().sendKeys(String.valueOf(area));
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));

        fillFloor(floor);

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific())
                .hasPrice(price).hasAreaValue(area).hasFloorsTotal(floor + 1).hasFloors(floor);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().locationControls().suggest().should(hasValue(DEFAULT_LOCATION));
        offerAddSteps.onOfferAddPage().priceField().priceInput().should(hasValue(String.valueOf(price)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(FIRST).should(hasValue(String.valueOf(floor)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(SECOND).should(hasValue(String.valueOf(floor + 1)));
        offerAddSteps.onOfferAddPage().featureField(AREA).input().should(hasValue(String.valueOf(area)));
    }

    @Step("Заполняем этажность")
    private void fillFloor(long floor) {
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(FIRST, String.valueOf(floor));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(SECOND, String.valueOf(floor + 1));
    }
}
