package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.VertisChatMessage;
import ru.auto.tests.publicapi.model.AutoApiMessageListingResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.io.File;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("GET /chat/message")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class MessageTest {
    private static final String PHOTO_PATH = "photo/photo.jpg";
    private static final String CONTROL_NAME = "file";

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
        api.chat().getMessages().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        AutoApiErrorResponse response = api.chat().getMessages().reqSpec(defaultSpec()).roomIdQuery(roomId).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);
        assertThat(response).hasError(NO_AUTH).hasStatus(ERROR)
                .hasDetailedError("Expected registered user. Provide valid session_id");
    }

    @Test
    public void shouldSeeNoMessages() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        AutoApiMessageListingResponse messageListingResponse = api.chat().getMessages().roomIdQuery(roomId).reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));
        Assertions.assertThat(messageListingResponse.getMessages()).isNull();
    }

    @Test
    public void shouldSeeNoMessagesVisibleFor() {
        Account sender = am.create();
        Account recipient1 = am.create();
        Account recipient2 = am.create();
        String senderSessionId = adaptor.login(sender).getSession().getId();
        String recipientSessionId = adaptor.login(recipient2).getSession().getId();
        String roomId = adaptor.createRoom(senderSessionId, newArrayList(sender, recipient1, recipient2)).getRoom().getId();

        adaptor.createMessage(sender.getId(), roomId, recipient1.getId());

        AutoApiMessageListingResponse messageListingResponse = api.chat().getMessages().roomIdQuery(roomId).reqSpec(defaultSpec()).xSessionIdHeader(recipientSessionId).executeAs(validatedWith(shouldBeSuccess()));

        Assertions.assertThat(messageListingResponse.getMessages()).isNull();
    }

    @Test
    public void shouldSeeMessages() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        VertisChatMessage message = adaptor.createMessage(sessionId, roomId).getMessage();

        AutoApiMessageListingResponse messageListingResponse = api.chat().getMessages().roomIdQuery(message.getRoomId()).reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        assertThat(messageListingResponse).hasOnlyMessages(message);
    }

    @Test
    public void shouldSeeMessagesVisibleFor() {
        Account sender = am.create();
        Account recipient1 = am.create();
        Account recipient2 = am.create();
        String senderSessionId = adaptor.login(sender).getSession().getId();
        String recipientSessionId = adaptor.login(recipient1).getSession().getId();
        String roomId = adaptor.createRoom(senderSessionId, newArrayList(sender, recipient1, recipient2)).getRoom().getId();

        adaptor.createMessage(sender.getId(), roomId, recipient1.getId());

        AutoApiMessageListingResponse messageListingResponse = api.chat().getMessages().roomIdQuery(roomId).reqSpec(defaultSpec()).xSessionIdHeader(recipientSessionId).executeAs(validatedWith(shouldBeSuccess()));

        Assertions.assertThat(messageListingResponse.getMessages()).isNotNull();
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeePhotoMessage() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        String uploadUrl = api.chat().bootstrapMessage().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .roomIdQuery(roomId).executeAs(validatedWith(shouldBeSuccess())).getUploadImageUrl();

        Object photoMessage = uploadImage(uploadUrl, PHOTO_PATH);

        VertisChatMessage lastMessage = api.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess())).getRooms().get(1).getLastMessage();

        assertThat(lastMessage).isEqualTo(photoMessage);
    }

    private File getFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

    public Object uploadImage(String uploadImage, String photoPath) {
        String result = RestAssured.given().config(RestAssuredConfig.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation()))
                .filter(new AllureLoggerFilter()).contentType("multipart/form-data")
                .multiPart(CONTROL_NAME, getFile(photoPath), "image/jpeg")
                .post(uploadImage).as(JsonObject.class, GSON).get("message").toString();

        return new JSON().deserialize(result, VertisChatMessage.class);
    }
}