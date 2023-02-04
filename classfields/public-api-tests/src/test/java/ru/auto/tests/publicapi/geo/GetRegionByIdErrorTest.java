package ru.auto.tests.publicapi.geo;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.REGION_NOT_FOUND;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /geo/{regionId}")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetRegionByIdErrorTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter
    @Parameterized.Parameter(0)
    public String regionId;


    @Parameterized.Parameters
    public static Object[] getParameters() {
        return new String[]{
                "0",
                "10000000000000000",
        };
    }

    @Test
    public void shouldSeeError() {
        AutoApiErrorResponse response = api.geo().regionInfo().regionIdPath(regionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);

        AutoApiErrorResponseAssert.assertThat(response).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR)
                .hasError(REGION_NOT_FOUND).hasDetailedError(REGION_NOT_FOUND.name());
    }
}
