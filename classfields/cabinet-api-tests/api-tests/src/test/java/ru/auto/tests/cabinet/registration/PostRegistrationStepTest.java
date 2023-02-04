package ru.auto.tests.cabinet.registration;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.RegistrationStep;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("POST /client/{client_id}/registration/step/{step}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class PostRegistrationStepTest {

    private static final String STEP_POI = "poi";
    private static final String STEP_REQUISITES = "requisites";
    private static final String STEP_TARIFF = "tariff";


    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public  void shouldSeeStepPoiconfirmedTrue() {
        String dealerId = "98";
        String userId = "754967";
        adaptor.clearRegistrationSteps(dealerId);
        api.registration().postForClient().xAutoruOperatorUidHeader(userId)
                .clientIdPath(dealerId).reqSpec(defaultSpec()).stepPath(STEP_POI)
                .execute(validatedWith(shouldBeCode(SC_OK)));
        List<RegistrationStep> response = api.registration().getByClient().xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec())
                .clientIdPath(dealerId).executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getAllowed()).isEqualTo(true);
        assertThat(response.get(0).getConfirmed()).isEqualTo(true);
        assertThat(response.get(1).getAllowed()).isEqualTo(true);
        assertThat(response.get(1).getConfirmed()).isEqualTo(false);
        assertThat(response.get(2).getAllowed()).isEqualTo(false);
        assertThat(response.get(2).getConfirmed()).isEqualTo(false);
    }

    @Test
    public  void shouldSeeStepRequisitesConfirmedTrue() {
        String dealerId = "222";
        String userId = "8786846";
        adaptor.clearRegistrationSteps(dealerId);
        adaptor.setRegistrationStepsConfirmed(dealerId,userId, STEP_POI);
        api.registration().postForClient().xAutoruOperatorUidHeader(userId)
                .clientIdPath(dealerId).reqSpec(defaultSpec()).stepPath(STEP_REQUISITES)
                .execute(validatedWith(shouldBeCode(SC_OK)));
        List<RegistrationStep> response = api.registration().getByClient().xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec())
                .clientIdPath(dealerId).executeAs(validatedWith(shouldBeCode(SC_OK)));
        System.out.println(response);

        assertThat(response.get(0).getAllowed()).isEqualTo(true);
        assertThat(response.get(0).getConfirmed()).isEqualTo(true);
        assertThat(response.get(1).getAllowed()).isEqualTo(true);
        assertThat(response.get(1).getConfirmed()).isEqualTo(true);
        assertThat(response.get(2).getAllowed()).isEqualTo(true);
        assertThat(response.get(2).getConfirmed()).isEqualTo(false);
    }

    @Test
    public  void shouldSeeStepTariffConfirmedTrue() {
        String dealerId = "232";
        String userId = "490593";
        adaptor.clearRegistrationSteps(dealerId);
        adaptor.setRegistrationStepsConfirmed(dealerId,userId, STEP_POI);
        adaptor.setRegistrationStepsConfirmed(dealerId,userId, STEP_REQUISITES);
        api.registration().postForClient().xAutoruOperatorUidHeader(userId)
                .clientIdPath(dealerId).reqSpec(defaultSpec()).stepPath(STEP_TARIFF)
                .execute(validatedWith(shouldBeCode(SC_OK)));
        List<RegistrationStep> response = api.registration().getByClient().xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec())
                .clientIdPath(dealerId).executeAs(validatedWith(shouldBeCode(SC_OK)));
        System.out.println(response);

        assertThat(response.get(0).getAllowed()).isEqualTo(true);
        assertThat(response.get(0).getConfirmed()).isEqualTo(true);
        assertThat(response.get(1).getAllowed()).isEqualTo(true);
        assertThat(response.get(1).getConfirmed()).isEqualTo(true);
        assertThat(response.get(2).getAllowed()).isEqualTo(true);
        assertThat(response.get(2).getConfirmed()).isEqualTo(true);
    }

    @Test
    public  void shouldSeeErrorStepRequisites() {
        String dealerId = "20101";
        adaptor.clearRegistrationSteps(dealerId);
        JsonObject response = api.registration().postForClient().xAutoruOperatorUidHeader("11296277")
                .clientIdPath(dealerId).reqSpec(defaultSpec()).stepPath(STEP_REQUISITES)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN))).as(JsonObject.class, ObjectMapperType.GSON);

        assertThat(response.get("errorCode").getAsString()).isEqualTo("AccessDenied");
        assertThat(response.get("message").getAsString())
                .isEqualTo(String.format("Step requisites is not allowed yet for %s", dealerId));

    }

    @Test
    public void shouldGetConfirmedStepPoi() {
        api.registration().postForClient().xAutoruOperatorUidHeader("906442")
                .clientIdPath("564").reqSpec(defaultSpec())
                .stepPath(STEP_POI).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetErrorBadUid() {
        api.registration().postForClient().xAutoruOperatorUidHeader("1129627")
                .clientIdPath("20101").reqSpec(defaultSpec())
                .stepPath(STEP_POI).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetErrorBadClient() {
        api.registration().postForClient().xAutoruOperatorUidHeader("11296277")
                .clientIdPath("2011").reqSpec(defaultSpec())
                .stepPath(STEP_POI).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetErrorBadStep() {
        api.registration().postForClient().xAutoruOperatorUidHeader("19565983")
                .clientIdPath("23334444e").reqSpec(defaultSpec())
                .stepPath(STEP_POI).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}