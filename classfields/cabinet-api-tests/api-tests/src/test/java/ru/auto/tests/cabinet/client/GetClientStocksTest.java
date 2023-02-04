package ru.auto.tests.cabinet.client;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import net.javacrumbs.jsonunit.core.Option;
import org.hamcrest.MatcherAssert;
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
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /client/{client_id}/stocks")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetClientStocksTest {

    private static final String DEALER_ID = "20101";
    private static final String USER_ID = "19565983";
    private static final Integer AMOUNT = 666;
    private static final Boolean RESOLUTION = true;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldGetInfoClientStocksHasNoDiffWithProduction() {
        adaptor.addAllStocksToDealer(DEALER_ID, USER_ID, AMOUNT, RESOLUTION);
        Function<ApiClient, JsonObject> response = apiClient -> apiClient.client().getClientStocks()
                .clientIdPath(DEALER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        System.out.println(response);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi))
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    public void shouldGetStatusOk() {
        api.client().getClientStocks().clientIdPath(DEALER_ID)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }
}