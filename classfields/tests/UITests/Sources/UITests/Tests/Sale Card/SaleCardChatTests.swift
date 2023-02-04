//
//  SaleCardChatTests.swift
//  UITests
//
//  Created by Pavel Savchenkov on 20.10.2021.
//

import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on  AutoRuSaleCard AutoRuChat AutoRuNewLogin
final class BottomChatButtonOnSaleCardTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)
    private lazy var chatSteps = ChatSteps(context: self)

    override func setUp() {
        super.setUp()
    }

    func test_shouldOpenChatAndCheckCallButtonVisible() {
        mockChatWithReplyDelay(2000)

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.chatButton)
            }
            .should(provider: .chatScreen, .exist)
            .focus { chatRoom in
                chatRoom
                    .should(.delayLable("Отвечает редко, лучше позвонить\n\nПозвонить"), .exist)
            }
    }

    func test_shouldOpenChatAndCheckCallButtonNotVisible() {
        mockChatWithReplyDelay(370)

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.chatButton)
            }
            .should(provider: .chatScreen, .exist)
            .focus { chatRoom in
                chatRoom
                    .should(.delayLable("Отвечает в течение суток"), .exist)
            }
    }

    func test_shouldOpenChatWhenAuthUserTabChatButton() {
        mockChatRoom()

        let requestCreateChatRoom = expectationForRequest(method: "POST", uri: "/chat/room")

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.chatButton)
            }
            .shouldEventBeReported("Сообщения. Написать сообщение", with: ["Продавец": "Частник", "Источник": "Карточка объявления"])
            .should(provider: .chatScreen, .exist)
            .focus { chatRoom in
                chatRoom
                    .should(.presets, .exist)
                    .focus(on: .offerPanel, ofType: .chatOfferPanel) { element in
                        element.should(.title, .contain("Toyota Camry VI (XV40), 2008"))
                    }
                    .focus(on: .inputBar, ofType: .chatInputBar) { element in
                        element
                            .should(.hint, .exist)
                            .should(.text, .exist)
                    }
            }

        wait(for: [requestCreateChatRoom], timeout: 1)
    }

    func test_shouldOpenChatWhenNonAuthUserTabChatButtonAndLogin() {
        mockChatRoom()

        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_postAuthLoginOrRegister()
            .mock_postUserConfirm()

        let requestCreateChatRoom = expectationForRequest(method: "POST", uri: "/chat/room")
        let loginSteps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.chatButton)
            }
            .should(provider: .loginScreen, .exist)

        mocker
            .mock_user()
            .mock_getSession()
            .setForceLoginMode(.forceLoggedIn)

        loginSteps
            .focus { loginScreen in
                loginScreen.type("89850000000", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { codeInputScreen in
                codeInputScreen.type("0000", in: .codeInput)
            }
            .should(provider: .chatScreen, .exist)
            .focus { chatRoom in
                chatRoom
                    .should(.presets, .exist)
                    .focus(on: .offerPanel, ofType: .chatOfferPanel) { element in
                        element.should(.title, .contain("Toyota Camry VI (XV40), 2008"))
                    }
                    .focus(on: .inputBar, ofType: .chatInputBar) { element in
                        element
                            .should(.hint, .exist)
                            .should(.text, .exist)
                    }
            }

        wait(for: [requestCreateChatRoom], timeout: 1)
    }

    func test_shouldReturnToCardWhenTapChatButtonAndCancelAuth() {
        mockChatRoom()

        let requestCreateChatRoomNotSend = expectationForRequest(method: "POST", uri: "/chat/room")
        requestCreateChatRoomNotSend.isInverted = true

        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_postAuthLoginOrRegister()
            .mock_postUserConfirm()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.chatButton)
            }
            .should(provider: .loginScreen, .exist)
            .focus { loginScreen in
                loginScreen.tap(.closeButton)
            }
            .should(provider: .saleCardScreen, .exist)

        wait(for: [requestCreateChatRoomNotSend], timeout: 1)
    }

    func test_shouldOpenChatAndReturnToOffer() {
        mockChatRoom()

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.chatButton)
            }
            .should(provider: .chatScreen, .exist).focus { screen in
                screen.tap(.offerPanel)
            }
            .should(provider: .saleCardScreen, .exist)
    }

    private func mockChatRoom() {
        mocker
            .mock_base()
            .mock_getChatRoom()
            .mock_postChatRoom()
            .mock_offerFromHistoryLastAll()
            .mock_getChatMessage()
            .mock_getChatMessageSpam()
            .mock_deleteChatMessageUnread()
            .mock_dictionariesMessagePresets()
            .mock_dictionariesMessageHelloPresets()
            .mock_dictionariesSellerMessagePresets()
            .mock_dictionariesSellerMessageHelloPresets()
            .startMock()
    }

    private func mockChatWithReplyDelay(_ delay: Int32) {
        mocker
            .mock_base()
            .mock_offerFromHistoryLastAll()
            .startMock()

        api.chat.room
            .post
            .ok(mock: .file("chat_new_room", mutation: { resp in
                let users = resp.room.users
                    .filter({ $0.id == resp.room.subject.offer.owner })
                    .map { user -> Auto_Api_Chat_ChatUser in
                        var owner = user
                        owner.averageReplyDelayMinutes.value = delay
                        return owner
                    }
                resp.room.users = users
            }))
    }
}
