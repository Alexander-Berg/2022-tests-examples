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
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequest;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestProduct;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestSubscribePurchase;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("POST /billing/{salesmanDomain}/payment/init")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class InitPaymentVinHistoryTest {

    private static final String DOMAIN = "autoru";
    private static final String OFFERS_HISTORY_REPORTS = "offers-history-reports";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager accountManager;

    @Test
    @Owner(TIMONDL)
    public void shouldSeeTicketIdForVinHistoryProductWithVin() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiBillingInitPaymentResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name(OFFERS_HISTORY_REPORTS).count(1))
                        .subscribePurchase(new AutoApiBillingInitPaymentRequestSubscribePurchase().count(1).vinOrLicensePlate("WBAFG61070LT71269")))
                .executeAs(validatedWith(shouldBe200OkJSON()));

        Assertions.assertThat(response.getTicketId()).isNotNull();
        Assertions.assertThat(response.getPaymentMethods()).isNotNull().isNotEmpty();
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeTicketIdForVinHistoryProductWithOfferId() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiBillingInitPaymentResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name(OFFERS_HISTORY_REPORTS).count(1))
                        .subscribePurchase(new AutoApiBillingInitPaymentRequestSubscribePurchase().count(1).offerId("1092041734-3db3f1a1")))
                .executeAs(validatedWith(shouldBe200OkJSON()));

        Assertions.assertThat(response.getTicketId()).isNotNull();
        Assertions.assertThat(response.getPaymentMethods()).isNotNull().isNotEmpty();
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutVinOrOfferId() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name(OFFERS_HISTORY_REPORTS).count(1))
                        .subscribePurchase(new AutoApiBillingInitPaymentRequestSubscribePurchase().count(1)))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(BAD_REQUEST)
                .hasDetailedError("missing vin_or_license_plate or offer_id fields");
    }
}
