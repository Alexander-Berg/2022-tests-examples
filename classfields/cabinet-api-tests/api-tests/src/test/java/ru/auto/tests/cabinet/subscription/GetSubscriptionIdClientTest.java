package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /subscription/{id}/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetSubscriptionIdClientTest {

    private static final String CATEGORY = "money";
    private static final String EMAIL = getRandomEmail();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeStatusOkForManager() {
        String dealerId = "25970";
        String userId = "21865602";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY, EMAIL).getId();
        api.subscription().getById().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec()).idPath(subscriptionId)
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "25912";
        String userId = "33058702";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY, EMAIL).getId();
        api.subscription().getById().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader("1956598").reqSpec(defaultSpec()).idPath(subscriptionId)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}