package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.beans.card.Address;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
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
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature("Отображение адреса")
@DisplayName("Отображение адреса")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardAddressesTest {

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockCard mockCard;

    @Parameterized.Parameter(2)
    public String addressInSidebar;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Адрес", mockCard(BASIC_CARD).setAddresses(ADDRESS), ADDRESS_NAME},
                {"Район", mockCard(BASIC_CARD).setAddresses(DISTRICT), DISTRICT_NAME},
                {"Метро", mockCard(BASIC_CARD).setAddresses(METRO), format("м. %s", METRO_NAME)},
                {"Адрес + район + метро", mockCard(BASIC_CARD).setAddresses(ADDRESS, METRO, DISTRICT),
                        format("%s + 2 адреса", ADDRESS_NAME)},
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение адреса для покупателя")
    public void shouldSeeAddressBuyerCard() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().address().should(hasText(addressInSidebar));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение адреса для продавца")
    public void shouldSeeAddressOwnerCard() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setIsOwner(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().address().should(hasText(addressInSidebar));
    }

}
