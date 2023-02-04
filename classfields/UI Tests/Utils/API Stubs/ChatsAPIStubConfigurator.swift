//
//  ChatsAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 08.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YRETestsUtils
import Swifter
import XCTest

final class ChatsAPIStubConfigurator {
    enum ChatAction: String {
        case mute
        case unmute
        case block
        case unblock
        case delete = "inactive"
    }

    enum BlockStatus {
        case blockedByMe
        case blockedByOtherUser
        case meWasBanned
        case otherUserWasBanned
        case meChangedType
        case otherUserChangedType
        case otherUserDisabledChat
        case flatCanNoLongerBeRented
        case rentCallCenterClosedChat
    }

    init(dynamicStubs: HTTPDynamicStubs) {
        self.dynamicStubs = dynamicStubs
    }

    // MARK: - List

    func setupChatRoomsListError(expectation: XCTestExpectation) {
        let middleware = MiddlewareBuilder()
            .expectation(expectation)
            .respondWith(.response(.internalServerError))
            .build()

        self.dynamicStubs.register(method: .GET, path: Paths.chatRoomsList, middleware: middleware)
    }

    func setupListWithOneUserChatRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.chatRoomsList,
            filename: Stubs.Rooms.List.oneUserChat
        )
    }

    func setupListWithOneRentChatRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.chatRoomsList,
            filename: Stubs.Rooms.List.oneRentChat
        )
    }

    func setupListWithOneUserChatWithMissedOffer() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.chatRoomsList,
            filename: Stubs.Rooms.List.oneUserChatWithMissedOffer
        )
    }

    func setupListWithOneSiteDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.chatRoomsList, filename: Stubs.Rooms.List.oneSiteDevChat)
    }

    func setupListWithOneOfferDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.chatRoomsList, filename: Stubs.Rooms.List.oneOfferDevChat)
    }

    // MARK: - Chat

    func setupTechSupportRoomError(expectation: XCTestExpectation) {
        let middleware = MiddlewareBuilder()
            .expectation(expectation)
            .respondWith(.response(.internalServerError))
            .build()

        self.dynamicStubs.register(method: .GET, path: Paths.techSupportRoom, middleware: middleware)
    }

    func setupEmptyTechSupportRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.techSupportRoom,
            filename: Stubs.Rooms.TechSupport.empty
        )
    }

    func setupCommonTechSupportRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.techSupportRoom,
            filename: Stubs.Rooms.TechSupport.common
        )
    }

    func setupCommonUserChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.userChatRoom, filename: Stubs.Rooms.UserChat.common)
    }

    func setupCommonRentChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.rentChatRoom, filename: Stubs.Rooms.RentChat.common)
    }

    func setupCommonSiteDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.siteDevChatRoom, filename: Stubs.Rooms.DevChat.siteCommon)
    }

    func setupSiteDisabledChatDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.siteDevChatRoom, filename: Stubs.Rooms.DevChat.siteDisabledChat)
    }

    func setupOfferDisabledChatDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.offerDevChatRoom, filename: Stubs.Rooms.DevChat.offerDisabledChat)
    }

    func setupOutdatedOfferDisabledChatDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.offerDevChatRoom, filename: Stubs.Rooms.DevChat.outdatedOfferDisabledChat)
    }

    func setupOutdatedOfferUserChatRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.userChatRoom,
            filename: Stubs.Rooms.UserChat.outdatedOffer
        )
    }

    func setupMissingOfferUserChatRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.userChatRoom,
            filename: Stubs.Rooms.UserChat.missingOffer
        )
    }

    func setupBlockedUserChatRoom(blockStatus: BlockStatus) {
        let filename: String
        switch blockStatus {
            case .blockedByMe:
                filename = Stubs.Rooms.UserChat.blockedByMe
            case .blockedByOtherUser:
                filename = Stubs.Rooms.UserChat.blockedByOtherUser
            case .meWasBanned:
                filename = Stubs.Rooms.UserChat.meWasBanned
            case .otherUserWasBanned:
                filename = Stubs.Rooms.UserChat.otherUserWasBanned
            case .meChangedType:
                filename = Stubs.Rooms.UserChat.meChangedType
            case .otherUserChangedType:
                filename = Stubs.Rooms.UserChat.otherUserChangedType
            case .otherUserDisabledChat:
                filename = Stubs.Rooms.UserChat.otherUserDisabledChat
            case .flatCanNoLongerBeRented:
                filename = Stubs.Rooms.UserChat.flatCanNoLongerBeRented
            case .rentCallCenterClosedChat:
                filename = Stubs.Rooms.UserChat.rentCallCenterClosedChat
        }

        self.dynamicStubs.register(method: .GET, path: Paths.userChatRoom, filename: filename)
    }

    func setupCommonMessagesInUserChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.userChatMessages, filename: Stubs.Messages.UserChat.common)
    }

    func setupCommonMessagesInRentChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.rentChatMessages, filename: Stubs.Messages.RentChat.common)
    }

    func setupCommonMessagesInSiteDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.siteDevChatMessages, filename: Stubs.Messages.DevChat.siteCommon)
    }

    func setupCommonMessagesInOfferDevChatRoom() {
        self.dynamicStubs.register(method: .GET, path: Paths.offerDevChatMessages, filename: Stubs.Messages.DevChat.offerCommon)
    }

    func setupWebLinkMessageInUserChatRoom(with messageText: String) {
        let middleware = MiddlewareBuilder()
            .flatMap({ _, response, _ -> MiddlewareBuilder in
                let anyObject = ResourceProvider.jsonObject(from: Stubs.Messages.UserChat.webLinks)
                if var jsonObject = anyObject as? [AnyHashable: Any],
                   var response = jsonObject["response"] as? [AnyHashable: Any],
                   var messages = response["messages"] as? [[AnyHashable: Any]],
                   var message = messages.first,
                   var messagePayload = message["payload"] as? [AnyHashable: Any] {
                    messagePayload["value"] = messageText
                    message["payload"] = messagePayload
                    messages = [message]
                    response["messages"] = messages
                    jsonObject["response"] = response
                    return .respondWith(.ok(.init(generator: .json(jsonObject))))
                }
                else {
                    return .respondWith(.internalServerError())
                }
            })
            .build()

        self.dynamicStubs.register(method: .GET, path: Paths.userChatMessages, middleware: middleware)
    }

    func setupPhoneConfirmationMessagesInSiteDevChatRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.siteDevChatMessages,
            filename: Stubs.Messages.DevChat.sitePhoneConfirmation
        )
    }

    func setupEmptyMesssagesInTechSupportRoom() {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.techSupportMessages,
            filename: Stubs.Messages.TechSupport.empty
        )
    }

    func setupTechSupportBigChat(pageCallback: @escaping (_ isFirstPage: Bool) -> Void) {
        let middleware = MiddlewareBuilder()
            .flatMap({ request, _, _ -> MiddlewareBuilder in
                let isFirstPage = !request.queryParams.contains(where: { (key, value) -> Bool in
                    return key == "from" && !value.isEmpty
                })

                pageCallback(isFirstPage)

                if isFirstPage {
                    return .respondWith(.ok(.contentsOfJSON(Stubs.Messages.TechSupport.bigChatPage1)))
                }
                else {
                    return .respondWith(.ok(.contentsOfJSON(Stubs.Messages.TechSupport.bigChatPage2)))
                }
            })
            .build()

        self.dynamicStubs.register(
            method: .GET,
            path: Paths.techSupportMessages,
            middleware: middleware
        )
    }

    // @l-saveliy: Just check that we try to send message
    func setupSendMessageWithError(expectation: XCTestExpectation) {
        let middleware = MiddlewareBuilder()
            .expectation(expectation)
            .respondWith(.internalServerError())
            .build()

        self.dynamicStubs.register(method: .POST, path: Paths.sendMessage, middleware: middleware)
    }

    func setupSendMessageWithOtherBlockedChatError() {
        let middleware = MiddlewareBuilder()
            .respondWith(.forbidden(errorJSONFileName: Stubs.Messages.UserChat.sendMessageOtherBlockedChat))
            .build()

        self.dynamicStubs.register(method: .POST, path: Paths.sendMessage, middleware: middleware)
    }

    func setupSendSiteDevChatOpened(expectation: XCTestExpectation) {
        self.setupSendChatOpened(expectation: expectation, path: Paths.sendSiteDevChatOpened)
    }

    func setupSendOfferDevChatOpened(expectation: XCTestExpectation) {
        self.setupSendChatOpened(expectation: expectation, path: Paths.sendOfferDevChatOpened)
    }

    func setupSendUserChatOpened(expectation: XCTestExpectation) {
        self.setupSendChatOpened(expectation: expectation, path: Paths.sendUserChatOpened)
    }

    func setupSendTechSupportChatOpened(expectation: XCTestExpectation) {
        self.setupSendChatOpened(expectation: expectation, path: Paths.sendTechSupportChatOpened)
    }

    func setupWebSocketSendingStringAndPhotoMessages() {
        self.setupWebSocket { [weak self] session in
            guard let strongSelf = self else { return }
            strongSelf.sendSocketMessage(session: session, filename: Stubs.Websocket.TechSupport.incomingTextMessage)
            strongSelf.sendSocketMessage(session: session, filename: Stubs.Websocket.TechSupport.incomingPhotoMessage)
        }
    }

    func setupWebSocketSendingTechSupportPoll(techSupportPollSavedExpecation: XCTestExpectation) {
        self.setupWebSocket { [weak self] session in
            guard let strongSelf = self else { return }
            strongSelf.setupTechSupportPollSaved(expectation: techSupportPollSavedExpecation, session: session)
            strongSelf.sendSocketMessage(session: session, filename: Stubs.Websocket.TechSupport.poll)
        }
    }

    func setupWebSocketSendingButtonsMessage() {
        self.setupWebSocket { [weak self] session in
            guard let strongSelf = self else { return }
            strongSelf.sendSocketMessage(session: session, filename: Stubs.Websocket.TechSupport.buttonsMessage)
        }
    }

    func setupChatActionSuccess(action: ChatAction, handler: @escaping () -> Void) {
        let middleware = MiddlewareBuilder()
            .callback { _ in handler() }
            .respondWith(.ok(.contentsOfJSON(Stubs.successUserChatAction)))
            .build()
        self.dynamicStubs.register(method: .PUT, path: Paths.chatActionPrefix + action.rawValue, middleware: middleware)
    }

    private enum Stubs {
        enum Rooms {
            enum TechSupport {
                static let empty = "chat-room-tech-support-emptyChat.debug"
                static let common = "chat-room-tech-support-commonChat.debug"
            }

            enum UserChat {
                static let common = "chat-room-userChat-common.debug"
                static let unread = "chat-room-userChat-unread.debug"
                static let outdatedOffer = "chat-room-userChat-outdatedOffer.debug"
                static let missingOffer = "chat-room-userChat-missingOffer.debug"
                static let blockedByMe = "chat-room-userChat-blockedByMe.debug"
                static let blockedByOtherUser = "chat-room-userChat-blockedByOtherUser.debug"
                static let meWasBanned = "chat-room-userChat-meWasBanned.debug"
                static let otherUserWasBanned = "chat-room-userChat-otherUserWasBanned.debug"
                static let meChangedType = "chat-room-userChat-meChangedType.debug"
                static let otherUserChangedType = "chat-room-userChat-otherUserChangedType.debug"
                static let otherUserDisabledChat = "chat-room-userChat-otherUserDisabledChat.debug"
                static let flatCanNoLongerBeRented = "chat-room-userChat-flatCanNoLongerBeRented.debug"
                static let rentCallCenterClosedChat = "chat-room-userChat-rentCallCenterClosedChat.debug"
            }

            enum RentChat {
                static let common = "chat-room-rentChat-common.debug"
            }

            enum DevChat {
                static let siteCommon = "chat-room-devChat-siteCommon.debug"
                static let siteDisabledChat = "chat-room-devChat-siteDisabledChat.debug"
                static let offerDisabledChat = "chat-room-devChat-offerDisabledChat.debug"
                static let outdatedOfferDisabledChat = "chat-room-devChat-outdatedOfferDisabledChat.debug"
            }

            enum List {
                static let justTechSupport = "chat-rooms-list-all-justTechSupport.debug"
                static let oneUserChat = "chat-rooms-list-all-oneUserChat.debug"
                static let oneRentChat = "chat-rooms-list-all-oneRentChat.debug"
                static let oneUserChatWithMissedOffer = "chat-rooms-list-all-oneUserChatWithMissedOffer.debug"
                static let oneSiteDevChat = "chat-rooms-list-all-oneSiteDevChat.debug"
                static let oneOfferDevChat = "chat-rooms-list-all-oneOfferDevChat.debug"
            }
        }

        enum Messages {
            enum TechSupport {
                static let empty = "chat-messages-tech-support-emptyChat.debug"
                static let bigChatPage1 = "chat-messages-tech-support-bigChatPage1.debug"
                static let bigChatPage2 = "chat-messages-tech-support-bigChatPage2.debug"
                static let sendMessage = "chat-messages-sendMessage.debug"
            }

            enum UserChat {
                static let common = "chat-messages-userChat-common.debug"
                static let webLinks = "chat-messages-userChat-weblinks.debug"
                static let sendMessageOtherBlockedChat = "chat-messages-sendMessageOtherBlockedChat.debug"
            }

            enum DevChat {
                static let siteCommon = "chat-messages-devChat-siteCommon.debug"
                static let sitePhoneConfirmation = "chat-messages-devChat-sitePhoneConfirmation.debug"
                static let offerCommon = "chat-messages-devChat-offerCommon.debug"
            }

            enum RentChat {
                static let common = "chat-messages-rentChat-common.debug"
            }
        }

        enum Websocket {
            enum TechSupport {
                static let incomingTextMessage = "websocket-message-textMessage.debug"
                static let incomingPhotoMessage = "websocket-message-photoMessage.debug"
                static let poll = "websocket-message-techSupportPollMessage.debug"
                static let feedbackRequest = "websocket-message-feedbackRequestMessage.debug"
                static let buttonsMessage = "websocket-message-buttonsMessage.debug"
            }

            enum UserChat {
                static let incomingTextMessage = "websocket-message-userChat-textMessage.debug"
            }

            static let connect = "device-websocket-connect.debug"
        }

        static let techSupportPollSaved = "chat-tech-support-poll-saved.debug"
        static let successUserChatAction = "chat-userChat-action-successResult.debug"
    }

    private enum Paths {
        static let chatRoomsList = "2.0/chat/rooms/list/all"
        static let techSupportRoom = "/2.0/chat/room/tech-support"
        static let userChatRoom = "2.0/chat/room/\(Self.userChatRoomID)"
        static let rentChatRoom = "2.0/chat/room/\(Self.rentChatRoomID)"
        static let siteDevChatRoom = "2.0/chat/room/\(Self.siteDevChatRoomID)"
        static let offerDevChatRoom = "2.0/chat/room/\(Self.offerDevChatRoomID)"
        static let techSupportMessages = "/2.0/chat/messages/room/\(Self.techSupportRoomID)"
        static let userChatMessages = "2.0/chat/messages/room/\(Self.userChatRoomID)"
        static let rentChatMessages = "2.0/chat/messages/room/\(Self.rentChatRoomID)"
        static let siteDevChatMessages = "2.0/chat/messages/room/\(Self.siteDevChatRoomID)"
        static let offerDevChatMessages = "2.0/chat/messages/room/\(Self.offerDevChatRoomID)"
        static let sendMessage = "2.0/chat/messages"
        static let requestWebsocketURL = "1.0/device/websocket"
        static let techSupportPoll = "2.0/chat/tech-support/poll/\(Self.techSupportPollHash)"
        static let chatActionPrefix = "2.0/chat/room/\(Self.userChatRoomID)/mark/"

        static let sendSiteDevChatOpened = "2.0/chat/room/\(Self.siteDevChatRoomID)/open"
        static let sendOfferDevChatOpened = "2.0/chat/room/\(Self.offerDevChatRoomID)/open"
        static let sendUserChatOpened = "2.0/chat/room/\(Self.userChatRoomID)/open"
        static let sendTechSupportChatOpened = "2.0/chat/room/\(Self.techSupportRoomID)/open"

        // @l-saveliy: Just some url for test http://127.0.0.1:8080/path/to/connect/websocket
        // Returned from server in device-websocket-connect.debug.json
        static let websocketConnection = "/path/to/connect/websocket"

        // @l-saveliy: Should be the same as in "chat-room-tech-support-emptyChat.debug.json"
        private static let techSupportRoomID = "techSupportRoomID"

        // @l-saveliy: Should be the same as in "websocket-message-techSupportPollMessage.debug.json"
        private static let techSupportPollHash = "techSupportPollHash"

        // @l-saveliy: Should be the same as in "chat-room-userChat-unread.debug.json"
        private static let userChatRoomID = "userChatRoomID"

        // @l-saveliy: Should be the same as in "chat-room-devChat-oneSiteDevChat.debug.json"
        private static let siteDevChatRoomID = "siteDevChatRoomID"

        // @l-saveliy: Should be the same as in "chat-room-devChat-oneOfferDevChat.debug.json"
        private static let offerDevChatRoomID = "offerDevChatRoomID"

        private static let rentChatRoomID = "rentChatRoomID"
    }

    private let dynamicStubs: HTTPDynamicStubs

    private func setupWebSocket(onConnect: @escaping (WebSocketSession) -> Void) {
        self.dynamicStubs.register(
            method: .GET,
            path: Paths.requestWebsocketURL,
            filename: Stubs.Websocket.connect
        )
        self.dynamicStubs.registerWebSocket(path: Paths.websocketConnection, onConnect: onConnect)
    }


    private func sendSocketMessage(session: WebSocketSession, filename: String) {
        let string = ResourceProvider.jsonString(from: filename)
        session.writeText(string)
    }

    private func setupTechSupportPollSaved(expectation: XCTestExpectation, session: WebSocketSession) {
        let middleware = MiddlewareBuilder()
            .expectation(expectation)
            .callback { [weak self] _ in
                self?.sendSocketMessage(session: session, filename: Stubs.Websocket.TechSupport.feedbackRequest)
            }
            .respondWith(.ok(.contentsOfJSON(Stubs.techSupportPollSaved)))
            .build()

        self.dynamicStubs.register(
            method: .PUT,
            path: Paths.techSupportPoll,
            middleware: middleware
        )
    }

    private func setupSendChatOpened(expectation: XCTestExpectation, path: String) {
        let middleware = MiddlewareBuilder()
            .expectation(expectation)
            .respondWith(.internalServerError())
            .build()

        self.dynamicStubs.register(method: .PUT, path: path, middleware: middleware)
    }
}
