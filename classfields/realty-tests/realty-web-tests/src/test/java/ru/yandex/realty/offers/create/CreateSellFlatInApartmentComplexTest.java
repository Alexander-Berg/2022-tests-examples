package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.adaptor.SearcherAdaptor;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Location.SPB_AND_LO;
import static ru.yandex.realty.consts.OfferAdd.BUILT_YEAR;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.REASSIGNMENT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Добавление оффера продать квартиру")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateSellFlatInApartmentComplexTest {

    private static final String APARTMENT_COMPLEX = "проезд Берёзовая Роща, 3";
    private static final String SITEID = "56599";

    private String price;

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

    @Inject
    private SearcherAdaptor searcherAdaptor;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void openManagementAddPage() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        urlSteps.shouldUrl(containsString("add"));
        offerAddSteps.fillRequiredFieldsForSellFlat(APARTMENT_COMPLEX);
        offerAddSteps.onOfferAddPage().priceField().priceInput().click();
        offerAddSteps.onOfferAddPage().priceField().clearSign().click();
        price = "2" + RandomStringUtils.randomNumeric(6);
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
    }

    //TODO: убрать когда будут тесты на бэк
    @Test
    @DisplayName("Создаем оффер в ЖК-новостройке с типом сделки - переустпка, проверяем что есть на выдаче")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeAtListing() {
        offerAddSteps.onOfferAddPage().featureField(BUILT_YEAR).inputList().waitUntil(hasSize(greaterThan(0))).get(0)
                .clearSign().clickIf(isDisplayed());
        offerAddSteps.onOfferAddPage().featureField(BUILT_YEAR).input().sendKeys("2022");
        offerAddSteps.onOfferAddPage().featureField(BUILT_YEAR).selectCheckBox("Не сдан");
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(REASSIGNMENT);
        offerAddSteps.publish().waitPublish();
        String offerId = api.getOfferInfo(account).getOffer().getId();
        searcherAdaptor.waitOffer(offerId);
        passportSteps.logoff();
        urlSteps.testing().path(SPB_AND_LO.getPath()).path(KUPIT).path(KVARTIRA).queryParam("siteId", SITEID)
                .queryParam("priceMin", price).queryParam("priceMax", price).queryParam("showSimilar", "NO").open();
        basePageSteps.onOffersSearchPage().findOffer(offerId).should(isDisplayed());
    }
}