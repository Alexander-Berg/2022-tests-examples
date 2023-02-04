package ru.auto.tests.publicapi.search;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters.StateGroupEnum.ALL;
import static ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters.StateGroupEnum.NEW;
import static ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters.StateGroupEnum.USED;
import ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters;
import static ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters.MotoCategoryEnum.ATV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /search/moto")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PostSearchMotoCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Тело запроса")
    @Parameterized.Parameter
    public AutoApiSearchSearchRequestParameters body;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<AutoApiSearchSearchRequestParameters> getParameters() {
        //todo: добавь параметры
        return newArrayList(
                new AutoApiSearchSearchRequestParameters().stateGroup(ALL),
                new AutoApiSearchSearchRequestParameters().stateGroup(USED),
                new AutoApiSearchSearchRequestParameters().stateGroup(NEW),
                new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("HONDA")).motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(ATV)),
                new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("HONDA#ATC_200X")).motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(ATV))
        );
    }

    @Test
    public void shouldSearchMotoHasNoDifferenceWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.search().postSearchMoto()
                .pageQuery(1).pageSizeQuery(2).body(body)
                .sortQuery("fresh_relevance_1-desc")
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths(

                )
        );
    }
}
