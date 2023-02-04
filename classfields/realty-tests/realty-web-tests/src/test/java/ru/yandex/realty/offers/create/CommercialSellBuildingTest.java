package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
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
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_PREMISES;
import static ru.yandex.realty.consts.OfferAdd.COMMERCIAL_REALTY;
import static ru.yandex.realty.consts.OfferAdd.DESTINATION;
import static ru.yandex.realty.consts.OfferAdd.POSSIBLE_DESTINATION;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.model.offer.Purpose.BEAUTY_SHOP;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 27.07.17.
 */
@DisplayName("Форма добавления объявления «продать - коммерческая». " +
        "С параметрамом  «Торговое помещение».")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Story(CREATE_OFFER)
public class CommercialSellBuildingTest {

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

    @Test
    @DisplayName("Нажимаем на чекбокс «Салон красоты»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeNewButton() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(COMMERCIAL_REALTY);
        offerAddSteps.onOfferAddPage().featureField(DESTINATION).selectButton(COMMERCIAL_PREMISES);
        offerAddSteps.fillRequiredFieldsForCommercial();
        offerAddSteps.onOfferAddPage().featureField(POSSIBLE_DESTINATION).selectCheckBox("Салон красоты");


        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasPurpose("BEAUTY_SHOP");
    }
}
