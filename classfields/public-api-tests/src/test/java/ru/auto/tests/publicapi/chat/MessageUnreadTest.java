package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiUnreadMessagesResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("GET /chat/message/unread")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class MessageUnreadTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403ForGetMessageWhenNoAuth() {
        api.chat().getUnreadMessages().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        adaptor.createEmptyRoom(sessionId);
        AutoApiErrorResponse response = api.chat().getUnreadMessages().reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);
        assertThat(response).hasError(NO_AUTH).hasStatus(ERROR)
                .hasDetailedError("Expected registered user. Provide valid session_id");
    }

    @Test
    public void shouldSeeNoUnreadMessages() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        adaptor.createEmptyRoom(sessionId);
        AutoApiUnreadMessagesResponse response = api.chat().getUnreadMessages().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
        assertThat(response).hasHasUnread(false);
    }

    @Test
    public void shouldSeeUnreadMessages() {
        Account account = am.create();
        Account secondAccount = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String secondAccountSessionId = adaptor.login(secondAccount).getSession().getId();

        String roomId = adaptor.createRoom(sessionId, newArrayList(secondAccount)).getRoom().getId();
        adaptor.createMessage(secondAccountSessionId, roomId);

        AutoApiUnreadMessagesResponse response = api.chat().getUnreadMessages().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe200OkJSON())).as(AutoApiUnreadMessagesResponse.class);
        assertThat(response).hasHasUnread(true);
    }
}