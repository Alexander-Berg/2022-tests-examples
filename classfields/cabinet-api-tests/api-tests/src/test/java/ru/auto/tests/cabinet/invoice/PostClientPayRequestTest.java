package ru.auto.tests.cabinet.invoice;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.PaymentRequestParams;
import ru.auto.tests.cabinet.model.PaymentResponse;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /client/{client_id}/payrequest")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostClientPayRequestTest {

    private static final String MANAGER_ID = "19565983";

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetIdInvoicePayRequest() {
        String demoUser = "6796836";
        String demoDealer = "22633";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, demoUser).getResult().get(0).getId();
        PaymentResponse response = api.invoice().postPayRequest().clientIdPath(demoDealer)
                .xAutoruOperatorUidHeader(demoUser).reqSpec(defaultSpec())
                .body(new PaymentRequestParams().quantity(12345l).returnPath("auto.ru")
                        .balancePersonId(Long.valueOf(balancePersonId)))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getPaymentUrl()).isNotEmpty();
        assertThat(response.getInvoiceId()).isNotNull();
        assertThat(response.getAmount()).isNotNull();
        assertThat(response.getRequestId()).isNotNull();
        assertThat(response.getTransactionId()).isNotEmpty();
    }

    @Test
    public void shouldGetIdInvoicePayRequestForManager() {
        String demoUser = "18879763";
        String demoDealer = "23543";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, demoUser).getResult().get(0).getId();
        api.invoice().postPayRequest().clientIdPath(demoDealer)
                .xAutoruOperatorUidHeader(MANAGER_ID).reqSpec(defaultSpec())
                .body(new PaymentRequestParams().quantity(12345l).returnPath("auto.ru")
                        .balancePersonId(Long.valueOf(balancePersonId))).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        String demoUser = "6796836";
        String demoDealer = "22633";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, demoUser).getResult().get(0).getId();
        api.invoice().postPayRequest().clientIdPath(demoDealer).xAutoruOperatorUidHeader("679683")
                .reqSpec(defaultSpec()).body(new PaymentRequestParams().quantity(12345l).returnPath("auto.ru")
                .balancePersonId(Long.valueOf(balancePersonId)))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}