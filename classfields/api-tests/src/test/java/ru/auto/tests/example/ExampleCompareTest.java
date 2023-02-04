package ru.auto.tests.example;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.example.anno.Prod;
import ru.auto.tests.example.module.ExampleApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.example.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.example.ResponseSpecBuilders.validatedWith;

@DisplayName("GET /..")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ExampleApiModule.class)
public class ExampleCompareTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void simpleCompareTest() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.store().getInventory().execute(validatedWith(shouldBeCode(SC_OK)))
                .as(JsonObject.class);
      assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}

