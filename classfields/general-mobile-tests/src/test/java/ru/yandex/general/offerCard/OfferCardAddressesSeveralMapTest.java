package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.beans.card.Address;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.beans.card.Address.address;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.beans.card.District.district;
import static ru.yandex.general.beans.card.GeoPoint.geoPoint;
import static ru.yandex.general.beans.card.MetroStation.metroStation;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.SHOW_MAP;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature("Отображение адреса в попапе с картой")
@DisplayName("Отображение адреса в попапе с картой")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class OfferCardAddressesSeveralMapTest {

    private static final String ID = "12345";
    private static final String ADDRESS_NAME = "Павелецкая площадь, 2с1";
    private static final String DISTRICT_NAME = "Замоскворечье";
    private static final String METRO_NAME = "Павелецкая";

    private static final Address ADDRESS = address()
            .setAddress(addressText().setAddress(ADDRESS_NAME))
            .setGeoPoint(geoPoint().setLatitude("55.730423").setLongitude("37.634796"));
    private static final Address METRO = address()
            .setGeoPoint(geoPoint().setLatitude("55.729797").setLongitude("37.638952"))
            .setMetroStation(metroStation().setId("20475").setName(METRO_NAME).setLineIds(asList("213_2"))
                    .setColors(asList("#4f8242")).setIsEnriched(false));
    private static final Address DISTRICT = address()
            .setGeoPoint(geoPoint().setLatitude("55.734157").setLongitude("37.63429"))
            .setDistrict(district().setId("117067").setName(DISTRICT_NAME).setIsEnriched(false));

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение двух адресов в попапе с картой")
    public void shouldSee2AddressesInMapPopup() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAddresses(ADDRESS, METRO).setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().spanLink(SHOW_MAP).click();

        basePageSteps.onOfferCardPage().popup().addressList().should(hasSize(2)).should(hasItems(
                hasText(containsString(ADDRESS_NAME)),
                hasText(containsString(format("м. %s", METRO_NAME)))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение двух пинов адресов в попапе с картой")
    public void shouldSee2AddressesPinsInMapPopup() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAddresses(ADDRESS, METRO).setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().spanLink(SHOW_MAP).click();

        basePageSteps.onOfferCardPage().popup().map().pinList().should(hasSize(2))
                .should(hasItems(
                        hasText("1"),
                        hasText("2")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение трех адресов в попапе с картой")
    public void shouldSee3AddressesInMapPopup() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAddresses(ADDRESS, METRO, DISTRICT).setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().spanLink(SHOW_MAP).click();

        basePageSteps.onOfferCardPage().popup().addressList().should(hasSize(3)).should(hasItems(
                hasText(containsString(ADDRESS_NAME)),
                hasText(containsString(format("м. %s", METRO_NAME))),
                hasText(containsString(DISTRICT_NAME))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение трех пинов адресов в попапе с картой")
    public void shouldSee3AddressesPinsInMapPopup() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAddresses(ADDRESS, METRO, DISTRICT).setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().spanLink(SHOW_MAP).click();

        basePageSteps.onOfferCardPage().popup().map().pinList().should(hasSize(3))
                .should(hasItems(
                        hasText("1"),
                        hasText("2"),
                        hasText("3")));
    }

}
