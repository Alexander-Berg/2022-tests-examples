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
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestSubscribePurchase.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestSubscribePurchase.SectionEnum.USED;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.PRODUCT_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NoAuth;


@DisplayName("POST /billing/{salesmanDomain}/payment/init")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class InitPaymentNegativeTest {

    private static final String DOMAIN = "autoru";

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
    public void shouldSee403WithNoAuth() {
        api.billing().initPayment().salesmanDomainPath(DOMAIN).executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSession() {
        api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .body(new AutoApiBillingInitPaymentRequest())
                .execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutBody() {
        AutoApiErrorResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST);
        Assertions.assertThat(response.getDetailedError()).contains("Expect message object but got: null");
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutCarInfoAtSingleVinHistoryReport() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name("offers-history-reports").count(1))
                        .subscribePurchase(new AutoApiBillingInitPaymentRequestSubscribePurchase().count(1)))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST).hasDetailedError("missing vin_or_license_plate or offer_id fields");
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithInvalidCountAtOffersHistoryReports() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name("offers-history-reports").count(1))
                        .subscribePurchase(new AutoApiBillingInitPaymentRequestSubscribePurchase().count(2).category(CARS).section(USED)))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST);
    }
}
