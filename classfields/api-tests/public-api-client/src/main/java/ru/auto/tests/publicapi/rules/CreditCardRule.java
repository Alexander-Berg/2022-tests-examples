package ru.auto.tests.publicapi.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.mapper.ObjectMapperType;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.adaptor.kassa.KassaResponseBody;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequest;
import ru.auto.tests.publicapi.model.AutoApiBillingProcessPaymentRequest;
import ru.auto.tests.publicapi.model.AutoApiBillingProcessPaymentRequestAccountRefill;
import ru.auto.tests.publicapi.model.AutoApiUserResponse;
import ru.auto.tests.publicapi.model.VertisBankerPaymentRequestPayGateContext;
import ru.auto.tests.publicapi.model.VertisBankerPaymentRequestPayGateContextYandexKassaContext;
import ru.auto.tests.publicapi.model.VertisBankerPaymentRequestPayGateContextYandexKassaContextPaymentData;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.publicapi.model.AutoApiBillingProcessPaymentRequestAccountRefill.PsIdEnum.YANDEXKASSA_V3;
import static ru.auto.tests.publicapi.model.VertisBankerPaymentRequestPayGateContextYandexKassaContext.ConfirmationTypeEnum.EXTERNAL;
import static ru.auto.tests.publicapi.model.VertisBankerPaymentRequestPayGateContextYandexKassaContextPaymentData.TypeEnum.BANK_CARD;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

public class CreditCardRule extends ExternalResource {

    private static final String DOMAIN = "autoru";
    private static final String PS_METHOD_ID = "bank_card";
    private static final long AMOUNT = 100L;

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    public CreditCardRule() {}

    protected void after() {
        accountKeeper.get().forEach(this::unlinkAllCreditCards);
    }

    @Step("Привязываем банковскую карточку к пользователю {sessionId}")
    public void addCreditCard(String sessionId) {
        api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest().accountRefillPurchase(new Object()))
                .execute(validatedWith(shouldBe200OkJSON()));

        String token = getTokenFromYandexKassa();
        AutoApiBillingProcessPaymentRequestAccountRefill billingAccountRefill = buildBillingAccountRefill(token);

        api.billing().processPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingProcessPaymentRequest().accountRefill(billingAccountRefill))
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем токен от Яндекс.Кассы")
    private String getTokenFromYandexKassa() {
        return RestAssured.given().filter(new AllureRestAssured())
                .header("content-type", "application/json")
                .header("x-ym-user-agent", "Yandex.Checkout.SDK.yandex-checkout-ui/0.2.11 Web")
                .body(getResourceAsString("kassa/token_request_body.json"))
                .post("https://payment.yandex.net/payment-api/checkout-js/api/v1/tokenize")
                .body().as(KassaResponseBody.class, ObjectMapperType.GSON)
                .getResult().getPaymentToken();
    }

    @Step("Удаляем банковские карточки у пользователя {account.login}:{account.password} ({account.id})")
    private void unlinkAllCreditCards(Account account) {
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiUserResponse userResponse = api.user().getCurrentUser().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        if (userResponse.getTiedCards() != null) {
            userResponse.getTiedCards().forEach(creditCard -> {
                String cddPanMask = creditCard.getProperties().getCddPanMask();

                api.billing().removeTiedCard().reqSpec(defaultSpec())
                        .xSessionIdHeader(sessionId)
                        .salesmanDomainPath("autoru")
                        .cardIdQuery(cddPanMask)
                        .paymentSystemIdQuery(YANDEXKASSA_V3.getValue().toLowerCase())
                        .execute(validatedWith(shouldBe200Ok()));
            });
        }
    }

    private AutoApiBillingProcessPaymentRequestAccountRefill buildBillingAccountRefill(String token) {
        VertisBankerPaymentRequestPayGateContextYandexKassaContextPaymentData bankerYandexKassaContextData =
                new VertisBankerPaymentRequestPayGateContextYandexKassaContextPaymentData().type(BANK_CARD);

        VertisBankerPaymentRequestPayGateContextYandexKassaContext bankerYandexKassaContext =
                new VertisBankerPaymentRequestPayGateContextYandexKassaContext()
                        .data(bankerYandexKassaContextData).paymentToken(token).save(true).confirmationType(EXTERNAL);

        VertisBankerPaymentRequestPayGateContext bankerPayGateContext = new VertisBankerPaymentRequestPayGateContext()
                .yandexKassaContext(bankerYandexKassaContext);

        return new AutoApiBillingProcessPaymentRequestAccountRefill()
                .psId(YANDEXKASSA_V3).psMethodId(PS_METHOD_ID).amount(AMOUNT)
                .payGateContext(bankerPayGateContext);
    }
}
