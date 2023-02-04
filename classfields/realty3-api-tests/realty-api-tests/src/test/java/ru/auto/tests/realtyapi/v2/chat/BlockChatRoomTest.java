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
import ru.auto.tests.realtyapi.v2.model.RealtyApiChatBooleanActionStatus;
import ru.auto.tests.realtyapi.v2.model.RealtyApiError;
import ru.yandex.qatools.allure.annotations.Title;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.model.RealtyApiError.CodeEnum.CHAT_FORBIDDEN;

@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Title("PUT /chat/room/{room_id}/mark/{action}")
public class BlockChatRoomTest {

    @Inject
    private ChatRoomApiAdaptor chatRoomApiAdaptor;

    @Inject
    private ApiClient apiV2;

    @Test
    public void blockUnblockChat() {
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();

        RealtyApiChatBooleanActionStatus response = apiV2.chatRooms().markOfferBasedRoomByRoomIdRoute()
                .roomIdPath(p2pChat.getRoomId())
                .actionPath("block")
                .authorizationHeader(p2pChat.getSellerToken())
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();

        //noinspection ConstantConditions
        assertEquals(p2pChat.getRoomId(), response.getId());
        assertTrue(response.getStatus());

        RealtyApiError error = chatRoomApiAdaptor.sendMessageWithError(p2pChat.getBuyerToken(),
                chatRoomApiAdaptor.processChatMessage(ChatRoomApiAdaptor.chatMessageData(p2pChat.getRoomId(), "anything")), 403);

        assertEquals(CHAT_FORBIDDEN, error.getCode());
        assertEquals("sending messages to blocked rooms is forbidden", error.getMessage());


        response = apiV2.chatRooms().markOfferBasedRoomByRoomIdRoute()
                .roomIdPath(p2pChat.getRoomId())
                .actionPath("unblock")
                .authorizationHeader(p2pChat.getSellerToken())
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();

        //noinspection ConstantConditions
        assertEquals(p2pChat.getRoomId(), response.getId());
        assertTrue(response.getStatus());

        chatRoomApiAdaptor.sendMessage(p2pChat.getBuyerToken(), p2pChat.getRoomId(), "anything");
    }
}
