package ru.auto.tests.publicapi.carfax.orders;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiVinOrdersCreateOrderRequest;
import ru.auto.tests.publicapi.model.AutoApiVinOrdersOrderResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.UUID;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.CARFAX;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("GET /carfax/orders/result")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetOrderResultTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(CARFAX)
    public void shouldSee401WhenNoSession() {
        api.carfax().getOrderResult()
                .reqSpec(defaultSpec())
                .orderIdQuery(UUID.randomUUID())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldSee403WhenNoAuth() {
        api.carfax().getOrderResult().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldHasNoDiffWithProductionTinkoffReport() {

        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        String orderId = api.carfax().createOrder()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .body(
                        new AutoApiVinOrdersCreateOrderRequest()
                                .identifier("Z0NZWE00054341234")
                                .identifierType(AutoApiVinOrdersCreateOrderRequest.IdentifierTypeEnum.VIN)
                                .reportId("tinkoff_insurance_report")
                )
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(AutoApiVinOrdersOrderResponse.class, GSON)
                .getOrder().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().getOrderResult()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .orderIdQuery(orderId)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        JsonObject resp = req.apply(api);
        JsonObject prodResp = req.apply(prodApi);

        MatcherAssert.assertThat(resp, jsonEquals(prodResp).whenIgnoringPaths(OrderUtils.IGNORED_PATHS));
    }

    @Test
    @Owner(CARFAX)
    public void shouldHasNoDiffWithProductionFullReport() {

        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().getOrderResult()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .orderIdQuery("6478914a-41db-46ec-9ee9-229e882d49b6") // existing succeeded report
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        JsonObject resp = req.apply(api);
        JsonObject prodResp = req.apply(prodApi);

        MatcherAssert.assertThat(resp, jsonEquals(prodResp).whenIgnoringPaths(OrderUtils.ignoredPaths("full_report")));
    }
}
