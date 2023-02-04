//
//  UserChatTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 24.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class UserChatTests: BaseTestCase {
    func testOpenOfferFromChat() {
        self.chatsStubConfigurator.setupListWithOneUserChatRoom()
        self.chatsStubConfigurator.setupCommonUserChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInUserChatRoom()

        self.openFirstUserChat()
            .tapOfferPanelView()

        OfferCardSteps()
            .isOfferCardPresented()
    }

    func testYaRentOfferChat() {
        self.chatsStubConfigurator.setupListWithOneRentChatRoom()
        self.chatsStubConfigurator.setupCommonRentChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInRentChatRoom()

        self.openFirstUserChat()
            .tapOfferPanelView()

        OfferCardSteps()
            .isOfferCardPresented()
    }

    func testChatActions() {
        self.chatsStubConfigurator.setupListWithOneUserChatRoom()
        self.chatsStubConfigurator.setupCommonUserChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInUserChatRoom()

        let chatSteps = self.openFirstUserChat()
        let chatActionsAlertSteps = ChatActionsAlertSteps()
        let confirmationAlertSteps = ChatActionConfirmationAlertSteps()

        for (action, shouldBeConfirmed) in self.chatActionsToTest {
            let expectation = self.prepareForChatActionTest(action: action, shouldBeConfirmed: shouldBeConfirmed)

            chatSteps.tapChatActionsButton()
            chatActionsAlertSteps
                .screenIsPresented()

            self.performChatAction(action, using: chatActionsAlertSteps)

            if let shouldBeConfirmed = shouldBeConfirmed {
                self.performConfirmationAction(shouldBeConfirmed: shouldBeConfirmed, using: confirmationAlertSteps)
            }

            expectation?.yreEnsureFullFilledWithTimeout()
        }
    }

    func testBlockChatLayout() {
        self.chatsStubConfigurator.setupListWithOneUserChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInUserChatRoom()

        self.relaunchApp(with: .chatRoomsListTests)

        let chatRoomsSteps = ChatRoomsSteps()
            .isContentViewPresented()

        let chatSteps = ChatSteps()

        for blockStatus in BlockStatus.allCases {
            let expectation = XCTestExpectation(description: "отправка события открытия чата")
            self.chatsStubConfigurator.setupSendUserChatOpened(expectation: expectation)
            self.setupBlockedRoom(for: blockStatus)
            chatRoomsSteps.tapOnCell(row: 1)

            expectation.yreEnsureFullFilledWithTimeout()

            switch blockStatus {
                case .blockedByMe:
                    self.unblockChatFromInputViewTest(using: chatSteps)
                case .blockedByOtherUser:
                    // nothing extra test
                    break
                case .meWasBanned:
                    self.openTechSupportFromInputViewTest(using: chatSteps)
                case .otherUserWasBanned:
                    // nothing extra test
                    break
                case .meChangedType:
                    self.openTechSupportFromInputViewTest(using: chatSteps)
                case .otherUserChangedType:
                    self.callOfferFromInputViewTest(using: chatSteps)
                    chatSteps.tapBackButton()
                case .otherUserDisabledChat:
                    self.callOfferFromInputViewTest(using: chatSteps)
                    chatSteps.tapBackButton()
                case .flatCanNoLongerBeRented:
                    // nothing extra test
                    break
                case .rentCallCenterClosedChat:
                    // nothing extra test
                    break
            }

            chatSteps.tapBackButton()
        }
    }

    func testSendMessageWithBlockError() {
        self.chatsStubConfigurator.setupListWithOneUserChatRoom()
        self.chatsStubConfigurator.setupCommonUserChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInUserChatRoom()
        self.chatsStubConfigurator.setupSendMessageWithOtherBlockedChatError()

        self.openFirstUserChat()
            .typeText("Test")
            .tapSendButton()
            .isBlockedInputViewPresented()
    }

    private enum ChatAction: CaseIterable {
        case mute
        case unmute
        case block
        case unblock
        case delete
    }

    private enum BlockStatus: CaseIterable {
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

    private lazy var chatsStubConfigurator = ChatsAPIStubConfigurator(dynamicStubs: self.dynamicStubs)

    private var chatActionsToTest: [(action: ChatAction, shouldBeConfirmed: Bool?)] {
        return [
            (action: .mute, shouldBeConfirmed: nil),
            (action: .unmute, shouldBeConfirmed: nil),
            (action: .block, shouldBeConfirmed: false),
            (action: .block, shouldBeConfirmed: true),
            (action: .unblock, shouldBeConfirmed: nil),
            (action: .delete, shouldBeConfirmed: false),
            (action: .delete, shouldBeConfirmed: true),
        ]
    }

    private func openFirstUserChat() -> ChatSteps {
        self.relaunchApp(with: .chatRoomsListTests)

        ChatRoomsSteps()
            .isScreenPresented()
            .isContentViewPresented()
            .tapOnCell(row: 1)

        let steps = ChatSteps()
            .isScreenPresented()
            .isContentViewPresented()

        return steps
    }

    private func prepareForChatActionTest(action: ChatAction, shouldBeConfirmed: Bool?) -> XCTestExpectation? {
        var expectation: XCTestExpectation? = nil
        switch (action, shouldBeConfirmed) {
            case (.mute, .none):
                expectation = XCTestExpectation(description: "Выключаем уведомления")
                self.chatsStubConfigurator.setupChatActionSuccess(action: .mute) { expectation?.fulfill() }
            case (.unmute, .none):
                expectation = XCTestExpectation(description: "Включаем уведомления")
                self.chatsStubConfigurator.setupChatActionSuccess(action: .unmute) { expectation?.fulfill() }
            case (.block, false):
                expectation = nil
                self.chatsStubConfigurator.setupChatActionSuccess(action: .block) { XCTFail("Действие не должно быть выполнено") }
            case (.block, true):
                expectation = XCTestExpectation(description: "Блокируем чат")
                self.chatsStubConfigurator.setupChatActionSuccess(action: .block) { expectation?.fulfill() }
            case (.unblock, .none):
                expectation = XCTestExpectation(description: "Разблокируем чат")
                self.chatsStubConfigurator.setupChatActionSuccess(action: .unblock) { expectation?.fulfill() }
            case (.delete, false):
                expectation = nil
                self.chatsStubConfigurator.setupChatActionSuccess(action: .delete) { XCTFail("Действие не должно быть выполнено") }
            case (.delete, true):
                expectation = XCTestExpectation(description: "Удаляем чат")
                self.chatsStubConfigurator.setupChatActionSuccess(action: .delete) { expectation?.fulfill() }
            default:
                XCTFail("Unexpected call")
        }

        return expectation
    }

    private func performChatAction(_ action: ChatAction, using steps: ChatActionsAlertSteps) {
        switch action {
            case .mute:
                steps.tapOnMuteButton()
            case .unmute:
                steps.tapOnUnmuteButton()
            case .block:
                steps.tapOnBlockButton()
            case .unblock:
                steps.tapOnUnblockButton()
            case .delete:
                steps.tapOnDeleteButton()
        }
    }

    private func performConfirmationAction(shouldBeConfirmed: Bool, using steps: ChatActionConfirmationAlertSteps) {
        steps.screenIsPresented()
        if shouldBeConfirmed {
            steps.pressYesButton()
        }
        else {
            steps.pressNoButton()
        }
    }

    private func setupBlockedRoom(for blockStatus: BlockStatus) {
        switch blockStatus {
            case .blockedByMe:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .blockedByMe)
            case .blockedByOtherUser:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .blockedByOtherUser)
            case .meWasBanned:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .meWasBanned)
            case .otherUserWasBanned:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .otherUserWasBanned)
            case .meChangedType:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .meChangedType)
            case .otherUserChangedType:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .otherUserChangedType)
            case .otherUserDisabledChat:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .otherUserDisabledChat)
            case .flatCanNoLongerBeRented:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .flatCanNoLongerBeRented)
            case .rentCallCenterClosedChat:
                self.chatsStubConfigurator.setupBlockedUserChatRoom(blockStatus: .rentCallCenterClosedChat)
        }
    }

    private func unblockChatFromInputViewTest(using chatSteps: ChatSteps) {
        let expectation = XCTestExpectation(description: "Разблокируем чат")
        
        self.chatsStubConfigurator.setupChatActionSuccess(action: .unblock) {
            expectation.fulfill()
        }
        
        chatSteps.tapUnblockChatButton()

        expectation.yreEnsureFullFilledWithTimeout()
    }

    private func openTechSupportFromInputViewTest(using chatSteps: ChatSteps) {
        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.chatsStubConfigurator.setupEmptyMesssagesInTechSupportRoom()

        chatSteps.tapOpenTechSupportButton()
        // @l-saveliy: make sure that it's empty tech support room
        chatSteps.isEmptyViewPresented()
        chatSteps.tapBackButton()
    }

    private func callOfferFromInputViewTest(using chatSteps: ChatSteps) {
        chatSteps.tapCallOfferButton()

        OfferCardSteps()
            .isOfferCardPresented()
    }
}
