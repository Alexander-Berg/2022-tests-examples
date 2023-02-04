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

@DisplayName("DELETE /subscriptions/client/{client_id}/category/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class DeleteSubscriptionsClientCategoryTest {

    private static final String CATEGORY = "money";
    private static final String EMAIL = getRandomEmail();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeEmptyArraySubscriptions() {
        String dealerId = "29248";
        String userId = "24899328";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, Lists.newArrayList(CATEGORY), EMAIL);
        api.subscription().deleteByCategory().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .categoryPath(CATEGORY).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
        List<ClientSubscription> response = adaptor.getAllSubscriptionDealer(dealerId, userId);
        assertThat(response.size()).isZero();
    }

    @Test
    public void shouldSeeStatusOkForManager() {
        String dealerId = "29262";
        String userId = "19565983";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, Lists.newArrayList(CATEGORY), EMAIL);
        api.subscription().deleteByCategory().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .categoryPath(CATEGORY).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "29338";
        String userId = "17548139";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, Lists.newArrayList(CATEGORY), EMAIL);
        api.subscription().deleteByCategory().clientIdPath(dealerId).xAutoruOperatorUidHeader("1956598")
                .categoryPath(CATEGORY).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}