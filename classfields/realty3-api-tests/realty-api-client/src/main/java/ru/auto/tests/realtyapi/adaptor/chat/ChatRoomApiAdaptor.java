package ru.auto.tests.realtyapi.adaptor.chat;

import com.google.inject.Inject;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.UpdateExplicitUserResp;
import ru.auto.tests.realtyapi.responses.UserInnerInner;
import ru.auto.tests.realtyapi.v1.model.VosUserModel;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiError;
import ru.auto.tests.realtyapi.v2.model.VertisChatMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.model.VosUserModel.PaymentTypeEnum.NATURAL_PERSON;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

public class ChatRoomApiAdaptor {

    @Prod
    @Inject
    private ru.auto.tests.realtyapi.v1.ApiClient apiV1;

    @Prod
    @Inject
    private ApiClient apiV2;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Inject
    private FtlProcessor ftlProcessor;

    public P2PChatRoomComponents createP2pChatRoom() {
        return createP2pChatRoom(apiV2);
    }

    public P2PChatRoomComponents createP2pChatRoom(String offerFreemarkerTemplate) {
        return createP2pChatRoom(apiV2, offerFreemarkerTemplate);
    }

    public P2PChatRoomComponents createP2pChatRoom(ApiClient apiV2) {
        return createP2pChatRoom(apiV2, "offers/apartment_complex_reassignment_sell.ftl");
    }

    public String createOfferForChat(boolean enableChat) {
        return createOfferForChat("offers/apartment_complex_reassignment_sell.ftl", enableChat);
    }

    public P2PChatRoomComponents createP2pChatRoom(ApiClient apiV2, String offerFreemarkerTemplate) {
        Account sellerAccount = am.create();
        String sellerToken = oAuth.getToken(sellerAccount);
        UserInnerInner sellerVosUser = adaptor.vosUser(sellerToken).getResponse().getUser();
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        VosUserModel sellerUserModel = new VosUserModel()
                .login(sellerAccount.getId())
                .name(sellerAccount.getName())
                .type(0)
                .redirectPhones(true)
                .email(sellerVosUser.getEmail())
                .telephones(Collections.singletonList(sellerAccount.getPhone().get()))
                .paymentType(NATURAL_PERSON)
                // Allow communication through chats
                .addAllowedCommunicationChannelsItem("COM_CHATS");
        UpdateExplicitUserResp resp = apiV1.user().updateUserRoute()
                .reqSpec(authSpec())
                .authorizationHeader(sellerToken)
                .uidPath(sellerAccount.getId())
                .body(sellerUserModel)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()))
                .as(UpdateExplicitUserResp.class, GSON);
        String offerId = adaptor.createOffer(sellerToken, offerFreemarkerTemplate)
                .getResponse().getId();
        Account buyerAccount = am.create();
        String buyerToken = oAuth.getToken(buyerAccount);
        adaptor.waitOfferIsInSearcher(offerId);

        String roomId = apiV2.chatRooms().createOfferBasedRoomRoute()
                .offerIdPath(offerId)
                .authorizationHeader(buyerToken)
                .reqSpec(authSpec())
                // RealtyApiChatRoomResponse failing when parsing subject.offer.offerState.moderationInfo.opinion[].type = UNKNOWN
//                 .executeAs(validatedWith(shouldBe200OkJSON())).getResponse().getId();
                .execute(validatedWith(shouldBe200OkJSON())).as(ChatRoomResponse.class, GSON).getResponse().getId();

        return new P2PChatRoomComponents(sellerAccount, sellerToken, buyerAccount, buyerToken, offerId, roomId);
    }


    private String createOfferForChat(String offerFreemarkerTemplate, boolean enableChat) {
        Account sellerAccount = am.create();
        String sellerToken = oAuth.getToken(sellerAccount);
        UserInnerInner sellerVosUser = adaptor.vosUser(sellerToken).getResponse().getUser();
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        VosUserModel sellerUserModel = new VosUserModel()
                .login(sellerAccount.getId())
                .name(sellerAccount.getName())
                .type(0)
                .redirectPhones(true)
                .email(sellerVosUser.getEmail())
                .telephones(Collections.singletonList(sellerAccount.getPhone().get()))
                .paymentType(NATURAL_PERSON);

        if (enableChat) {
            sellerUserModel.addAllowedCommunicationChannelsItem("COM_CHATS");
        }
        apiV1.user().updateUserRoute()
                .reqSpec(authSpec())
                .authorizationHeader(sellerToken)
                .uidPath(sellerAccount.getId())
                .body(sellerUserModel)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()))
                .as(UpdateExplicitUserResp.class, GSON);
        String offerId = adaptor.createOffer(sellerToken, offerFreemarkerTemplate)
                .getResponse().getId();
        adaptor.waitOfferIsInSearcher(offerId);
        return offerId;
    }

    public VertisChatMessage sendMessage(String userToken, String roomId, String message) {
        return sendMessage(apiV2, userToken, roomId, message);
    }

    public VertisChatMessage sendMessage(ApiClient apiV2, String userToken, String roomId, String message) {
        return apiV2.chatMessages().sendMessageRoute()
                .authorizationHeader(userToken)
                .reqSpec(authSpec())
                .reqSpec(req -> req.setBody(processChatMessage(chatMessageData(roomId, message))))
                .executeAs(validatedWith(shouldBe200OkJSON())).getResponse();
    }

    public RealtyApiError sendMessageWithError(String userToken, String messageBody, int code) {
        return sendMessageWithError(apiV2, userToken, messageBody, code);
    }

    public RealtyApiError sendMessageWithError(ApiClient apiV2, String userToken, String messageBody, int code) {
        return apiV2.chatMessages().sendMessageRoute()
                .authorizationHeader(userToken)
                .reqSpec(authSpec())
                .reqSpec(req -> req.setBody(messageBody))
                .executeAs(validatedWith(shouldBeCode(code))).getError();
    }

    public String processChatMessage(Object data) {
        return ftlProcessor.process(data, "chat/message.ftl");
    }

    public static Map<String, String> chatMessageData(String roomId, String message) {
        return new HashMap<>() {{
            put("roomId", roomId);
            put("message", message);
        }};
    }

    public static class ChatRoomResponse {
        private ChatRoom response;

        public ChatRoom getResponse() {
            return response;
        }

        public void setResponse(ChatRoom response) {
            this.response = response;
        }
    }

    public static class User {
        private String id;

        private boolean mutedNotifications;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean getMutedNotifications() {
            return mutedNotifications;
        }

        public void setMutedNotifications(boolean mutedNotifications) {
            this.mutedNotifications = mutedNotifications;
        }
    }

    public static class ChatRoom {
        private String id;

        private List<User> users;

        private boolean hasUnread;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }

        public boolean getHasUnread() {
            return hasUnread;
        }

        public void setHasUnread(boolean hasUnread) {
            this.hasUnread = hasUnread;
        }
    }

    public static class AllRoomsResponse {
        private AllRooms response;

        public AllRooms getResponse() {
            return response;
        }

        public void setResponse(AllRooms response) {
            this.response = response;
        }
    }

    public static class AllRooms {
        private List<ChatRoom> rooms;

        public List<ChatRoom> getRooms() {
            return rooms;
        }

        public void setRooms(List<ChatRoom> rooms) {
            this.rooms = rooms;
        }
    }
}
