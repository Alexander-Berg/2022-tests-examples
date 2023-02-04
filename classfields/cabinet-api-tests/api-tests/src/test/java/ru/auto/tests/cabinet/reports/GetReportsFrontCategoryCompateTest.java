package ru.auto.tests.cabinet.reports;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /reports/{report_type}/source/client/{client_id}/category/{category}")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(CabinetApiModule.class)
public class GetReportsFrontCategoryCompateTest {

    private static final String DATE_REPORT = "2019-08";
    private static final String USER_ID = "11913489";
    private static final String DEALER_ID = "20101";
    private static final String TYPE_PDF = "pdf";

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Категория")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "[{index}] {0}")
    public static List<String> getCategories() {
        return newArrayList("cars:used", "cars:new", "commercial", "moto", "total");
    }

    @Test
    public void shouldGetReportFrontCategoryHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.reports().getReportSource()
                .reportTypePath(TYPE_PDF).clientIdPath(DEALER_ID).categoryPath(category)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader(USER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}