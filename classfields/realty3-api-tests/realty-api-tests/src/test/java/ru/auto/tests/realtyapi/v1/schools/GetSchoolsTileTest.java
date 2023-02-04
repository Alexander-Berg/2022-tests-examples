package ru.auto.tests.realtyapi.v1.schools;


import com.carlosbecker.guice.GuiceModules;
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

import static org.hamcrest.CoreMatchers.is;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /schools/tile")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSchoolsTileTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String x;

    @Parameterized.Parameter(1)
    public String y;

    @Parameterized.Parameter(2)
    public String z;

    @Parameterized.Parameter(3)
    public String scale;


    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "x={0} y={1} z={2} scale={3}")
    public static Collection<Object[]> getParameters() {
        return Collections.singletonList(new Object[]{"1236", "641", "11", "1"});
    }

    @Test
    public void shouldOfferHasNoDiffWithProduction() {
        Function<ApiClient, byte[]> request = apiClient -> apiClient.schools().schoolTileRoute()
                .reqSpec(authSpec())
                .xQuery(x)
                .yQuery(y)
                .zQuery(z)
                .scaleQuery(scale)
                .execute(validatedWith(shouldBe200Ok())).asByteArray();

        MatcherAssert.assertThat(request.apply(api), is(request.apply(prodApi)));
    }
}
