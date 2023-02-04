package ru.auto.tests.realtyapi.v2.cabinet;

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
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.ME;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.getCabinetData;

@Title("GET /cabinet/{billing-domain}/user/{user}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetUserInfoCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String uid;

    @Parameterized.Parameter(1)
    public String clientId;

    @Parameterized.Parameter(2)
    public String domain;

    @Parameterized.Parameter(3)
    public String node;

    @Parameterized.Parameters(name = "uid={0} clientId={1} billing-domain={2} type={3}")
    public static Object[][] getParameters() {
        return getCabinetData();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldClientHasValidId() {
        String responseId = api.cabinet().userInfo()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(uid)
                .userPath(ME)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject(node)
                .get("id").getAsString();

        MatcherAssert.assertThat(responseId, equalTo(clientId));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.cabinet().userInfo()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .userPath(ME)
                .xUidHeader(uid)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
