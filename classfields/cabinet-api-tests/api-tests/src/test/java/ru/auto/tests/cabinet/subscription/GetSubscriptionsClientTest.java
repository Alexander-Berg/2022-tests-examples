package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.List;
import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.*;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /subscriptions/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetSubscriptionsClientTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeSubscriptionsClient() {
        String dealerId = "9015";
        String userId = "10380477";
        String email = getRandomEmail();
        List<String> categories = Lists.newArrayList("autoload", "money");

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, categories, email);
        Function<ApiClient, JsonArray> response = apiClient -> apiClient.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonArray.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi)));
    }

    @Test
    public void shouldSeeStatusOkForManager() {
        String dealerId = "20101";
        String userId = "19565983";

        api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "20101";
        String userId = "1956598";

        api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}