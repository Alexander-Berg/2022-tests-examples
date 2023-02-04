package ru.yandex.realty.managementnew;


import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.OfferType;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_RENT;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_RENT;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_SELL;
import static ru.auto.test.api.realty.OfferType.HOUSE_RENT;
import static ru.auto.test.api.realty.OfferType.HOUSE_SELL;
import static ru.auto.test.api.realty.OfferType.LOT_SELL;
import static ru.auto.test.api.realty.OfferType.ROOM_RENT;
import static ru.auto.test.api.realty.OfferType.ROOM_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.OfferByRegion.Region.LOW;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Частные лица. Отображение различных типов недвижимости")
@Issue("VERTISTEST-818")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferRealtyTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Parameterized.Parameter
    public OfferType realtyType;

    @Parameterized.Parameter(1)
    public String offerInfo;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {APARTMENT_SELL, "55 м², 1-комнатная квартира"},
                {APARTMENT_RENT, "100 м², студия"},
                {ROOM_SELL, "40 м², комната в 10-комнатной квартире"},
                {ROOM_RENT, "40 м², комната в 5-комнатной квартире"},
                {HOUSE_SELL, "100 м², дом с уч."},
                {HOUSE_RENT, "100 м², дом с уч."},
                {COMMERCIAL_SELL, "130 м², офис"},
                {COMMERCIAL_RENT, "130 м², офис"},
                {LOT_SELL, "77 м², участок"}
        });
    }

    @Before
    public void openManagementPage() {
        api.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(realtyType)
                        .withLocation(getLocationForRegion(LOW))).withSearcherWait().create();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("У созданного объявления корректно отображен соответствующий тип недвижимости")
    public void shouldSeeOfferRealtyType() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().offerLink().should(hasText(offerInfo));
    }
}
