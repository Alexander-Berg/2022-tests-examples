package ru.auto.tests.publicapi.stats;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withJsonBody;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by scrooge on 29.12.17.
 */


@DisplayName("GET /stats/predict")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PredictCompareTest {

    private static final String RESOURCE_FILE = "stats_predict_test_data.txt";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Параметры")
    @Parameterized.Parameter
    public String body;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() throws IOException {
        InputStream dataStream = PredictCompareTest.class.getClassLoader().getResourceAsStream(RESOURCE_FILE);
        return IOUtils.readLines(dataStream, UTF_8);
    }

    @Test
    @Owner(SCROOGE)
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.stats().predict()
                .reqSpec(defaultSpec())
                .reqSpec(withJsonBody(body))
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldHasNoDiffWithProductionForAnonym() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.stats().predict()
                .reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
