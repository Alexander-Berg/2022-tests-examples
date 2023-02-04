package ru.auto.tests.cabinet.invoice;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.ExternalPaymentRequestParams;
import ru.auto.tests.cabinet.model.PaymentResponse;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.math.BigDecimal;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;

@DisplayName("POST /external/payrequest")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostExternalPayRequestTest {

    private static final String EMAIL = getRandomEmail();

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetIdInvoiceExternalPayRequest() {
        String demoUser = "24913024";
        String demoDealer = "27516";
        String balancePersonId = adaptor.getBalancePersonsId(demoDealer, demoUser).getResult().get(0).getId();
        BigDecimal balanceClientId = adaptor.getBalanceClientInfo(demoDealer, demoUser).getBalanceClientId();
        Long balanceOrderId = adaptor.getOrderClient(demoDealer).getId();
        PaymentResponse response = api.invoice().postExternalPayRequest()
                .body(new ExternalPaymentRequestParams().quantity(12345l).balancePersonId(Long.valueOf(balancePersonId))
                        .balanceClientId(balanceClientId.longValue()).balanceOrderId(balanceOrderId)
                        .receiptEmail(EMAIL).returnPath("auto.ru")).executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getPaymentUrl()).isNotEmpty();
        assertThat(response.getInvoiceId()).isNotNull();
        assertThat(response.getAmount()).isNotNull();
        assertThat(response.getRequestId()).isNotNull();
        assertThat(response.getTransactionId()).isNotEmpty();
    }
}