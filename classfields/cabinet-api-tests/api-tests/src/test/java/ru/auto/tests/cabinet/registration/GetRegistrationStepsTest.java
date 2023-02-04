package ru.auto.tests.cabinet.registration;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonArray;
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


@DisplayName("GET /client/{client_id}/registration/steps")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetRegistrationStepsTest {

    private static final String USER_ID = "11913489";
    private static final String DEALER_ID = "20101";

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetRegistrationStatusOk() {
        api.registration().getByClient().xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec())
                .clientIdPath(DEALER_ID).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetRegistrationErrorForbiddenBadUid() {
        api.registration().getByClient().xAutoruOperatorUidHeader("1129627")
                .reqSpec(defaultSpec())
                .clientIdPath(DEALER_ID).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetRegistrationErrorForbiddenBadClient() {
        api.registration().getByClient().xAutoruOperatorUidHeader("11296277")
                .reqSpec(defaultSpec())
                .clientIdPath("2010").execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetRegistrationErrorNotFoundBadClient() {
        api.registration().getByClient().xAutoruOperatorUidHeader("19565983")
                .reqSpec(defaultSpec())
                .clientIdPath("2010e").execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetRegistrationStepsHasNoDiffWithProduction() {
        Function<ApiClient, JsonArray> request = apiClient -> apiClient.registration().getByClient()
                .xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec())
                .clientIdPath(DEALER_ID).execute(validatedWith(shouldBeCode(SC_OK))).as(JsonArray.class);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}