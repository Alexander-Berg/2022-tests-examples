package ru.auto.tests.cabinet.access;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.model.AccessView;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;


@DisplayName("GET /access/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetAccessTest {

    private static final String DEALER_ID = "36645";
    private static final String MANAGER_ID = "19565983";

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetRoleManager() {
        AccessView response = api.access().checkAccess().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(MANAGER_ID)
                .xRequestIDHeader("1").executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getRole()).isEqualTo("manager");
    }

    @Test
    public void shouldGetRoleClient() {
        AccessView response = api.access().checkAccess().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("35142987")
                .xRequestIDHeader("1").executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getRole()).isEqualTo("client");
    }

    @Test
    public void shouldGetRoleAgent() {
        AccessView response = api.access().checkAccess().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("8769372")
                .xRequestIDHeader("1").executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getRole()).isEqualTo("agency");
    }

    @Test
    public void shouldGetRoleCompany() {
        AccessView response = api.access().checkAccess().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("23480672")
                .xRequestIDHeader("1").executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getRole()).isEqualTo("company");
    }

    @Test
    public void shouldGetRStatusForbidden() {
        api.access().checkAccess().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("2348067")
                .xRequestIDHeader("1").execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetRStatusNotFound() {
        api.access().checkAccess().clientIdPath("1q").xAutoruOperatorUidHeader(MANAGER_ID)
                .xRequestIDHeader("1").execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetRStatusBadRequest() {
        api.access().checkAccess().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("")
                .xRequestIDHeader("1").execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}