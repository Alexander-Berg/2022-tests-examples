package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiBillingTiedCard;
import ru.auto.tests.publicapi.model.AutoApiUserResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.rules.CreditCardRule;

import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.VertisBankerPaymentMethodCardProperties.BrandEnum.MASTERCARD;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("POST /billing/{salesmanDomain}/payment/process")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class AddCardTest {

    private static final long CARD_ID = 555554444L;
    private static final String CARD_MASK = "5555554444";
    private static final String CDD_PAN_MASK = "555555|4444";
    private static final String EXPIRE_YEAR = "2040";
    private static final String EXPIRE_MONTH = "12";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public CreditCardRule creditCardRule;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager accountManager;

    @Test
    @Owner(TIMONDL)
    public void shouldLinkBankCardToAccount() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        creditCardRule.addCreditCard(sessionId);

        AutoApiUserResponse userResponse = api.user().getCurrentUser().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        Assertions.assertThat(userResponse.getTiedCards())
                .as("Проверяем, что список карточек упользователя не пустой и == 1")
                .isNotEmpty().hasSize(1);
        assertThat(userResponse.getTiedCards().get(0))
                .as("Проверяем ID, MASK и PS_ID у карточки")
                .hasId(CARD_ID)
                .hasCardMask(CARD_MASK)
                .hasPsId(AutoApiBillingTiedCard.PsIdEnum.YANDEXKASSA_V3);
        assertThat(userResponse.getTiedCards().get(0).getProperties())
                .as("Проверяем PAN_MASK, BRAND, год и месяц у карточки")
                .hasCddPanMask(CDD_PAN_MASK)
                .hasBrand(MASTERCARD)
                .hasExpireMonth(EXPIRE_MONTH)
                .hasExpireYear(EXPIRE_YEAR);
    }
}
