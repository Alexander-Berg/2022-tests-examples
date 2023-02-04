package ru.auto.tests.realtyapi.v2.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.junit.Assert.*;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Title("GET /chat/rooms/list/all")
public class TechSupportChatTest {
    @Inject
    private AccountManager am;

    @Inject
    private ApiClient apiV2;

    @Inject
    private OAuth oAuth;

    @Test
    @DisplayName("Получение всех чатов с чатом тех поддержки")
    public void techSupportChat() {
        Account testAccount = am.create();
        String testUserToken;
        testUserToken = oAuth.getToken(testAccount);

        assertFalse(hasTechSupportRoom(testUserToken, false));
        assertTrue(hasTechSupportRoom(testUserToken, true));
    }

    boolean hasTechSupportRoom(String userToken, boolean includeTechSupport) {
        return apiV2.chatRooms().getRoomsAllRoute()
                .authorizationHeader(userToken)
                .reqSpec(authSpec())
                .includeTechSupportQuery(includeTechSupport)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(ChatRoomApiAdaptor.AllRoomsResponse.class, GSON)
                .getResponse()
                .getRooms().stream().anyMatch(
                        room -> room.getUsers().stream().anyMatch(
                                user -> user.getId().equals("techSupport")
                        )
                );
    }
}
