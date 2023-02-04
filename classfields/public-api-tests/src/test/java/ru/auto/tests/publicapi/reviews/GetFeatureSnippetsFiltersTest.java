package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
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
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /reviews/{subject}/featureSnippet/{category}/snippet")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetFeatureSnippetsFiltersTest {

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

    @Parameter("Название хатактеристики")
    @Parameterized.Parameter(1)
    public String feature;

    @Parameter("Марка")
    @Parameterized.Parameter(2)
    public String mark;

    @Parameter("Модель")
    @Parameterized.Parameter(3)
    public String model;

    @Parameter("Поколение")
    @Parameterized.Parameter(4)
    public String generation;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} - {1} - {2} - {3} - {4}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(featuresSnippetFilters());
    }

    private static Object[][] featuresSnippetFilters() {
        return new Object[][]{
                {CARS, "maintenance_cost", "VAZ", "2107", "2307270"},
                {MOTO, "gear", "YAMAHA", "YZF_R1", ""},
                {TRUCKS, "comfort", "FIAT_PROFESSIONAL", "DUCATO", ""}
        };
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldGetFeaturesHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.reviews().featureSnippet().subjectPath(AUTO).categoryPath(category).markQuery(mark)
                .modelQuery(model).superGenQuery(generation).featureQuery(feature).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
