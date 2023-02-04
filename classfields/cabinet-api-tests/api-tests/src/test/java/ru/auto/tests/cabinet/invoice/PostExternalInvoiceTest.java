package ru.auto.tests.cabinet.invoice;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.ExternalInvoiceParams;
import ru.auto.tests.cabinet.model.InvoicePdfLink;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.math.BigDecimal;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;

@DisplayName("POST /client/{client_id}/invoice")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostExternalInvoiceTest {

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetIdInvoice() {
        String demoUser = "12661882";
        String demoDealer = "25013";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, demoUser).getResult().get(0).getId();
        BigDecimal balanceClientId = adaptor.getBalanceClientInfo(demoDealer, demoUser).getBalanceClientId();
        Long balanceOrderId = adaptor.getOrderClient(demoDealer).getId();
        InvoicePdfLink response = api.invoice().postExternalInvoice().body(new ExternalInvoiceParams().quantity(12345l)
                .balancePersonId(Long.valueOf(balancePersonId)).balanceClientId(balanceClientId.longValue())
                .balanceOrderId(balanceOrderId)).executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getContentType()).isNotEmpty();
        assertThat(response.getFilename()).isNotEmpty();
        assertThat(response.getInvoiceId()).isNotNull();
        assertThat(response.getMdsLink()).isNotEmpty();
    }

    @Test
    public void shouldGetIdInvoiceForManager() {
        String manager = "19565983";
        String demoDealer = "24689";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, manager).getResult().get(0).getId();
        BigDecimal balanceClientId = adaptor.getBalanceClientInfo(demoDealer, manager).getBalanceClientId();
        Long balanceOrderId = adaptor.getOrderClient(demoDealer).getId();
        InvoicePdfLink response = api.invoice().postExternalInvoice().body(new ExternalInvoiceParams().quantity(12345l)
                .balancePersonId(Long.valueOf(balancePersonId)).balanceClientId(balanceClientId.longValue())
                .balanceOrderId(balanceOrderId)).executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getContentType()).isNotEmpty();
        assertThat(response.getFilename()).isNotEmpty();
        assertThat(response.getInvoiceId()).isNotNull();
        assertThat(response.getMdsLink()).isNotEmpty();
    }
}