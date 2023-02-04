package ru.auto.tests.realtyapi.v1.schools;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /schools")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSchoolsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String leftLongitude;

    @Parameterized.Parameter(1)
    public String rightLongitude;

    @Parameterized.Parameter(2)
    public String topLongitude;

    @Parameterized.Parameter(3)
    public String bottomLongitude;

    @Parameterized.Parameter(4)
    public String count;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "leftLongitude={0} rightLongitude={1} topLongitude={2} bottomLongitude={3} count={4}")
    public static Collection<Object[]> getParameters() {
        return Collections.singletonList(new Object[]{"37.591413", "37.616132", "55.767574", "55.756274", "10"});
    }

    @Test
    public void shouldSchoolHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.schools().searchSchoolRoute()
                .reqSpec(authSpec())
                .leftLongitudeQuery(leftLongitude)
                .rightLongitudeQuery(rightLongitude)
                .topLatitudeQuery(topLongitude)
                .bottomLatitudeQuery(bottomLongitude)
                .countQuery(count)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
