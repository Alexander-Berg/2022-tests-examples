package ru.auto.tests.cabinet.invoice;

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

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /client/{client_id}/invoice/{invoice_id}/pdf")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetInvoicePdfTest {

    private static final String USER_ID = "20011723";
    private static final String DEALER_ID = "24361";

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetInvoicePdfHasNoDiffWithProduction() {
        String balancePersonId = adaptor.getBalancePersonsId(DEALER_ID, USER_ID).getResult().get(0).getId();
        Long invoiceId = adaptor.getInvoiceDealer(DEALER_ID, USER_ID, Long.valueOf(balancePersonId)).getInvoiceId();
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.invoice().getInvoicePdf().clientIdPath(DEALER_ID)
                .invoiceIdPath(invoiceId).reqSpec(defaultSpec()).xAutoruOperatorUidHeader(USER_ID)
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        String balancePersonId = adaptor.getBalancePersonsId(DEALER_ID, USER_ID).getResult().get(0).getId();
        Long invoiceId = adaptor.getInvoiceDealer(DEALER_ID, USER_ID, Long.valueOf(balancePersonId)).getInvoiceId();
        api.invoice().getInvoicePdf().clientIdPath(DEALER_ID)
                .invoiceIdPath(invoiceId).reqSpec(defaultSpec()).xAutoruOperatorUidHeader("2001172")
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}