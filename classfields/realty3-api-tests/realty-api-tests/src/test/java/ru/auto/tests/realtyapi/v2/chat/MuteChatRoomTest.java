package ru.auto.tests.realtyapi.v2.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor;
import ru.auto.tests.realtyapi.adaptor.chat.P2PChatRoomComponents;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.*;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.junit.Assert.*;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor.ChatRoomResponse;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor.ChatRoom;

import java.util.Optional;

@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Title("PUT /chat/room/{room_id}/mark/{action}")
public class MuteChatRoomTest {

    @Inject
    private ChatRoomApiAdaptor chatRoomApiAdaptor;

    @Inject
    private ApiClient apiV2;

    @Test
    public void muteUnmuteChat() {
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();
        String testUserToken = p2pChat.getSellerToken();
        String roomId = p2pChat.getRoomId();
        String testUserId = p2pChat.getSellerAccount().getId();

        RealtyApiChatBooleanActionStatus response = apiV2.chatRooms().markOfferBasedRoomByRoomIdRoute()
                .roomIdPath(roomId)
                .actionPath("mute")
                .authorizationHeader(testUserToken)
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();
        //noinspection ConstantConditions
        assertEquals(p2pChat.getRoomId(), response.getId());
        assertTrue(response.getStatus());
        assertTrue(isNotificationsMuted(roomId, testUserToken, testUserId));

        response = apiV2.chatRooms().markOfferBasedRoomByRoomIdRoute()
                .roomIdPath(roomId)
                .actionPath("unmute")
                .authorizationHeader(testUserToken)
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();
        //noinspection ConstantConditions
        assertEquals(p2pChat.getRoomId(), response.getId());
        assertTrue(response.getStatus());
        assertFalse(isNotificationsMuted(roomId, testUserToken, testUserId));
    }

    private boolean isNotificationsMuted(String roomId, String userToken, String userId) {
        ChatRoom chatRoomResponse = apiV2.chatRooms()
                .getOfferBasedRoomByRoomIdRoute()
                .roomIdPath(roomId)
                .authorizationHeader(userToken)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(ChatRoomResponse.class, GSON)
                .getResponse();

        Optional<Boolean> mutedNotifications = chatRoomResponse.getUsers().stream().filter(
                user -> user.getId().equals(userId)
        ).findFirst().map(ChatRoomApiAdaptor.User::getMutedNotifications);
        assertTrue(mutedNotifications.isPresent());
        return mutedNotifications.get();
    }
}
