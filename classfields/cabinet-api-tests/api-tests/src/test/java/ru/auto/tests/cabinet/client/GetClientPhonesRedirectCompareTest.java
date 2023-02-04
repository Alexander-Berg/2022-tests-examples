package ru.auto.tests.cabinet.client;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;

@DisplayName("GET /client/{client_id}/phones/redirects")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetClientPhonesRedirectCompareTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetPhoneRedirectInfoHasNotDiffProduction() {

        Function<ApiClient, JsonObject> response = apiClient -> apiClient.client().getClientPhonesRedirects()
                .clientIdPath("16453").execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi)));
    }
}