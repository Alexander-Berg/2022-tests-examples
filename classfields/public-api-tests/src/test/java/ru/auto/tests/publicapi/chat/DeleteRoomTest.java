package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("DELETE /chat/room/{id}/me")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteRoomTest {

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
    public void shouldSee403ForDeleteChatWhenNoAuth() {
        api.chat().excludeUser().idPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400ForDeleteNotExistChat() {
        String chatId = Utils.getRandomString();
        AutoApiErrorResponse errorResponse = api.chat().excludeUser().idPath(chatId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class);
        assertThat(errorResponse).hasStatus(ERROR).hasError(BAD_REQUEST)
                .hasDetailedError(String.format("Incorrect chat room id: [%s]", chatId));
    }

    @Test
    public void shouldDeleteChat() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String id = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        api.chat().excludeUser().idPath(id).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldTwiceDeleteChat() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.createEmptyRoom(sessionId).getRoom().getId();

        api.chat().excludeUser().idPath(id).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        api.chat().excludeUser().idPath(id).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }
}
