package ru.auto.tests.publicapi.stats;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by scrooge on 02.02.18.
 */


@DisplayName("GET /stats/predict")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetPredictTest {

    private static final String RESOURCE_FILE = "negative_predict_test_data.txt";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Параметры")
    @Parameterized.Parameter
    public String body;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() throws IOException {
        InputStream dataStream = GetPredictTest.class.getClassLoader().getResourceAsStream(RESOURCE_FILE);
        return IOUtils.readLines(dataStream, UTF_8);
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithIncorrect() {
        AutoApiErrorResponse response = api.stats().predict()
                .reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class);

        assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR);
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithIncorrectForAnonym() {
        AutoApiErrorResponse response = api.stats().predict()
                .reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class);

        assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR);
    }
}
