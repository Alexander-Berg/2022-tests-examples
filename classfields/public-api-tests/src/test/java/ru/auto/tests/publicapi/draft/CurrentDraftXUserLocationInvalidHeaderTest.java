package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_PARAMS_DETAILS;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.xUserLocationHeader;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("GET /user/draft/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CurrentDraftXUserLocationInvalidHeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Header")
    @Parameterized.Parameter(0)
    public String headerValue;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<String> getParameters() {
        return provideHeadersValue();
    }

    private static List<String> provideHeadersValue() {
        return newArrayList(
                "lat=59.9596322",
                "lon=59.9596322",
                "acc=23.419",
                "lat=59.9596322;lon=30.4064225"
        );
    }

    @Test
    public void shouldSee400WithInvalidUserLocationHeader() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiErrorResponse response = api.draft().currentDraft().categoryPath(CARS.name()).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .reqSpec(xUserLocationHeader(headerValue))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);
        assertThat(response).hasError(BAD_PARAMS_DETAILS)
                .hasStatus(ERROR)
                .hasDetailedError("Some of lat, lon or acc is not defined");
    }
}
