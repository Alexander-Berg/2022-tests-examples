package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.VertisChatMessagePayload;
import ru.auto.tests.publicapi.model.AutoApiMessageResponse;
import ru.auto.tests.publicapi.model.AutoApiSendMessageRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.VertisChatMessagePayload.ContentTypeEnum.PLAIN;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("POST /chat/message")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CreateMessageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private Account account;

    @Test
    public void shouldSee403ForCreateMessageWhenNoAuth() {
        api.chat().sendMessage().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        AutoApiErrorResponse response = api.chat().sendMessage().reqSpec(defaultSpec())
                .body(new AutoApiSendMessageRequest().roomId(roomId)).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);
        assertThat(response).hasError(NO_AUTH).hasStatus(ERROR)
                .hasDetailedError("Expected registered user. Provide valid session_id");
    }

    @Test
    public void shouldNotSendChatWithoutRoom() {
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiErrorResponse response = api.chat().sendMessage().reqSpec(defaultSpec())
                .body(new AutoApiSendMessageRequest()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);

        assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR);
    }

    @Test
    //todo: SC_INTERNAL_SERVER_ERROR??
    public void shouldNotSendChatToNotExistRoom() {
        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = Utils.getRandomString();
        AutoApiErrorResponse response = api.chat().sendMessage().reqSpec(defaultSpec())
                .body(new AutoApiSendMessageRequest().roomId(roomId)).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR))).as(AutoApiErrorResponse.class);

        //.hasError(UNKNOWN_ERROR)
        assertThat(response).hasStatus(ERROR);
    }

    @Test
    public void shouldCreateMessage() {
        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        String messageValue = Utils.getRandomString();
        AutoApiMessageResponse response = api.chat().sendMessage().reqSpec(defaultSpec())
                .body(new AutoApiSendMessageRequest()
                        .payload(new VertisChatMessagePayload().contentType(PLAIN).value(messageValue)).roomId(roomId)).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response.getMessage()).hasRoomId(roomId);
        assertThat(response.getMessage().getPayload()).hasContentType(PLAIN).hasValue(messageValue);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        String messageValue = Utils.getRandomString();
        JsonObject response = api.chat().sendMessage().reqSpec(defaultSpec())
                .body(new AutoApiSendMessageRequest()
                        .payload(new VertisChatMessagePayload().contentType(PLAIN).value(messageValue)).roomId(roomId)).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        JsonObject responseProd = prodApi.chat().sendMessage().reqSpec(defaultSpec())
                .body(new AutoApiSendMessageRequest()
                        .payload(new VertisChatMessagePayload().contentType(PLAIN).value(messageValue)).roomId(roomId)).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(response, jsonEquals(responseProd).whenIgnoringPaths("message.id", "message.created"));
    }
}
