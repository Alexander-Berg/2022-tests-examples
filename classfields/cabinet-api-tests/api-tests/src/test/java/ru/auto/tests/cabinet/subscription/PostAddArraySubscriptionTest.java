package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.ClientSubscription;
import ru.auto.tests.cabinet.model.ClientSubscriptionSeq;
import ru.auto.tests.cabinet.model.SubscriptionProto;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /subscriptions/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class PostAddArraySubscriptionTest {

    private static final String CATEGORY_AUTOLOAD = "autoload";
    private static final String CATEGORY_MONEY = "money";
    private static final String EMAIL = getRandomEmail();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeArraySubscriptions() {
        String dealerId = "1916";
        String userId = "2666903";

        adaptor.clearSubscriptions(dealerId, userId);
        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(newArrayList(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL),
                        new SubscriptionProto().category(CATEGORY_MONEY).emailAddress(EMAIL))))
                .execute(validatedWith(shouldBeCode(SC_OK)));

        List<ClientSubscription> response = api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getCategory()).isEqualTo(CATEGORY_AUTOLOAD);
        assertThat(response.get(0).getEmailAddress()).isEqualTo(EMAIL);
        assertThat(response.get(1).getCategory()).isEqualTo(CATEGORY_MONEY);
        assertThat(response.get(1).getEmailAddress()).isEqualTo(EMAIL);
    }

    @Test
    @Ignore("VSMONEY-208")
    public void shouldSeeStatusConflict() {
        String dealerId = "1916";
        String userId = "2666903";

        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(newArrayList(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL),
                        new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL))))
                .execute(validatedWith(shouldBeCode(SC_CONFLICT)));
    }

    @Test
    public void shouldSeeStatusBadRequest() {
        String dealerId = "1916";
        String userId = "2666903";

        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(newArrayList(new SubscriptionProto().category(CATEGORY_AUTOLOAD))))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSeeStatusForebidden() {
        String dealerId = "1916";
        String userId = "2666901";

        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(newArrayList(new SubscriptionProto().category(CATEGORY_AUTOLOAD))))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeArraySubscriptionsForManager() {
        String dealerId = "2884";
        String userId = "19565983";

        adaptor.clearSubscriptions(dealerId, userId);
        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(newArrayList(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL),
                        new SubscriptionProto().category(CATEGORY_MONEY).emailAddress(EMAIL))))
                .execute(validatedWith(shouldBeCode(SC_OK)));

        List<ClientSubscription> response = api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getCategory()).isEqualTo(CATEGORY_AUTOLOAD);
        assertThat(response.get(0).getEmailAddress()).isEqualTo(EMAIL);
        assertThat(response.get(1).getCategory()).isEqualTo(CATEGORY_MONEY);
        assertThat(response.get(1).getEmailAddress()).isEqualTo(EMAIL);
    }

    @Test
    public void shouldSeeArraySubscriptionsForAgency() {
        String dealerId = "4254";
        String userId = "14439810";

        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(newArrayList(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL),
                        new SubscriptionProto().category(CATEGORY_MONEY).emailAddress(EMAIL))))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeArraySubscriptionsForCompany() {
        String dealerId = "16011";
        String userId = "23480672";

        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(newArrayList(new SubscriptionProto().category(CATEGORY_AUTOLOAD).emailAddress(EMAIL),
                        new SubscriptionProto().category(CATEGORY_MONEY).emailAddress(EMAIL))))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}