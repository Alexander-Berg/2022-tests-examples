package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.offer.create.userid.Offer;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.time.LocalDateTime;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.test.api.realty.OfferType.APARTMENT_RENT_WITHOUT_SPECIFIC;
import static ru.yandex.realty.consts.OfferAdd.HAGGLE;
import static ru.yandex.realty.consts.OfferAdd.RENT_PLEDGE;
import static ru.yandex.realty.consts.OfferAdd.UTILITIES;
import static ru.yandex.realty.consts.OfferByRegion.Region.LOW;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.page.OfferAddPage.SAVE_AND_CONTINUE;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

/**
 * Created by ivanvan on 25.08.17.
 */
@DisplayName("Добавление оффера сдать квартиру.")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateRentRoomPricesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String buttonName;

    @Parameterized.Parameter(1)
    public String offerInfoField;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {RENT_PLEDGE, "rentPledge"},
                {UTILITIES, "utilitiesIncluded"},
                {HAGGLE, "haggle"},
        });
    }

    @Before
    public void openEditPage() {
        api.createVos2Account(account, OWNER);
        Offer body = OfferBuildingSteps.getDefaultOffer(APARTMENT_RENT_WITHOUT_SPECIFIC)
                .withLocation(getLocationForRegion(LOW));
        String id = offerBuildingSteps.addNewOffer(account)
                .withBody(body.withCreateTime(reformatOfferCreateDate(LocalDateTime.now().minusDays(1))))
                .create()
                .getId();

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(id).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeePriceSpecific() {
        offerAddSteps.onOfferAddPage().selectCheckBox(buttonName);
        offerAddSteps.onOfferAddPage().button(SAVE_AND_CONTINUE).click();
        offerAddSteps.waitPublish();
        Boolean actual = api.getOfferInfo(account).getSpecific().get(offerInfoField);
        assertThat(actual).isTrue();
    }
}
