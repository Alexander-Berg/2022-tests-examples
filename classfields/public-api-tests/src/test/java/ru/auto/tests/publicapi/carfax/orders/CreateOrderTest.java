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
import ru.auto.tests.publicapi.carfax.RawReportUtils;
import ru.auto.tests.publicapi.model.AutoApiVinOrdersCreateOrderRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Arrays;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.CARFAX;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("POST /carfax/orders/create")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CreateOrderTest {

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
        api.carfax().createOrder()
                .reqSpec(defaultSpec())
                .body(new AutoApiVinOrdersCreateOrderRequest())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldSee403WhenNoAuth() {
        api.carfax().createOrder().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldHasNoDiffWithProductionGibddReport() {

        createdOrderShouldNoDiffWIthProd(
                "Z0NZWE00054341234",
                AutoApiVinOrdersCreateOrderRequest.IdentifierTypeEnum.VIN,
                AutoApiVinOrdersCreateOrderRequest.ReportTypeEnum.GIBDD_REPORT,
                null
        );
    }

    @Test
    @Owner(CARFAX)
    public void shouldHasNoDiffWithProductionTinkoffReport() {

        createdOrderShouldNoDiffWIthProd(
                "Z0NZWE00054341234",
                AutoApiVinOrdersCreateOrderRequest.IdentifierTypeEnum.VIN,
                null,
                "tinkoff_insurance_report"
        );
    }

    @Test
    @Owner(CARFAX)
    public void shouldHasNoDiffWithProductionCMEReport() {

        createdOrderShouldNoDiffWIthProd(
                "A355YY116",
                AutoApiVinOrdersCreateOrderRequest.IdentifierTypeEnum.LICENSE_PLATE,
                AutoApiVinOrdersCreateOrderRequest.ReportTypeEnum.CM_EXPERT_REPORT,
                null
        );

        createdOrderShouldNoDiffWIthProd(
                "Z0NZWE00054341234",
                AutoApiVinOrdersCreateOrderRequest.IdentifierTypeEnum.VIN,
                AutoApiVinOrdersCreateOrderRequest.ReportTypeEnum.CM_EXPERT_REPORT,
                null
        );
    }

    private void createdOrderShouldNoDiffWIthProd(
            String identifier,
            AutoApiVinOrdersCreateOrderRequest.IdentifierTypeEnum identifierType,
            AutoApiVinOrdersCreateOrderRequest.ReportTypeEnum reportType,
            String reportId) {

        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().createOrder()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .body(
                        new AutoApiVinOrdersCreateOrderRequest()
                                .identifier(identifier)
                                .identifierType(identifierType)
                                .reportType(reportType)
                                .reportId(reportId)
                )
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        JsonObject resp = req.apply(api);
        JsonObject prodResp = req.apply(prodApi);

        MatcherAssert.assertThat(resp, jsonEquals(prodResp).whenIgnoringPaths(OrderUtils.IGNORED_PATHS));
    }
}
