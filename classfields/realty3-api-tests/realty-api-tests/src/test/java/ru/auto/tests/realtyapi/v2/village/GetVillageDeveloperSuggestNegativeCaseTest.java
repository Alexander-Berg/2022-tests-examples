package ru.auto.tests.realtyapi.v2.village;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
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
import ru.auto.tests.realtyapi.v2.model.RealtyVillageResponseDeveloperSuggest;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

@Title("GET /village/developerSuggest")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetVillageDeveloperSuggestNegativeCaseTest {

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

    @Parameterized.Parameters(name = "text={0} rgid={1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"ричн", 587795},
                {"A", 120894}
        };
    }


    @Test
    @Owner(ARTEAMO)
    public void shouldTwoRequestsSeeEmptyResponse() {
        RealtyVillageResponseDeveloperSuggest response = api.village().villageDeveloperSuggestRoute()
                .reqSpec(authSpec())
                .textQuery(text)
                .rgidQuery(rgid)
                .executeAs(validatedWith(shouldBe200Ok())).getResponse();

        MatcherAssert.assertThat(response, jsonEquals(new JsonObject()));
    }
}
