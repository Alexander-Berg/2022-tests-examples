package ru.auto.tests.cabinet.agreement;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.model.AgreementParams;
import ru.auto.tests.cabinet.model.UpdateResult;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /client/{client_id}/offer/agreement")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class PostAgreementForClientTest {

    private static final String DEALER_ID = "20101";

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetIdLine() {
        UpdateResult response = api.agreement().postForClient().clientIdPath(DEALER_ID)
                .xAutoruOperatorUidHeader("11913489").reqSpec(defaultSpec())
                .body(new AgreementParams().offerId(0L)).executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getRowCount()).isEqualTo(1);
        assertThat(response.getId()).isNotNull();
    }

    @Test
    public void shouldGetStatusOkForManager() {
        api.agreement().postForClient().clientIdPath(DEALER_ID)
                .xAutoruOperatorUidHeader("11913489").reqSpec(defaultSpec())
                .body(new AgreementParams().offerId(0L)).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        api.agreement().postForClient().clientIdPath(DEALER_ID)
                .xAutoruOperatorUidHeader("1191348").reqSpec(defaultSpec())
                .body(new AgreementParams().offerId(0L)).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}