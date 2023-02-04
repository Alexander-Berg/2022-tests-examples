package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiBillingPaymentStatusResponse.PaymentStatusEnum.NEW;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.TRANSACTION_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NoAuth;


@DisplayName("GET /billing/{salesmanDomain}/payment")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetPaymentStatusTest {

    private static final String DOMAIN = "autoru";
    private static final String PRODUCT = "boost";
    private static final Integer NOT_EXISTENT_TICKET_ID = 1;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private Account account;

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WithNoAuth() {
        api.billing().getPaymentStatus().salesmanDomainPath(DOMAIN).executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSession() {
        api.billing().getPaymentStatus().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .ticketIdQuery(NOT_EXISTENT_TICKET_ID)
                .execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutTicketId() {
        AutoApiErrorResponse response = api.billing().getPaymentStatus().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST);
        Assertions.assertThat(response.getDetailedError()).contains("Request is missing required query parameter 'ticket_id'");
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee404WithNotExistentTicketId() {
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.billing().getPaymentStatus().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .ticketIdQuery(NOT_EXISTENT_TICKET_ID)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(TRANSACTION_NOT_FOUND)
                .hasDetailedError(TRANSACTION_NOT_FOUND.getValue());
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeEmptySchedulesList() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String ticketId = adaptor.initPayment(sessionId, offerId, PRODUCT).getTicketId();

        AutoApiBillingPaymentStatusResponse scheduleResponse = api.billing().getPaymentStatus()
                .reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .ticketIdQuery(ticketId)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(scheduleResponse).hasPaymentStatus(NEW);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldNotSeeDifferenceWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String ticketId = adaptor.initPayment(sessionId, offerId, PRODUCT).getTicketId();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.billing().getPaymentStatus()
                .reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .ticketIdQuery(ticketId)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
