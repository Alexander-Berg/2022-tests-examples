package ru.auto.tests.cabinet.client;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;

@DisplayName("GET /client/{client_id}/phones")
@GuiceModules(CabinetApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetClientPhonesCompareTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public String category;

    @Parameter("Секция")
    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"cars", "used"},
                {"cars", "new"},
                {"moto", "used"},
                {"moto", "new"},
                {"trucks", "used"},
                {"trucks", "new"}
        });
    }

    @Test
    public void shouldGetPhoneInfoHasNotDiffProduction() {
        Function<ApiClient, JsonObject> response = apiClient -> apiClient.client().getClientPhones()
                .clientIdPath("16453").categoryQuery(category).sectionQuery(section)
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi)));
    }
}