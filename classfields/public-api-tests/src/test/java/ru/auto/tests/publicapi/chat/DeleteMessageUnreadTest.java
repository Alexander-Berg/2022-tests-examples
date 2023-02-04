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
import ru.auto.tests.publicapi.model.VertisChatMessage;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("DELETE /chat/message/unread")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteMessageUnreadTest {

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
        api.chat().markMessagesRead().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        AutoApiErrorResponse response = api.chat().markMessagesRead().reqSpec(defaultSpec()).roomIdQuery(roomId).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);
        assertThat(response).hasError(NO_AUTH).hasStatus(ERROR)
                .hasDetailedError("Expected registered user. Provide valid session_id");
    }

    @Test
    public void shouldDeleteEmptyChat() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        api.chat().markMessagesRead().roomIdQuery(roomId).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldDeleteUnreadMessages() {
        Account account = am.create();
        Account secondAccount = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String secondAccountSessionId = adaptor.login(secondAccount).getSession().getId();
        String roomId = adaptor.createRoom(sessionId, newArrayList(secondAccount)).getRoom().getId();

        VertisChatMessage message = adaptor.createMessage(secondAccountSessionId, roomId).getMessage();

        api.chat().markMessagesRead().roomIdQuery(message.getRoomId()).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

}
