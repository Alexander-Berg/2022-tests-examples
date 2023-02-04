package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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

@DisplayName("DELETE /subscription/{id}/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class DeleteSubscriptionIdClientTest {

    private static final String CATEGORY = "money";
    private static final String EMAIL = getRandomEmail();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeEmptyArraySubscriptions() {
        String dealerId = "33380";
        String userId = "30790322";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY, EMAIL).getId();
        api.subscription().deleteById().idPath(subscriptionId).clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
        List<ClientSubscription> response = adaptor.getAllSubscriptionDealer(dealerId, userId);
        assertThat(response.size()).isZero();
    }

    @Test
    public void shouldSeeStatusOkforManager() {
        String dealerId = "33386";
        String userId = "19565983";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY, EMAIL).getId();
        api.subscription().deleteById().idPath(subscriptionId).clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "33410";
        String userId = "30816890";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY, EMAIL).getId();
        api.subscription().getById().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader("1956598").reqSpec(defaultSpec()).idPath(subscriptionId)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}