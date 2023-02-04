package ru.auto.tests.realtyapi.v2.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor;
import ru.auto.tests.realtyapi.adaptor.chat.P2PChatRoomComponents;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiChatBooleanActionResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyApiChatBooleanActionStatus;
import ru.auto.tests.realtyapi.v2.model.RealtyApiChatUserHasUnreadMessagesResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUserHasUnreadMessages;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.junit.Assert.*;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor.AllRoomsResponse;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor.AllRooms;

@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Title("DELETE /chat/messages/room/{room_id}/unread")
public class UnreadMessageTest {

    @Inject
    private ChatRoomApiAdaptor chatRoomApiAdaptor;

    @Inject
    private ApiClient apiV2;

    @Test
    public void unreadMessage() {
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();
        String senderUserToken = p2pChat.getSellerToken();
        String receiverUserToken = p2pChat.getBuyerToken();
        String roomId = p2pChat.getRoomId();

        chatRoomApiAdaptor.sendMessage(senderUserToken, roomId, "message");
        assertTrue(hasUnreadByAll(roomId, receiverUserToken));
        assertTrue(userHasUnreadMessages(receiverUserToken));

        RealtyApiChatBooleanActionStatus response = apiV2.chatMessages()
                .markMessagesReadRoute()
                .roomIdPath(roomId)
                .authorizationHeader(receiverUserToken)
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()))
                .getResponse();
        //noinspection ConstantConditions
        assertEquals(response.getId(), roomId);
        assertTrue(response.getStatus());

        assertFalse(hasUnreadByAll(roomId, receiverUserToken));
        assertFalse(userHasUnreadMessages(receiverUserToken));
    }

    private boolean hasUnreadByAll(String roomId, String receiverUserToken) {
        AllRooms allRoomsResponse = apiV2.chatRooms().getRoomsAllRoute()
                .authorizationHeader(receiverUserToken)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(AllRoomsResponse.class, GSON)
                .getResponse();
        Optional<Boolean> optHasUnread = allRoomsResponse.getRooms().stream().filter(
                room -> room.getId().equals(roomId)
        ).findFirst().map(ChatRoomApiAdaptor.ChatRoom::getHasUnread);
        assertTrue(optHasUnread.isPresent());
        return optHasUnread.get();
    }

    private boolean userHasUnreadMessages(String receiverUserToken) {
        RealtyApiUserHasUnreadMessages userHasUnreadMessagesResponse = apiV2.chatMessages().userHasUnreadMessagesRoute()
                .authorizationHeader(receiverUserToken)
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()))
                .getResponse();
        //noinspection ConstantConditions
        return userHasUnreadMessagesResponse.getHasUnreadMessages();
    }
}
