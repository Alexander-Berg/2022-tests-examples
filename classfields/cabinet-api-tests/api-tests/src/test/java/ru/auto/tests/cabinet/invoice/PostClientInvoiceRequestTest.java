package ru.auto.tests.cabinet.invoice;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.model.InvoiceRequestParams;
import ru.auto.tests.cabinet.model.InvoiceUrls;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.math.BigDecimal;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /client/{client_id}/invoice/request")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostClientInvoiceRequestTest {

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetRequestIdForAgency() {
        String userAgency = "8769372";
        String dealer = "8991";
        BigDecimal agencyId = BigDecimal.valueOf(14342);
        InvoiceUrls response = api.invoice().postInvoiceRequest().clientIdPath(dealer).xAutoruOperatorUidHeader(userAgency)
                .reqSpec(defaultSpec()).body(new InvoiceRequestParams().quantity(12345l).customAgencyId(agencyId))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getUserPath()).isNotEmpty();
        assertThat(response.getAdminPath()).isNotEmpty();
        assertThat(response.getRequestId()).isNotNull();
    }

    @Test
    public void shouldGetRequestIdForDealer() {
        String userDealer = "3209389";
        String dealer = "1726";
        InvoiceUrls response = api.invoice().postInvoiceRequest().clientIdPath(dealer).xAutoruOperatorUidHeader(userDealer)
                .reqSpec(defaultSpec()).body(new InvoiceRequestParams().quantity(12345l))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getUserPath()).isNotEmpty();
        assertThat(response.getAdminPath()).isNotEmpty();
        assertThat(response.getRequestId()).isNotNull();
    }

    @Test
    public void shouldGetRequestIdForManager() {
        String manager = "19565983";
        String dealer = "898";
        InvoiceUrls response = api.invoice().postInvoiceRequest().clientIdPath(dealer).xAutoruOperatorUidHeader(manager)
                .reqSpec(defaultSpec()).body(new InvoiceRequestParams().quantity(12345l))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getUserPath()).isNotEmpty();
        assertThat(response.getAdminPath()).isNotEmpty();
        assertThat(response.getRequestId()).isNotNull();
    }

    @Test
    public void shouldGetStatusForbidden() {
        String manager = "1956598";
        String dealer = "898";
        api.invoice().postInvoiceRequest().clientIdPath(dealer).xAutoruOperatorUidHeader(manager)
                .reqSpec(defaultSpec()).body(new InvoiceRequestParams().quantity(12345l))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}