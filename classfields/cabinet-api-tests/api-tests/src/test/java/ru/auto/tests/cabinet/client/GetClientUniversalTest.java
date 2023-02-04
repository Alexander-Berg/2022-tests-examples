package ru.auto.tests.cabinet.client;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /client/{client_id}/universal")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetClientUniversalTest {

    private static final String DEALER_ID = "34694";
    private static final String MANAGER_ID = "19565983";


    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetInfoClientUniversalHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.client().getClientUniversal()
                .clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("32984704").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    public void shouldGetStatusOkForAgency() {
        api.client().getClientUniversal().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("14439810")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusOkForManager() {
        api.client().getClientUniversal().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(MANAGER_ID)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        api.client().getClientUniversal().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("1956598")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetStatusNotFound() {
        api.client().getClientUniversal().clientIdPath("1").xAutoruOperatorUidHeader(MANAGER_ID)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}