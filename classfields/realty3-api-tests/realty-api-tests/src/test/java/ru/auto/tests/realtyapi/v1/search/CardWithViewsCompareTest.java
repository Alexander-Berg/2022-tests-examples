package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getQueryMapParams;


@Title("GET /cardWithViews.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardWithViewsCompareTest {

    private static final String RESOURCE_FILE = "search/card_with_views_test_data.txt";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Parameter("Параметры")
    @Parameterized.Parameter
    public Map<String, String> queryParams;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() throws IOException {
        return getQueryMapParams(RESOURCE_FILE);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        String offerId = adaptor.getOfferIdFromSearcher();
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.search().cardWithViewsRoute()
                .reqSpec(authSpec())
                .reqSpec(req -> req.addQueryParams(queryParams))
                .idQuery(offerId)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        JsonObject testResponse = request.apply(api);
        JsonObject prodResponse = request.apply(prodApi);
        assertThat(
                testResponse,
                jsonEquals(prodResponse)
                        .whenIgnoringPaths(
                                "response.timeStamp",
                                "response.views",
                                "response.author.allowedCommunicationChannels"
                        )
        );
        assertThat(
                testResponse.getAsJsonObject("response").getAsJsonObject("author")
                        .getAsJsonArray("allowedCommunicationChannels"),
                jsonEquals(
                        prodResponse.getAsJsonObject("response").getAsJsonObject("author")
                                .getAsJsonArray("allowedCommunicationChannels")
                ).when(Option.IGNORING_ARRAY_ORDER)
        );
    }
}
