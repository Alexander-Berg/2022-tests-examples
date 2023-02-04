package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
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
import ru.auto.tests.publicapi.model.AutoApiRoomListingResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 13.07.18
 */

@DisplayName("PUT /chat/room/{id}/{mute, unmute}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class MuteUnmuteTest {

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
    public void shouldSee403WhenNoAuth() {
        api.chat().muteRoom().idPath(Utils.getRandomString()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithIncorrectRoomId() {
        String chatId = Utils.getRandomString();
        AutoApiErrorResponse response = api.chat().muteRoom().idPath(chatId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);

        AutoruApiModelsAssertions.assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
                .hasDetailedError(String.format("Incorrect chat room id: [%s]", chatId));
    }

    @Test
    public void shouldSee403WithNoAuth() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String chatId = adaptor.createEmptyRoom(sessionId).getRoom().getId();

        api.chat().muteRoom().idPath(chatId).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String chatId = adaptor.createEmptyRoom(sessionId).getRoom().getId();

        AutoApiErrorResponse response = api.chat().muteRoom().idPath(chatId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);

        AutoruApiModelsAssertions.assertThat(response).hasError(NO_AUTH).hasStatus(ERROR)
                .hasDetailedError("Expected registered user. Provide valid session_id");
    }

    @Test
    public void shouldMuteRoom() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        adaptor.createMessage(sessionId, roomId);
        muteRoom(roomId, sessionId);
        AutoApiRoomListingResponse response = api.chat().getRooms().xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response.getRooms().get(1).getMutedNotifications()).isTrue();
    }

    @Test
    public void shouldUnmuteRoom() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        adaptor.createMessage(sessionId, roomId);
        muteRoom(roomId, sessionId);
        unmuteRoom(roomId, sessionId);
        AutoApiRoomListingResponse response = api.chat().getRooms().xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response.getRooms().get(1).getMutedNotifications()).isNull();
    }

    @Step("Выключаем уведомления у комнаты «{roomId}»")
    public void muteRoom(String roomId, String sessionId) {
        api.chat().muteRoom().idPath(roomId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Включаем уведомления у комнаты «{roomId}»")
    public void unmuteRoom(String roomId, String sessionId) {
        api.chat().unmuteRoom().idPath(roomId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }
}