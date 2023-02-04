package ru.yandex.realty.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static ru.yandex.realty.consts.OfferAdd.HOW_TO_ADDRESS;
import static ru.yandex.realty.consts.OfferAdd.PHONE;
import static ru.yandex.realty.consts.OfferAdd.TYPE_OF_ACCOUNT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.utils.RealtyUtils.getStaticOgrn;

/**
 * Created by ivanvan on 15.08.17.
 */
@DisplayName("Форма добавления объявления. Агентство без привязанного телефона")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AgencyWithoutPhoneTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    private String phone = Utils.getRandomPhone();

    @Test
    @DisplayName("Проверяем, что сохраняется телефон")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Smoke.class, Testing.class})
    public void shouldSeePhone() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).selectButton("Агентство");
        offerAddSteps.onOfferAddPage().contactInfo().featureField(HOW_TO_ADDRESS).input().sendKeys("Валера");
        offerAddSteps.onOfferAddPage().contactInfo().ogrn().click();
        offerAddSteps.onOfferAddPage().contactInfo().ogrn().sendKeys(getStaticOgrn());
        offerAddSteps.onOfferAddPage().contactInfo().input(PHONE, removeStart(phone, "7"));
        offerAddSteps.normalPlacement().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasOnlyPhones("+" + phone);
    }
}
