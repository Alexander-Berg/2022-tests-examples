package ru.auto.tests.realtyapi.v2.village;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyVillageResponseDeveloperSuggestResponse;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getJsonArrayFromString;

@Title("GET /village/developerSuggest")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetVillageDeveloperSuggestPositiveCaseTest {

    private static final String EXPECTED_RICHNESS_STRING = "[{\"id\": 55798, \"name\": \"Richness Realty\"}]";
    private static final String EXPECTED_AMDEX_STRING = "[{\"id\": 1817922, \"name\": \"Amdex Group\"}]";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter
    @Parameterized.Parameter(0)
    public String text;

    @Parameter
    @Parameterized.Parameter(1)
    public long rgid;

    @Parameter
    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "text={0} rgid={1} expected={2}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"ричн", 417899, EXPECTED_RICHNESS_STRING},
                {"Richn", 741965, EXPECTED_RICHNESS_STRING},
                {"Амде", 587795, EXPECTED_AMDEX_STRING}
        };
    }


    @Test
    @Owner(ARTEAMO)
    public void shouldTwoRequestsHasSameResponse() {
        RealtyVillageResponseDeveloperSuggestResponse response = api.village().villageDeveloperSuggestRoute()
                .reqSpec(authSpec())
                .textQuery(text)
                .rgidQuery(rgid)
                .executeAs(validatedWith(shouldBe200Ok()));

        MatcherAssert.assertThat(response.getResponse().getSuggest(), jsonEquals(getJsonArrayFromString(expected)));
    }
}
