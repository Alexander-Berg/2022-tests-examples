package ru.auto.tests.cabinet.client;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;

import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;

@DisplayName("PUT /agency/{agency_id}/client/{client_id}/status/{status}")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PutAgencyClientStatusTest {

    private static final String AGENCY_ID = "19030";
    private static final String AGENCY_USER_ID = "14439810";

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldGetStatusDealerStopped() {
        String dealerId = "25596";
        String userId = "21487330";
        String status = "active";
        adaptor.setDealerSatatus(dealerId, status);
        api.client().changeClientStatus().agencyIdPath(AGENCY_ID).clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(AGENCY_USER_ID).xAutoruRequestIDHeader("1").statusPath("stopped")
                .execute(validatedWith(shouldBeCode(SC_OK)));
        JsonObject response = adaptor.getClientInfo(dealerId, userId);
        System.out.println(response);
        assertThat(response.getAsJsonObject("properties").get("status").getAsString()).isEqualTo("stopped");
    }

    @Test
    public void shouldGetStatusDealerActive() {
        String dealerId = "43100";
        String userId = "48732940";
        String status = "stopped";
        adaptor.setDealerSatatus(dealerId, status);
        api.client().changeClientStatus().agencyIdPath(AGENCY_ID).clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(AGENCY_USER_ID).xAutoruRequestIDHeader("1").statusPath("active")
                .execute(validatedWith(shouldBeCode(SC_OK)));
        JsonObject response = adaptor.getClientInfo(dealerId, userId);
        System.out.println(response);
        assertThat(response.getAsJsonObject("properties").get("status").getAsString()).isEqualTo("active");
    }


    @Test
    public void shouldGetStatusOkForManager() {
        String dealerId = "16636";
        String manager = "19565983";
        String status = "stopped";
        adaptor.setDealerSatatus(dealerId, status);
        api.client().changeClientStatus().agencyIdPath(AGENCY_ID).clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(manager).xAutoruRequestIDHeader("1").statusPath("active")
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        String dealerId = "16636";
        String manager = "1956598";
        String status = "stopped";
        adaptor.setDealerSatatus(dealerId, status);
        api.client().changeClientStatus().agencyIdPath(AGENCY_ID).clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(manager).xAutoruRequestIDHeader("1").statusPath("active")
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}