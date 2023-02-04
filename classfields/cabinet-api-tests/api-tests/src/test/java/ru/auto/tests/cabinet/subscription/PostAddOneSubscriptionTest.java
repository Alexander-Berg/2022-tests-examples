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
import ru.auto.tests.cabinet.model.SubscriptionProto;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /subscription/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class PostAddOneSubscriptionTest {

    private static final String CATEGORY = "autoload";
    private static final String EMAIL = getRandomEmail();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeOneSubcription() {
        String dealerId = "26622";
        String userId = "22678692";

        adaptor.clearSubscriptions(dealerId, userId);
        api.subscription().postById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY)
                .emailAddress(EMAIL)).execute(validatedWith(shouldBeCode(SC_OK)));

        List<ClientSubscription> response = api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getCategory()).isEqualTo(CATEGORY);
        assertThat(response.get(0).getEmailAddress()).isEqualTo(EMAIL);
    }

    @Test
    public void shouldGetStatusForbidden() {
        api.subscription().postById().clientIdPath("20101").xAutoruOperatorUidHeader("2666903")
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category("autoload")
                .emailAddress("atestemailsubscription@yandex.ru")).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetStatusBadRequest() {
        api.subscription().postById().clientIdPath("20101").xAutoruOperatorUidHeader("19565983")
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category("autoload"))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldGetStatusConflict() {
        String dealerId = "1476";
        String userId = "14130368";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addSubscription(dealerId, userId, CATEGORY, EMAIL);
        api.subscription().postById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY)
                .emailAddress(EMAIL)).execute(validatedWith(shouldBeCode(SC_CONFLICT)));
    }

    @Test
    public void shouldSeeOneSubcriptionForManager() {
        String dealerId = "2060";
        String userId = "19565983";

        adaptor.clearSubscriptions(dealerId, userId);
        api.subscription().postById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY)
                .emailAddress(EMAIL)).execute(validatedWith(shouldBeCode(SC_OK)));

        List<ClientSubscription> response = api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getCategory()).isEqualTo(CATEGORY);
        assertThat(response.get(0).getEmailAddress()).isEqualTo(EMAIL);
    }

    @Test
    public void shouldSeeOneSubcriptionForAgency() {
        String dealerId = "23382";
        String userId = "33638666";

        api.subscription().postById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY)
                .emailAddress(EMAIL)).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeOneSubcriptionForCompany() {
        String dealerId = "28082";
        String userId = "23480672";

        api.subscription().postById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(CATEGORY)
                .emailAddress(EMAIL)).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}