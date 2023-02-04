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
import ru.yandex.qatools.allure.annotations.Title;

import static org.junit.Assert.assertNotNull;

@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Title("POST /chat/room/offer/{offer_id}")
public class ChatRoomTest {

    @Inject
    private ChatRoomApiAdaptor chatRoomApiAdaptor;

    @Inject
    private ApiClient apiV2;

    @Test
    public void createRoom() {
        P2PChatRoomComponents room = chatRoomApiAdaptor.createP2pChatRoom(apiV2);

        assertNotNull(room.getRoomId());
        assertNotNull(room.getOfferId());
        assertNotNull(room.getSellerAccount());
        assertNotNull(room.getBuyerAccount());
        assertNotNull(room.getSellerToken());
        assertNotNull(room.getBuyerToken());
    }
}
