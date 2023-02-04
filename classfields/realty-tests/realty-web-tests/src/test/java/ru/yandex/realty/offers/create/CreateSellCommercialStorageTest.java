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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.OfferAdd.AVAILABLE_LOGISTIC;
import static ru.yandex.realty.consts.OfferAdd.AVAILABLE_RAILWAYS;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_REALTY;
import static ru.yandex.realty.consts.OfferAdd.DESTINATION;
import static ru.yandex.realty.consts.OfferAdd.FREIGHT_ELEVATOR;
import static ru.yandex.realty.consts.OfferAdd.OFFICE_IN_STORAGE;
import static ru.yandex.realty.consts.OfferAdd.RAMP;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.OfferAdd.TRUCK_ENTRANCE;
import static ru.yandex.realty.consts.OfferAdd.WAREHOUSE_PREMISES;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 27.07.17.
 */
@DisplayName("Форма добавления объявления «продать - коммерческая». " +
        "С параметрамом «Складское помещение»")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateSellCommercialStorageTest {

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

    @Parameterized.Parameter
    public String service;

    @Parameterized.Parameter(1)
    public String offerInfoField;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {OFFICE_IN_STORAGE, "officeWarehouse"},
                {FREIGHT_ELEVATOR, "freightElevator"},
                {TRUCK_ENTRANCE, "truckEntrance"},
                {RAMP, "ramp"},
                {AVAILABLE_RAILWAYS, "railway"},
                {AVAILABLE_LOGISTIC, "serviceThreePl"}
        });
    }

    @Before
    public void openManagementAddPage() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(COMMERCIAL_REALTY);
        offerAddSteps.onOfferAddPage().featureField(DESTINATION).selectButton(WAREHOUSE_PREMISES);
        offerAddSteps.fillRequiredFieldsForCommercial();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeOfficeInStorage() {
        offerAddSteps.onOfferAddPage().selectCheckBox(service);
        offerAddSteps.publish().waitPublish();
        Boolean actual = api.getOfferInfo(account).getSpecific().get(offerInfoField);
        assertThat(actual).isTrue();
    }
}
