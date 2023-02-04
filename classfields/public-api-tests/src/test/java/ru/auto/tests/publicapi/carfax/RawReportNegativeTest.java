package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.VIN_CODE_INVALID;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /carfax/report/raw")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class RawReportNegativeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WhenNoAuth() {
        api.carfax().rawReport().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithInvalidVinOrLicensePlate() {
        String invalidVinOrLicensePlate = getRandomString();

        AutoApiErrorResponse response = api.carfax().rawReport()
                .reqSpec(defaultSpec())
                .vinOrLicensePlateQuery(invalidVinOrLicensePlate)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class);

        assertThat(response).hasStatus(ERROR)
                .hasError(VIN_CODE_INVALID)
                .hasDetailedError(format("%s doesn't look like vin", invalidVinOrLicensePlate));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithMissedVinOrLicensePlate() {
        AutoApiErrorResponse response = api.carfax().rawReport()
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class);

        assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST);
        Assertions.assertThat(response.getDetailedError())
                .contains("Request is missing required query parameter 'vin_or_license_plate'");
    }
}
