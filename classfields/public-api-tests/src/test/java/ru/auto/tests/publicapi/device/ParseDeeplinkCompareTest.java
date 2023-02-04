package ru.auto.tests.publicapi.device;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.builder.RequestSpecBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.api.DeviceApi;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Generated compare test for ParseDeeplink
 */
@DisplayName("GET /device/deeplink-parse")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParseDeeplinkCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter
    public Consumer<RequestSpecBuilder> reqSpec;

    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return newArrayList(req -> req.addQueryParam(DeviceApi.ParseDeeplinkOper.LINK_QUERY, "https://m.auto.ru/moskva/cars/audi/a4/used/?price_from=1000000&autoru_body_type=SEDAN")
//                   .addQueryParam(DeviceApi.ParseDeeplinkOper.TYPE_QUERY, "type_example")
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /device/deeplink-parse")
    public void shouldParseDeeplinkHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.device().parseDeeplink()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
