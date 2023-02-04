package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequest;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestAutoruPurchase;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestProduct;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("POST /billing/{salesmanDomain}/payment/init")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class InitPaymentAutoruPurchaseTest {

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

    @Parameterized.Parameter
    public String productName;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static List<String> getParameters() {
        return newArrayList("boost", "highlighting", "special-offer", "top", "vip-package", "turbo-package", "express-package", "badge");
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeTicketIdForOfferProducts() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        AutoApiBillingInitPaymentResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name(productName))
                        .autoruPurchase(new AutoApiBillingInitPaymentRequestAutoruPurchase().offerId(offerId).prolongable(false)))
                .executeAs(validatedWith(shouldBe200OkJSON()));

        Assertions.assertThat(response.getTicketId()).isNotNull();
        Assertions.assertThat(response.getPaymentMethods()).isNotNull().isNotEmpty();
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutOfferId() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name("boost"))
                        .autoruPurchase(new AutoApiBillingInitPaymentRequestAutoruPurchase().prolongable(false)))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(BAD_REQUEST)
                .hasDetailedError("Incorrect offer id: []");
    }
}
