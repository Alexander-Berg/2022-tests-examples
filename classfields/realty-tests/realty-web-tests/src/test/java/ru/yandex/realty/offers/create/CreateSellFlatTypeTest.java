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
import static ru.yandex.realty.consts.OfferAdd.AIRCONDITION;
import static ru.yandex.realty.consts.OfferAdd.CLOSE_AREA;
import static ru.yandex.realty.consts.OfferAdd.CONCIERGE;
import static ru.yandex.realty.consts.OfferAdd.FRIDGE;
import static ru.yandex.realty.consts.OfferAdd.INTERNET;
import static ru.yandex.realty.consts.OfferAdd.KITCHEN_FURNITURE;
import static ru.yandex.realty.consts.OfferAdd.LIFT;
import static ru.yandex.realty.consts.OfferAdd.ROOM_FURNITURE;
import static ru.yandex.realty.consts.OfferAdd.RUBBISH_CHUTE;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 03.08.17.
 */
@DisplayName("Форма добавления объявления. Добавление оффера продать квартиру. Поля с кнопками «Есть», «Нет»")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateSellFlatTypeTest {

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
                {INTERNET, "internet"},
                {FRIDGE, "refrigerator"},
                {KITCHEN_FURNITURE, "kitchenFurniture"},
                {AIRCONDITION, "aircondition"},
                {ROOM_FURNITURE, "roomFurniture"},
                {LIFT, "lift"},
                {RUBBISH_CHUTE, "rubbishChute"},
                {CONCIERGE, "alarm"},
                {CLOSE_AREA, "passBy"}
        });
    }


    @Before
    public void openManagementAddPage() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSpecificForFlat() {
        offerAddSteps.onOfferAddPage().selectCheckBox(service);
        offerAddSteps.publish().waitPublish();
        Boolean actual = api.getOfferInfo(account).getSpecific().get(offerInfoField);
        assertThat(actual).isTrue();
    }
}
