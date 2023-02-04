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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

/**
 * Created by vicdev on 07.11.17.
 */

@Title("GET /addressGeocoder.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddressGeocoderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Адрес")
    @Parameterized.Parameter(0)
    public String address;

    @Parameter("Долгота")
    @Parameterized.Parameter(1)
    public String longitude;

    @Parameter("Широта")
    @Parameterized.Parameter(2)
    public String latitude;


    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "address={0} longitude={1} latitude={2}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"Рубинштейна", "30.344749047607046", "59.930888161998205"},
                {"", "30.344749047607046", "59.930888161998205"},
                {"Рубинштейна", "", ""}
                //todo: добавить параметры
        });
    }

    @Test
    public void shouldNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.geo().addressGeocoderRoute()
                .reqSpec(authSpec())
                .addressQuery(address)
                .longitudeQuery(longitude)
                .latitudeQuery(latitude)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
