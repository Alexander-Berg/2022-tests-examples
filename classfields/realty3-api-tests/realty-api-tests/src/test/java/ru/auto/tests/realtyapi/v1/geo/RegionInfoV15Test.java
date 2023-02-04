package ru.auto.tests.realtyapi.v1.geo;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.trueFalse;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.validRgids;

/**
 * Created by vicdev on 08.11.17.
 */

@Title("GET /regionInfoV15.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RegionInfoV15Test {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("rgid")
    @Parameterized.Parameter(0)
    public String rgid;

    @Parameter("showOnMobile")
    @Parameterized.Parameter(1)
    public boolean showOnMobile;

    @Parameterized.Parameters(name = "rgid={0} showOnMobile={1}")
    public static List<Object[]> getParameters() {
        List<Object[]> parameters = new ArrayList<>();

        Arrays.stream(validRgids())
                .forEach(r -> trueFalse().forEach(m -> parameters.add(new Object[] {String.valueOf(r), m})));

        return parameters;
    }

    @Test
    public void shouldNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.geo().getRegionInfoRoute()
                .reqSpec(authSpec())
                .rgidQuery(rgid)
                .showOnMobileQuery(showOnMobile)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
