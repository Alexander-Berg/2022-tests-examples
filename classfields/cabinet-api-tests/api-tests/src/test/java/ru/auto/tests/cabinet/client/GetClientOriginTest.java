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
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;


@DisplayName("GET /client/{origin}")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetClientOriginTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetParameterClientHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.client().getClientByOrigin().originPath("msk3403")
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    public void shouldGetStatusErrorNotFound() {
        api.client().getClientByOrigin().originPath("msk3").execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetStatusErrorBadRequest() {
        api.client().getClientByOrigin().originPath("0").execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}