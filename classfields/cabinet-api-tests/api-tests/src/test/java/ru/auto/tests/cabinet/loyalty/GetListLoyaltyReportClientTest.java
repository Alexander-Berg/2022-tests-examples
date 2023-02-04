package ru.auto.tests.cabinet.loyalty;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import net.javacrumbs.jsonunit.core.Option;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /loyalty/report/client/{client_id}")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetListLoyaltyReportClientTest {

    private static final String MANAGER_ID = "19565983";
    private static final String USER_ID = "11296277";
    private static final String DEALER_ID = "20101";
    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetListLoyaltyReportHasNoDiffWithProduction() {
        Function<ApiClient, JsonArray> response = apiClient -> apiClient.loyalty().getClientReports()
                .clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(USER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonArray.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi))
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    public void shouldGetStatusOkForManager() {
        api.loyalty().getClientReports().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(MANAGER_ID)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        api.loyalty().getClientReports().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("1956598")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}