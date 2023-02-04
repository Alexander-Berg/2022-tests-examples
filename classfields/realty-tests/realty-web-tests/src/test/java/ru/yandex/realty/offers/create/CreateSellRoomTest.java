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

import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE_DIRECT;
import static ru.yandex.realty.consts.OfferAdd.FLOOR;
import static ru.yandex.realty.consts.OfferAdd.ROOM;
import static ru.yandex.realty.consts.OfferAdd.ROOMS_TOTAL;
import static ru.yandex.realty.consts.OfferAdd.ROOM_IN_DEAL;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.OfferAdd.STATUS;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.element.offers.FeatureField.SECOND;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Форма добавления объявления. Добавление оффера для продать комнату")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateSellRoomTest {

    private static final long ROOMS_IN_DEAL = 2;
    private static final long MIN_AREA = 10;

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
    public void openSellRoomPage() {
        long price = Utils.getRandomShortLong();
        long floor = Utils.getRandomShortLong();
        long area = MIN_AREA + Utils.getRandomShortLong();

        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(ROOM);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);


        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(FIRST, String.valueOf(floor));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(SECOND, String.valueOf(floor + 1));
        offerAddSteps.onOfferAddPage().featureField(ROOMS_TOTAL).selectButton(String.valueOf(ROOMS_IN_DEAL + 1));
        offerAddSteps.onOfferAddPage().featureField(ROOM_IN_DEAL).selectButton(String.valueOf(ROOMS_IN_DEAL));
        offerAddSteps.onOfferAddPage().featureField(AREA).input(FIRST).sendKeys(String.valueOf(area + 1));
        offerAddSteps.onOfferAddPage().featureField(AREA).input(SECOND).sendKeys(String.valueOf(area));
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));

    }

    @Test
    @Owner(KURAU)
    @DisplayName("Создание объявления")
    @Category({Regression.class, Testing.class})
    public void shouldSeeRoomsOffered() {

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific())
                .hasRoomsOffered(ROOMS_IN_DEAL);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(ROOM_IN_DEAL).button(String.valueOf(ROOMS_IN_DEAL)).should(isChecked());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Проверяем чекбокс «Апартаменты»")
    @Category({Regression.class, Testing.class})
    public void shouldSeeApartment() {
        offerAddSteps.onOfferAddPage().featureField(STATUS).selectButton("Апартаменты");
        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasApartments(true);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(STATUS).button("Апартаменты").should(isChecked());
    }
}
