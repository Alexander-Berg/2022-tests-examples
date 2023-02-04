package ru.auto.tests.cabinet.invoice;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.InvoiceParams;
import ru.auto.tests.cabinet.model.InvoicePdfLink;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /client/{client_id}/invoice")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostClientInvoiceTest {

    private static final String MANAGER_ID = "19565983";

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetIdInvoice() {
        String demoUser = "20529943";
        String demoDealer = "24687";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, demoUser).getResult().get(0).getId();
        InvoicePdfLink response = api.invoice().postInvoice().clientIdPath(demoDealer).typeQuery("regular")
                .xAutoruOperatorUidHeader(demoUser).reqSpec(defaultSpec())
                .body(new InvoiceParams().quantity(12345l).balancePersonId(Long.valueOf(balancePersonId)))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getContentType()).isNotEmpty();
        assertThat(response.getFilename()).isNotEmpty();
        assertThat(response.getInvoiceId()).isNotNull();
        assertThat(response.getMdsLink()).isNotEmpty();
    }

    @Test
    public void shouldGetIdInvoiceForManager() {
        String demoDealer = "25224";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, MANAGER_ID).getResult().get(0).getId();
        api.invoice().postInvoice().clientIdPath(demoDealer).typeQuery("regular")
                .xAutoruOperatorUidHeader(MANAGER_ID).reqSpec(defaultSpec())
                .body(new InvoiceParams().quantity(12345l).balancePersonId(Long.valueOf(balancePersonId)))
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        String demoDealer = "25224";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, MANAGER_ID).getResult().get(0).getId();
        api.invoice().postInvoice().clientIdPath(demoDealer).typeQuery("regular")
                .xAutoruOperatorUidHeader("1956598").reqSpec(defaultSpec())
                .body(new InvoiceParams().quantity(12345l).balancePersonId(Long.valueOf(balancePersonId)))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetStatusNotFound() {
        String demoDealer = "25224";
        api.invoice().postInvoice().clientIdPath(demoDealer).typeQuery("regular")
                .xAutoruOperatorUidHeader(MANAGER_ID).reqSpec(defaultSpec())
                .body(new InvoiceParams().quantity(12345l).balancePersonId(232l))
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}