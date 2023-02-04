package ru.yandex.realty.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithProxyModuleWithoutPhone;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.OfferAdd.HOW_TO_ADDRESS;
import static ru.yandex.realty.consts.OfferAdd.TYPE_OF_ACCOUNT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;

/**
 * Created by ivanvan on 15.08.17.
 */
@DisplayName("Форма добавления объявления. Агент без привязанного телефона")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModuleWithoutPhone.class)
public class AgentWithoutPhoneTest {

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

    @Inject
    private ProxySteps proxySteps;

    @Test
    @Issue("REALTY-12726")
    @DisplayName("Проверяем телефон")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Smoke.class, Testing.class})
    public void shouldSeeAgentPhone() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).selectButton("Агент");
        offerAddSteps.onOfferAddPage().contactInfo().featureField(HOW_TO_ADDRESS).input().sendKeys("Валера");
        String phone = proxySteps.fillAndConfirmPhone();
        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasOnlyPhones("+" + phone);
    }
}
