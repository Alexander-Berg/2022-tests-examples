package ru.auto.tests.realtyapi.v2.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.chat.ChatRoomApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.ra.ResponseSpecBuilders;
import ru.auto.tests.realtyapi.v2.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe400NoDeveloperWithChat;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe403ForbiddenForOfferWithoutChat;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404SiteNonFound;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class RoomReachableTest {

    @Inject
    private ChatRoomApiAdaptor chatRoomApiAdaptor;
    @Inject
    private AccountManager am;
    @Inject
    private ApiClient apiV2;
    @Inject
    private OAuth oAuth;


    private Account buyerAccount;
    private String buyerToken;
    private String offerIdWithoutChat;
    private String offerIdWithChat;

    private String siteWithDisabledChatsId = "2629550";
    private String nonExistentSite = "0";
    private String siteWithEnabledChatsId = "14921";

    private String developerOfferWithDisabledChatsId = "426887462994420138";
    private String developerOfferWithEnabledChatsId = "1676259269676770430";

    private List<String> roomsIds = new ArrayList<>();

    @Before
    public void createBuyer() {
        buyerAccount = am.create();
        buyerToken = oAuth.getToken(buyerAccount);
        offerIdWithoutChat = chatRoomApiAdaptor.createOfferForChat(false);
        offerIdWithChat = chatRoomApiAdaptor.createOfferForChat(true);
    }

    @Test
    public void runRoomReachableTests() {
        //check rooms for new user
        checkAllRoomsIsEmpty();

        //try to open forbidden rooms (expect error)
        checkThatWeCantFindChatInNewOffer();
        tryCreateOfferChatWithDisabledMessages();
        tryCreateDeveloperSiteChatWithDisabledMessages();
        tryCreateChatToNonExistentSite();

        //open correct rooms
        createChatForOfferWithChat();
        createDeveloperSiteChatWithEnabledMessages();

        //check rooms count and close
        checkAllRoomsAfterCreateChats();
        removeRooms();
        checkAllRoomsIsEmpty();
    }


    private void checkThatWeCantFindChatInNewOffer() {
        apiV2.chatRooms().getOfferBasedRoomByUserIdOfferIdRoute()
                .offerIdPath(offerIdWithoutChat)
                .authorizationHeader(buyerToken)
                .reqSpec(authSpec())
                .execute(validatedWith(ResponseSpecBuilders.shouldBe404ChatNotFound()))
                .as(ChatRoomApiAdaptor.ChatRoomResponse.class, GSON);
    }

    private void checkAllRoomsIsEmpty() {
        ChatRoomApiAdaptor.AllRooms response = apiV2.chatRooms()
                .getRoomsAllRoute()
                .authorizationHeader(buyerToken)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(ChatRoomApiAdaptor.AllRoomsResponse.class, GSON)
                .getResponse();
        assertTrue(response.getRooms().isEmpty());
    }

    private void tryCreateOfferChatWithDisabledMessages() {
        apiV2.chatRooms()
                .createOfferBasedRoomRoute()
                .authorizationHeader(buyerToken)
                .offerIdPath(offerIdWithoutChat)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe403ForbiddenForOfferWithoutChat(offerIdWithoutChat)));
    }

    private void tryCreateDeveloperSiteChatWithDisabledMessages() {
        apiV2.chatRooms()
                .createDeveloperSiteRoomRoute()
                .authorizationHeader(buyerToken)
                .siteIdPath(siteWithDisabledChatsId)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe400NoDeveloperWithChat(siteWithDisabledChatsId)));
    }

    private void tryCreateChatToNonExistentSite() {
        apiV2.chatRooms()
                .createDeveloperSiteRoomRoute()
                .authorizationHeader(buyerToken)
                .siteIdPath(nonExistentSite)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe404SiteNonFound(nonExistentSite)));
    }


    private void createChatForOfferWithChat() {
        String roomId = apiV2.chatRooms()
                .createOfferBasedRoomRoute()
                .authorizationHeader(buyerToken)
                .offerIdPath(offerIdWithChat)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(ChatRoomApiAdaptor.ChatRoomResponse.class, GSON)
                .getResponse().getId();
        roomsIds.add(roomId);
    }

    private void createDeveloperSiteChatWithEnabledMessages() {
        String roomId = apiV2.chatRooms()
                .createDeveloperSiteRoomRoute()
                .authorizationHeader(buyerToken)
                .siteIdPath(siteWithEnabledChatsId)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(ChatRoomApiAdaptor.ChatRoomResponse.class, GSON)
                .getResponse().getId();
        roomsIds.add(roomId);
    }


    private void checkAllRoomsAfterCreateChats() {
        ChatRoomApiAdaptor.AllRooms response = apiV2.chatRooms()
                .getRoomsAllRoute()
                .authorizationHeader(buyerToken)
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(ChatRoomApiAdaptor.AllRoomsResponse.class, GSON)
                .getResponse();
        List<String> responseRoomIds = response.getRooms().stream().map(
                ChatRoomApiAdaptor.ChatRoom::getId).sorted().collect(Collectors.toList());
        assertEquals(responseRoomIds, roomsIds.stream().sorted().collect(Collectors.toList()));
    }

    private void removeRooms() {
        for (String roomId : roomsIds) {
            apiV2.chatRooms()
                    .removeOfferBasedRoomByRoomIdRoute()
                    .authorizationHeader(buyerToken)
                    .reqSpec(authSpec())
                    .roomIdPath(roomId)
                    .execute(validatedWith(shouldBe200OkJSON()));
        }
    }


}
