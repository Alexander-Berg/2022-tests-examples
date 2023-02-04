package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.ClientSubscription;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.List;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("DELETE /subscriptions/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class DeleteSubscriptionsClientTest {


    private static final String EMAIL = getRandomEmail();
    private static final List<String> CATEGORIES = Lists.newArrayList("autoload", "money");

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeEmptyArraySubscriptions() {
        String dealerId = "29354";
        String userId = "35626507";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, CATEGORIES, EMAIL);
        api.subscription().deleteBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
        List<ClientSubscription> response = adaptor.getAllSubscriptionDealer(dealerId, userId);
        assertThat(response.size()).isZero();
    }

    @Test
    public void shouldSeeStatusOkForManager() {
        String dealerId = "29400";
        String userId = "19565983";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, CATEGORIES, EMAIL);
        api.subscription().deleteBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "29528";
        String userId = "12668458";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, CATEGORIES, EMAIL);
        api.subscription().deleteBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader("1956598")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}