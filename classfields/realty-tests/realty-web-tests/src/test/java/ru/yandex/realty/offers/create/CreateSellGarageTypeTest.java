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
import static ru.yandex.realty.consts.OfferAdd.AUTOMATIC_GATES;
import static ru.yandex.realty.consts.OfferAdd.CAR_SERVICE;
import static ru.yandex.realty.consts.OfferAdd.CAR_WASHING;
import static ru.yandex.realty.consts.OfferAdd.FIRE_ALARM;
import static ru.yandex.realty.consts.OfferAdd.GARAGE;
import static ru.yandex.realty.consts.OfferAdd.GUARD;
import static ru.yandex.realty.consts.OfferAdd.INSPECTION_PIT;
import static ru.yandex.realty.consts.OfferAdd.PASS_SYSTEM;
import static ru.yandex.realty.consts.OfferAdd.SCHEDULE;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.OfferAdd.VIDEO_CONTROL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.utils.AccountType.OWNER;


@DisplayName("Добавление оффера продать гараж. Выбор типов")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateSellGarageTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Parameterized.Parameter
    public String buttonName;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;
    @Inject
    private OfferAddSteps offerAddSteps;

    @Parameterized.Parameter(1)
    public String offerInfoField;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {FIRE_ALARM, "fireAlarm"},
                {SCHEDULE, "twentyFourSeven"},
                {PASS_SYSTEM, "accessControlSystem"},
                {AUTOMATIC_GATES, "automaticGates"},
                {VIDEO_CONTROL, "cctv"},
                {GUARD, "security"},
                {INSPECTION_PIT, "inspectionPit"},
                {CAR_WASHING, "carWash"},
                {CAR_SERVICE, "autoRepair"},
        });
    }


    @Before
    public void before() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellGarage();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeSpecificForGarage() {
        offerAddSteps.onOfferAddPage().selectCheckBox(buttonName);
        offerAddSteps.publish().waitPublish();

        Boolean actual = api.getOfferInfo(account).getSpecific().get(offerInfoField);
        assertThat(actual).isTrue();
    }
}
