package ru.yandex.general.form;

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
import ru.yandex.general.beans.card.Card;
import ru.yandex.general.consts.FormConstants;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.GraphqlSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.card.Address.address;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.beans.card.District.district;
import static ru.yandex.general.beans.card.GeoPoint.geoPoint;
import static ru.yandex.general.beans.card.MetroStation.metroStation;
import static ru.yandex.general.beans.card.Region.region;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Conditions.NEW;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.page.ContactsPage.ADD_ADDRESS;
import static ru.yandex.general.page.FormPage.CONTINUE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@DisplayName("Размещение оффера с протянутыми из настроек сохраненными адресами")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class AddOfferSavedAddressesTest {

    private static final String REGION_ID = "65";
    private static final String ADDRESS = "проспект Карла Маркса, 2";
    private static final String METRO = "Золотая Нива";
    private static final String DISTRICT = "Советский район";

    private FormConstants.Categories category = PERENOSKA;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private GraphqlSteps graphqlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        urlSteps.testing().path(MY).path(CONTACTS).open();

        offerAddSteps.onContactsPage().addressesList().get(0).sendKeys(ADDRESS);
        offerAddSteps.onContactsPage().suggestItem(ADDRESS).waitUntil(isDisplayed()).click();

        offerAddSteps.onContactsPage().spanLink(ADD_ADDRESS).click();
        offerAddSteps.onContactsPage().addressesList().get(1).sendKeys(DISTRICT);
        offerAddSteps.onContactsPage().suggestItem(DISTRICT).waitUntil(isDisplayed()).click();

        offerAddSteps.onContactsPage().spanLink(ADD_ADDRESS).click();
        offerAddSteps.onContactsPage().addressesList().get(2).sendKeys(METRO);
        offerAddSteps.onContactsPage().suggestItem(METRO).waitUntil(isDisplayed()).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).withCondition(NEW).fillToPublishStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера, с протянутыми из настроек сохраненными адресами")
    public void shouldAddOfferWithSavedAddresses() {
        offerAddSteps.publish();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Address> addresses = asList(
                address()
                        .setAddress(addressText().setAddress(ADDRESS))
                        .setGeoPoint(geoPoint().setLatitude("54.98296").setLongitude("82.897568"))
                        .setMetroStation(metroStation().setName("Площадь Маркса").setColors(asList("#f03d2f")))
                        .setDistrict(district().setName("Ленинский район"))
                        .setRegion(region().setId(REGION_ID).setName("Новосибирск")),
                address()
                        .setGeoPoint(geoPoint().setLatitude("54.867653").setLongitude("83.082019"))
                        .setDistrict(district().setName(DISTRICT))
                        .setRegion(region().setId(REGION_ID).setName("Новосибирск")),
                address()
                        .setGeoPoint(geoPoint().setLatitude("55.037928").setLongitude("82.976044"))
                        .setMetroStation(metroStation().setName(METRO).setColors(asList("#23a12c")))
                        .setDistrict(district().setName("Октябрьский район"))
                        .setRegion(region().setId(REGION_ID).setName("Новосибирск"))
        );

        assertThatJson(offerCard.getContacts().getAddresses().toString()).isEqualTo(addresses.toString());

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SCREENSHOT_TESTS)
    @DisplayName("Скриншот черновика с протянутыми из настроек сохраненными адресами")
    public void shouldSeeSavedAddressesDraft() {
        compareSteps.resize(1920, offerAddSteps.getMaxPageHeight());
        offerAddSteps.refresh();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().h1().click();
        offerAddSteps.scrollToTop();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().h1().click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
