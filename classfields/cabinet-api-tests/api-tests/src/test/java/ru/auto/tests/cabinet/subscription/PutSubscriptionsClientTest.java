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
import ru.auto.tests.cabinet.model.SubscriptionProto;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("PUT /subscription/{id}/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class PutSubscriptionsClientTest {

    private static final String CATEGORY_MONEY = "money";
    private static final String CATEGORY_AUTOLOAD = "autoload";
    private static final String EMAIL = getRandomEmail();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeEditedSubscription() {
        String dealerId = "33516";
        String userId = "30919040";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY_MONEY, EMAIL).getId();
        api.subscription().putById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId).idPath(subscriptionId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL))
                .execute(validatedWith(shouldBeCode(SC_OK)));
        List<ClientSubscription> response = adaptor.getAllSubscriptionDealer(dealerId, userId);

        assertThat(response.get(0).getCategory()).isEqualTo(CATEGORY_AUTOLOAD);
    }

    @Test
    public void shouldSeeStatusOkForManager() {
        String dealerId = "33538";
        String userId = "19565983";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY_MONEY, EMAIL).getId();
        api.subscription().putById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId).idPath(subscriptionId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL))
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "33648";
        String userId = "31129940";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY_MONEY, EMAIL).getId();
        api.subscription().putById().clientIdPath(dealerId).xAutoruOperatorUidHeader("1956598").idPath(subscriptionId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeStatusConflict() {
        String dealerId = "33811";
        String userId = "31335323";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addArraySubscriptions(dealerId, userId, Lists.newArrayList(CATEGORY_AUTOLOAD, CATEGORY_MONEY), EMAIL);
        Long subscriptionId = adaptor.getAllSubscriptionDealer(dealerId, userId)
                .stream()
                .filter(ClientSubscription -> ClientSubscription.getCategory().equals(CATEGORY_MONEY))
                .collect(Collectors.toList())
                .get(0).getId();
        api.subscription().putById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId).idPath(subscriptionId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL))
                .execute(validatedWith(shouldBeCode(SC_CONFLICT)));
    }

    @Test
    public void shouldSeeStatusBadRequest() {
        String dealerId = "34217";
        String userId = "31973709";

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, CATEGORY_MONEY, EMAIL).getId();
        api.subscription().putById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId).idPath(subscriptionId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().emailAddress(EMAIL))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSeeStatusNotFound() {
        String dealerId = "34217";
        String userId = "31973709";

        api.subscription().putById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId).idPath("09090090")
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL))
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}