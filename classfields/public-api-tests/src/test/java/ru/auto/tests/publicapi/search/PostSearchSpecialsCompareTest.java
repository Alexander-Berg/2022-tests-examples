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
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /search/spec")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PostSearchSpecialsCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public CategoryEnum category;

    @Parameter("Тело запроса")
    @Parameterized.Parameter(1)
    public AutoApiSearchSearchRequestParameters body;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        //todo: добавь параметры
        return newArrayList(new Object[][]{
                {CARS, new AutoApiSearchSearchRequestParameters()},
                {MOTO, new AutoApiSearchSearchRequestParameters()},
                {TRUCKS, new AutoApiSearchSearchRequestParameters()}});
    }

    @Test
    public void shouldSearchSpecialsHasNoDifferenceWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.search().specsPOST().categoryPath(category).pageQuery(1).pageSizeQuery(2)
                .body(body)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths("pagination.total_offers_count",
                        "pagination.total_page_count"));
    }
}
