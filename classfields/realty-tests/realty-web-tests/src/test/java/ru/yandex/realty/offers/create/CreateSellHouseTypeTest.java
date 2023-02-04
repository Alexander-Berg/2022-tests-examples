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
import static ru.yandex.realty.consts.OfferAdd.BILLIARD;
import static ru.yandex.realty.consts.OfferAdd.ELECTROSUPPLY;
import static ru.yandex.realty.consts.OfferAdd.GAS;
import static ru.yandex.realty.consts.OfferAdd.HEATING;
import static ru.yandex.realty.consts.OfferAdd.HOUSE;
import static ru.yandex.realty.consts.OfferAdd.KITCHEN_AVAILABLE;
import static ru.yandex.realty.consts.OfferAdd.PLUMBING;
import static ru.yandex.realty.consts.OfferAdd.PMG_ABILITY;
import static ru.yandex.realty.consts.OfferAdd.SAUNA;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.OfferAdd.SEWERAGE;
import static ru.yandex.realty.consts.OfferAdd.SWIMMINGPOOL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.utils.AccountType.OWNER;


@DisplayName("Добавление оффера для продать дом. Разные типы «Есть/Нет»")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateSellHouseTypeTest {

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
    public String buttonName;

    @Parameterized.Parameter(1)
    public String offerInfoField;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {SEWERAGE, "sewerageSupply"},
                {ELECTROSUPPLY, "electricitySupply"},
                {GAS, "gasSupply"},
                {BILLIARD, "billiard"},
                {SAUNA, "sauna"},
                {SWIMMINGPOOL, "pool"},
                {PMG_ABILITY, "pmg"},
                {KITCHEN_AVAILABLE, "kitchen"},
                {HEATING, "heatingSupply"},
                {PLUMBING, "waterSupply"}
        });
    }


    @Before
    public void before() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellHouse();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeSpecificForHouse() {
        offerAddSteps.onOfferAddPage().selectCheckBox(buttonName);
        offerAddSteps.publish().waitPublish();
        Boolean actual = api.getOfferInfo(account).getSpecific().get(offerInfoField);
        assertThat(actual).isTrue();
    }
}
