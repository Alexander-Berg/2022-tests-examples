package ru.auto.tests.cabinet.loyalty;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import net.javacrumbs.jsonunit.core.Option;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /loyalty/report/client/{client_id}/current")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetCurrentReportTest {

    private static final String MANAGER_ID = "19565983";

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetCurrentReportHasNoDiffWithProduction() {
        String userId = "2464501";
        String dealerId = "4568";
        adaptor.deleteLoyaltyReports(dealerId);
        adaptor.addLoyaltyReports(dealerId);
        Function<ApiClient, JsonObject> response = apiClient -> apiClient.loyalty().getCurrentReport()
                .clientIdPath(dealerId).xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi))
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    public void shouldGetStatusOkForManager() {
        String dealerId = "20057";
        adaptor.deleteLoyaltyReports(dealerId);
        adaptor.addLoyaltyReports(dealerId);
        api.loyalty().getCurrentReport().clientIdPath(dealerId).xAutoruOperatorUidHeader(MANAGER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));

    }

    @Test
    public void shouldGetStatusForbidden() {
        String dealerId = "21313";
        adaptor.deleteLoyaltyReports(dealerId);
        adaptor.addLoyaltyReports(dealerId);
        api.loyalty().getCurrentReport().clientIdPath(dealerId).xAutoruOperatorUidHeader("1956598")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}