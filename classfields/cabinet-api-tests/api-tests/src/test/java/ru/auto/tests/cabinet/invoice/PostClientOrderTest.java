package ru.auto.tests.cabinet.invoice;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.model.BalanceOrder;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /client/{client_id}/order")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostClientOrderTest {

    private static final String DEALER_ID = "20101";


    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetIdOrder() {
        BalanceOrder response = api.invoice().createOrder().clientIdPath(DEALER_ID).reqSpec(defaultSpec())
                .requireActualBalanceDataQuery("false").executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getId()).isNotNull();
    }

    @Test
    public void shouldGetIdOrderForAgency() {
        String dealer = "898";
        String agency = "14342";
        BalanceOrder response = api.invoice().createOrder().clientIdPath(dealer).reqSpec(defaultSpec())
                .requireActualBalanceDataQuery("false").agencyIdQuery(agency)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getId()).isNotNull();
    }

    @Test
    public void shouldGetIdOrderHasNotDiffWithProd() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.invoice().createOrder().clientIdPath(DEALER_ID)
                .reqSpec(defaultSpec()).requireActualBalanceDataQuery("false")
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    public void shouldGetStatusNotFound() {
        String dealer = "898";
        String agency = "1";
        api.invoice().createOrder().clientIdPath(dealer).reqSpec(defaultSpec())
                .requireActualBalanceDataQuery("false").agencyIdQuery(agency)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}