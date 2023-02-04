package ru.yandex.general.contacts;

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
import ru.yandex.general.mock.MockCurrentUser;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.beans.card.Address.address;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.beans.card.District.district;
import static ru.yandex.general.beans.card.GeoPoint.geoPoint;
import static ru.yandex.general.beans.card.MetroStation.metroStation;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Input.VALUE;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;

@Epic(CONTACTS_FEATURE)
@Feature("Отображение адресов")
@DisplayName("Список адресов на странице контактов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class ContactsAddressesViewTest {

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

    private MockCurrentUser currentUser = currentUserExample();
    private MockResponse mockResponse;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockResponse = mockResponse()
                .setCategoriesTemplate()
                .setRegionsTemplate();

        urlSteps.testing().path(MY).path(CONTACTS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавлено адресов")
    public void shouldSeeNoAddresses() {
        mockRule.graphqlStub(mockResponse.setCurrentUser(currentUser.addAddresses().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().addressesList()
                .should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, "")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавлен один адрес")
    public void shouldSeeOneAddress() {
        mockRule.graphqlStub(mockResponse.setCurrentUser(currentUser.addAddresses(ADDRESS).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().addressesList()
                .should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, ADDRESS_NAME)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавлено три адреса каждого типа")
    public void shouldSeeThreeAddresses() {
        mockRule.graphqlStub(mockResponse.setCurrentUser(currentUser.addAddresses(ADDRESS, METRO, DISTRICT).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().addressesList()
                .should(hasSize(3))
                .should(hasItems(
                        hasAttribute(VALUE, ADDRESS_NAME),
                        hasAttribute(VALUE, METRO_NAME),
                        hasAttribute(VALUE, DISTRICT_NAME)));
    }

}
