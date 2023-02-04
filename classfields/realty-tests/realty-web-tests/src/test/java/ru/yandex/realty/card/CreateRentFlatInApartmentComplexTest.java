package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_RENT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Location.SPB_AND_LO;
import static ru.yandex.realty.consts.OfferByRegion.Region.JK_BUILDING;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Добавление оффера сдать квартиру.")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateRentFlatInApartmentComplexTest {

    private static final String SITEID = "56599";
    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;
    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openManagementAddPage() {
        api.createVos2AccountWithoutLogin(account, OWNER);
        offerId = offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_RENT)
                .withLocation(getLocationForRegion(JK_BUILDING))).withSearcherWait().create().getId();
    }

    //TODO: убрать когда будут тесты на бэк
    @Test
    @DisplayName("Создаем оффер для сдачи в ЖК, проверяем что есть на выдаче")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeePanelHouse() {
        urlSteps.testing().path(SPB_AND_LO.getPath()).path(SNYAT).path(KVARTIRA).queryParam("siteId", SITEID)
                .queryParam("priceMin", "1000000").queryParam("priceMax", "1000000").open();
        basePageSteps.onOffersSearchPage().findOffer(offerId).should(isDisplayed());
    }
}