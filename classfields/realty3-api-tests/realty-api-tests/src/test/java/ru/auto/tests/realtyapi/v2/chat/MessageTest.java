package ru.auto.tests.realtyapi.v2.chat;

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
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor;
import ru.auto.tests.realtyapi.adaptor.chat.P2PChatRoomComponents;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.utils.UtilsRealtyApi;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiError;
import ru.auto.tests.realtyapi.v2.model.VertisChatMessage;
import ru.auto.tests.realtyapi.v2.model.VertisChatMessagePayload;
import ru.auto.tests.realtyapi.v2.model.VertisChatMessageProperties;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor.chatMessageData;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.model.RealtyApiError.CodeEnum.*;

@Title("POST /chat/messages")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@SuppressWarnings("ConstantConditions")
public class MessageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient apiV2;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private ChatRoomApiAdaptor chatRoomApiAdaptor;

    @Inject
    private FtlProcessor ftlProcessor;

    @Test
    @DisplayName("Отправка сообщения")
    public void sendMessage() {
        String chatMessageText = "e2";
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();

        VertisChatMessage chatMessage = chatRoomApiAdaptor
                .sendMessage(apiV2, p2pChat.getBuyerToken(), p2pChat.getRoomId(), chatMessageText);

        assertEquals(p2pChat.getRoomId(), chatMessage.getRoomId());
        assertEquals(p2pChat.getBuyerAccount().getId(), chatMessage.getAuthor());
        assertEquals(VertisChatMessagePayload.ContentTypeEnum.PLAIN,
                chatMessage.getPayload().getContentType());
        assertEquals(chatMessageText, chatMessage.getPayload().getValue());
        // null values for all fields
        assertEquals(new VertisChatMessageProperties(), chatMessage.getProperties());
    }

    @Test
    @DisplayName("Отправка сообщения с некорректным ContentType-ом")
    public void incorrectContentType() {
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();
        Map<String, String> data = chatMessageData(p2pChat.getRoomId(), "anything");

        data.put("contentType", "TEXT_HTML");
        sendMessageWithIncorrectContentType(p2pChat, data);

        data.put("skipContentType", "true");
        sendMessageWithIncorrectContentType(p2pChat, data);
    }

    private void sendMessageWithIncorrectContentType(P2PChatRoomComponents p2pChat, Map<String, String> data) {
        RealtyApiError error = chatRoomApiAdaptor.sendMessageWithError(apiV2, p2pChat.getBuyerToken(),
                chatRoomApiAdaptor.processChatMessage(data), 400);

        assertEquals(BAD_REQUEST, error.getCode());
        assertEquals("sending messages with non-PLAIN payload type is not allowed", error.getMessage());
    }

    @Test
    @DisplayName("Отправка сообщения в несуществующий чат")
    public void sendMessage_NonExistentRoom() {
        String nonExistedChatId = "11111111111111111111111111111111111";
        Account account = am.create();
        String token = oAuth.getToken(account);

        RealtyApiError error = chatRoomApiAdaptor.sendMessageWithError(apiV2, token,
                chatRoomApiAdaptor.processChatMessage(
                        chatMessageData(nonExistedChatId, "anything")), 404);

        assertEquals(CHAT_NOT_FOUND, error.getCode());
    }

    @Test
    @DisplayName("Попытка почитать сообщения чужого чата")
    public void messageFromThirdParty() {
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();
        Account otherAccount = am.create();
        String otherToken = oAuth.getToken(otherAccount);

        RealtyApiError error = apiV2.chatMessages().getMessagesRoute()
                .roomIdPath(p2pChat.getRoomId())
                .authorizationHeader(otherToken)
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBeCode(403))).getError();

        assertEquals(CHAT_FORBIDDEN, error.getCode());
    }

    @Test
    @DisplayName("Комплексный тест на получение сообщений с разными параметрами")
    public void getMessages() {
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();
        List<VertisChatMessage> messages = Arrays.asList(
                chatRoomApiAdaptor.sendMessage(apiV2, p2pChat.getBuyerToken(), p2pChat.getRoomId(), "First"),
                chatRoomApiAdaptor.sendMessage(apiV2, p2pChat.getSellerToken(), p2pChat.getRoomId(), "Second"),
                chatRoomApiAdaptor.sendMessage(apiV2, p2pChat.getBuyerToken(), p2pChat.getRoomId(), "Third"));

        // all
        List<VertisChatMessage> responseMessages = apiV2.chatMessages().getMessagesRoute()
                .roomIdPath(p2pChat.getRoomId())
                .authorizationHeader(p2pChat.getSellerToken())
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse().getMessages();

        assertThat(responseMessages).as("All messages").isEqualTo(messages);

        // limited
        responseMessages = apiV2.chatMessages().getMessagesRoute()
                .roomIdPath(p2pChat.getRoomId())
                .countQuery(2)
                .authorizationHeader(p2pChat.getSellerToken())
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse().getMessages();

        assertThat(responseMessages).as("Limited number of messages").isEqualTo(messages.subList(0, 2));

        // from the end
        responseMessages = apiV2.chatMessages().getMessagesRoute()
                .roomIdPath(p2pChat.getRoomId())
                .countQuery(2)
                .ascQuery(false)
                .authorizationHeader(p2pChat.getSellerToken())
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse().getMessages();

        assertThat(responseMessages).as("Two messages from the end").isEqualTo(messages.subList(1, 3));
    }

    @Test
    @DisplayName("Некорректный UID")
    public void unknownUid() {
        P2PChatRoomComponents chatRoom = chatRoomApiAdaptor.createP2pChatRoom();

        RealtyApiError error = apiV2.chatMessages().sendMessageRoute()
                .xUidHeader(UtilsRealtyApi.getRandomUID())
                .xAuthorizationHeader("Vertis swagger")
                .reqSpec(authSpec())
                .reqSpec(req -> req.setBody(chatRoomApiAdaptor.
                        processChatMessage(chatMessageData(chatRoom.getRoomId(), "test"))))
                .executeAs(validatedWith(shouldBeCode(404))).getError();

        assertEquals(NOT_FOUND, error.getCode());
    }

    @Test
    @DisplayName("Отправка вложений")
    public void sendImagesAttachment() {
        P2PChatRoomComponents chatRoom = chatRoomApiAdaptor.createP2pChatRoom();
        HashMap<String, String> attachmentSizes = new HashMap<String, String>() {{
            put("320x320", "avatars.mdst.yandex.net/get-vertis-chat/3915/1bd7bf38bfedd56cc6511e954e3a0b01/320x320");
            put("460x460", "avatars.mdst.yandex.net/get-vertis-chat/3915/1bd7bf38bfedd56cc6511e954e3a0b01/460x460");
            put("1200x1200", "avatars.mdst.yandex.net/get-vertis-chat/3915/1bd7bf38bfedd56cc6511e954e3a0b01/1200x1200");
        }};
        Map<String,Object> messageData = new HashMap<String, Object>() {{
            put("roomId", chatRoom.getRoomId());
            put("imageSizes", attachmentSizes);
        }};
        String body = ftlProcessor.process(messageData, "chat/image_sizes_message.ftl");

        VertisChatMessage chatMessageResponse = apiV2.chatMessages().sendMessageRoute()
                .authorizationHeader(chatRoom.getBuyerToken())
                .reqSpec(authSpec())
                .reqSpec(req -> req.setBody(body))
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();

        assertEquals(1, chatMessageResponse.getAttachments().size());
        assertEquals(attachmentSizes, chatMessageResponse.getAttachments().get(0).getImage().getSizes());
    }
}
