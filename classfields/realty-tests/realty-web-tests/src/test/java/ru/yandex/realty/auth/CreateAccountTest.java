package ru.yandex.realty.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static ru.yandex.qatools.htmlelements.matchers.common.IsElementEnabledMatcher.isEnabled;
import static ru.yandex.realty.consts.OfferAdd.EMAIL;
import static ru.yandex.realty.consts.OfferAdd.HOW_TO_ADDRESS;
import static ru.yandex.realty.consts.OfferAdd.PHONE;
import static ru.yandex.realty.consts.OfferAdd.TYPE_OF_ACCOUNT;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.AGENCY;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.getStaticOgrn;

/**
 * Created by ivanvan on 31.07.17.
 */
@DisplayName("Форма добавления объявления. Вход с yandex account.")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateAccountTest {

    private static final String AGENT = "Агент";

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

    @Before
    public void before() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        offerAddSteps.onOfferAddPage().contactInfo().featureField(HOW_TO_ADDRESS).input().sendKeys("Валера");
    }

    @Test
    @Owner(IVANVAN)
    @DisplayName("Заходим на страницу добавления оффера")
    @Description("Проверяем, что появляется кнопка «Собственник»")
    public void shouldSeeTypeButton() {
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).button("Собственник").waitUntil(isEnabled());
    }

    @Test
    @Issue("REALTY-12726")
    @Owner(IVANVAN)
    @DisplayName("Видим созданное объявление для «Агент»")
    public void shouldSeeAgentInBackEnd() {
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).button(AGENT).waitUntil(isEnabled()).click();
        offerAddSteps.publish().waitPublish();
        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasType(AccountType.AGENT.getValue());
    }

    @Test
    @Issue("REALTY-12726")
    @Owner(IVANVAN)
    @DisplayName("Видим созданное объявление для «Собственник»")
    public void shouldSeeOwnerInBackEnd() {
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).button("Собственник").waitUntil(isEnabled()).click();
        offerAddSteps.publish().waitPublish();
        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasType(OWNER.getValue());
    }

    @Test
    @Issue("REALTY-12726")
    @Owner(IVANVAN)
    @DisplayName("Видим созданное объявление для «Агентство»")
    public void shouldSeeAgencyInBackEnd() {
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).button("Агентство").waitUntil(isEnabled()).click();

        String phone = Utils.getRandomPhone();
        offerAddSteps.onOfferAddPage().contactInfo().input(PHONE, removeStart(phone, "7"));
        offerAddSteps.onOfferAddPage().contactInfo().ogrn().sendKeys(getStaticOgrn());
        offerAddSteps.normalPlacement().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasType(AGENCY.getValue());
    }

    @Test
    @Owner(IVANVAN)
    @DisplayName("Проверяем, что прокинулись поля")
    public void shouldSeeTypeEqualAgent() {
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).button(AGENT).waitUntil(isEnabled()).click();
        String name = Utils.getRandomString() + " " + Utils.getRandomString();
        String email = Utils.getRandomEmail();
        offerAddSteps.onOfferAddPage().contactInfo().featureField(HOW_TO_ADDRESS).input().click();
        offerAddSteps.onOfferAddPage().contactInfo().featureField(HOW_TO_ADDRESS).inputList().get(FIRST).clearSign().click();
        offerAddSteps.onOfferAddPage().contactInfo().featureField(HOW_TO_ADDRESS).input().sendKeys(name);
        offerAddSteps.onOfferAddPage().contactInfo().featureField(EMAIL).input().click();
        offerAddSteps.onOfferAddPage().contactInfo().featureField(EMAIL).inputList().get(FIRST).clearSign().click();
        offerAddSteps.onOfferAddPage().contactInfo().featureField(EMAIL).input().sendKeys(email);
        offerAddSteps.publish().waitPublish();
        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasType(AccountType.AGENT.getValue()).hasEmail(email).hasName(name);
    }

    @Test
    @Issue("REALTY-12726")
    @Owner(IVANVAN)
    @DisplayName("Проверяем, что поле ОГРН прокидывается")
    public void shouldSeeOGRN() {
        String ogrn = getStaticOgrn();
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).button(AGENT).waitUntil(isEnabled()).click();
        offerAddSteps.onOfferAddPage().contactInfo().ogrn().sendKeys(ogrn);
        offerAddSteps.normalPlacement().waitPublish();
        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasOgrn(ogrn);
    }
}
