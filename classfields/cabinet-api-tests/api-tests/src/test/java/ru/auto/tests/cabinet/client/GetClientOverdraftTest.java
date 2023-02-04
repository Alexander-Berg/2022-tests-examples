package ru.auto.tests.cabinet.client;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.model.DealerOverdraft;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /client/{client_id}/overdraft")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetClientOverdraftTest {

    private static final String DEALER_ID = "16453";
    private static final String USER_ID = "14090654";

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetAllowedTrue() {
        DealerOverdraft response = api.client().getClientOverdraft().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec()).averageOutcomeQuery("99999").executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getAllowed()).isEqualTo(true);
    }

    @Test
    public void shouldGetAllowedFalse() {
        DealerOverdraft response = api.client().getClientOverdraft().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec()).averageOutcomeQuery("1").executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getAllowed()).isEqualTo(false);
    }

    @Test
    public void shouldGetStatusOkForManager() {
        api.client().getClientOverdraft().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("19565983")
                .reqSpec(defaultSpec()).averageOutcomeQuery("1").execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        api.client().getClientOverdraft().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("1956598")
                .reqSpec(defaultSpec()).averageOutcomeQuery("1").execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}