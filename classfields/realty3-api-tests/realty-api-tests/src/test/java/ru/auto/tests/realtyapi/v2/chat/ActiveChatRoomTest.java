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
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.junit.Assert.*;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor.AllRoomsResponse;

@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Title("PUT /chat/room/{room_id}/mark/{action}")
public class ActiveChatRoomTest {

    @Inject
    private ChatRoomApiAdaptor chatRoomApiAdaptor;

    @Inject
    private ApiClient apiV2;

    @Test
    public void activeInactiveChat() {
        P2PChatRoomComponents p2pChat = chatRoomApiAdaptor.createP2pChatRoom();
        String testUserToken = p2pChat.getSellerToken();
        String roomId = p2pChat.getRoomId();

        RealtyApiChatBooleanActionStatus response = apiV2.chatRooms().markOfferBasedRoomByRoomIdRoute()
                .roomIdPath(roomId)
                .actionPath("inactive")
                .authorizationHeader(testUserToken)
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();
        //noinspection ConstantConditions
        assertEquals(p2pChat.getRoomId(), response.getId());
        assertTrue(response.getStatus());
        assertFalse(hasRoomInAllRoms(roomId, testUserToken));

        response = apiV2.chatRooms().markOfferBasedRoomByRoomIdRoute()
                .roomIdPath(roomId)
                .actionPath("active")
                .authorizationHeader(testUserToken)
                .reqSpec(authSpec())
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();
        //noinspection ConstantConditions
        assertEquals(p2pChat.getRoomId(), response.getId());
        assertTrue(response.getStatus());
        assertTrue(hasRoomInAllRoms(roomId, testUserToken));
    }

    private boolean hasRoomInAllRoms(String roomId, String userToken) {
        AllRoomsResponse allRoomsResponse = apiV2.chatRooms().getRoomsAllRoute()
                .authorizationHeader(userToken)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(AllRoomsResponse.class, GSON);
        return allRoomsResponse.getResponse().getRooms().stream().anyMatch(room -> room.getId().equals(roomId));
    }
}
