//
//  TechSupportChatTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 15.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig

final class TechSupportChatTests: BaseTestCase {
    func testUnauthorizedUser() {
        self.enterChat(mode: .unauthorized)
            .isUnauthorizedViewPresented()
            .isLoginButtonHittable()
    }

    func testFirstLoadingError() {
        self.enterChat(mode: .failed)
            .isErrorViewPresented()
            .tapRetryButton()
    }

    func testEmptyChat() {
        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.chatsStubConfigurator.setupEmptyMesssagesInTechSupportRoom()

        self.enterChat()
            .isEmptyViewPresented()
            .compareEmptyViewWithSnapshot()
    }

    func testPagination() {
        let expectation = XCTestExpectation(description: "Запрос второй страницы чата")

        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.chatsStubConfigurator.setupTechSupportBigChat { isFirstPage in
            if !isFirstPage {
                expectation.fulfill()
            }
        }

        self.enterChat()
            .isContentViewPresented()
            .scrollChatToTop()

        expectation.yreEnsureFullFilledWithTimeout()
    }

    func testIncomingPhotoMessage() {
        self.chatsStubConfigurator.setupWebSocketSendingStringAndPhotoMessages()
        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.chatsStubConfigurator.setupEmptyMesssagesInTechSupportRoom()

        self.enterChat()
            .compareFirstPhotoMessageWithSnapshot(snapshotID: "techSupportChatTests.message.incomingPhoto")
    }

    func testTechSupportPoll() {
        let voteTechSupportPollExpectation = XCTestExpectation(description: "отправка ответа голосовалки")
        let sendTechSupportFeedbackExpectation = XCTestExpectation(description: "отправка фидбэка")

        self.chatsStubConfigurator.setupWebSocketSendingTechSupportPoll(
            techSupportPollSavedExpecation: voteTechSupportPollExpectation
        )
        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.chatsStubConfigurator.setupEmptyMesssagesInTechSupportRoom()
        self.chatsStubConfigurator.setupSendMessageWithError(expectation: sendTechSupportFeedbackExpectation)

        let chatSteps = self.enterChat()
            .rateFirstTechSupportPoll(rate: 3)

        voteTechSupportPollExpectation.yreEnsureFullFilledWithTimeout()

        chatSteps
            .compareFirstTechSupportPresetsWithSnapshot(snapshotID: "techSupportChatTests.message.techSupportPresets")
            .selectFirstTechSupportPreset()

        sendTechSupportFeedbackExpectation.yreEnsureFullFilledWithTimeout()
    }

    func testButtonsMessage() {
        self.chatsStubConfigurator.setupWebSocketSendingButtonsMessage()
        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.chatsStubConfigurator.setupEmptyMesssagesInTechSupportRoom()

        self.enterChat()
            .compareFirstButtonsMessageWithSnapshot(snapshotID: "techSupportChatTests.message.buttons")
            .selectFirstButton()
    }

    func testChatNewMessagePushFromNotificationSettings() {
        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.chatsStubConfigurator.setupEmptyMesssagesInTechSupportRoom()
        self.relaunchApp(with: .chatsTests)

        ProfileMenuSteps()
            .screenIsPresented()
            .tapOnNotificationSettings()

        self.allowNotificationsOnSettingsScreen()

        self.communicator.sendPush(.techSupportNewMessage)

        PushNotificationSteps().openCurrentPush()

        ChatSteps()
            .isScreenPresented()
    }

    func testChatNewMessagePushOnChatScreen() {
        let sendChatOpenedExpectation = XCTestExpectation(description: "отправка события открытия чата")
        self.chatsStubConfigurator.setupSendTechSupportChatOpened(expectation: sendChatOpenedExpectation)
        self.chatsStubConfigurator.setupEmptyTechSupportRoom()
        self.relaunchApp(with: .chatsTests)

        let profileMenuSteps = ProfileMenuSteps()
            .screenIsPresented()
            .tapOnNotificationSettings()

        self.allowNotificationsOnSettingsScreen()
            .tapBackButton()

        profileMenuSteps
            .tapOnTechSupportChatCell()

        sendChatOpenedExpectation.yreEnsureFullFilledWithTimeout()

        ChatSteps()
            .isScreenPresented()

        self.communicator.sendPush(.techSupportNewMessage)

        PushNotificationSteps()
            .isPushNotPresented()
    }

    private enum ChatEnterMode {
        case unauthorized
        case failed
        case successed
    }

    private lazy var chatsStubConfigurator = ChatsAPIStubConfigurator(dynamicStubs: self.dynamicStubs)

    private func enterChat(mode: ChatEnterMode = .successed) -> ChatSteps {
        let isAuthorized: Bool
        let shouldSendChatOpened: Bool
        switch mode {
            case .unauthorized:
                isAuthorized = false
                shouldSendChatOpened = false
            case .failed:
                isAuthorized = true
                shouldSendChatOpened = false
            case .successed:
                isAuthorized = true
                shouldSendChatOpened = true
        }

        let config = ExternalAppConfiguration.chatsTests
        config.isAuthorized = isAuthorized

        self.relaunchApp(with: config)

        ProfileMenuSteps()
            .screenIsPresented()
            .tapOnTechSupportChatCell()

        if shouldSendChatOpened {
            let expectation = XCTestExpectation(description: "отправка события открытия чата")
            self.chatsStubConfigurator.setupSendTechSupportChatOpened(expectation: expectation)
            expectation.yreEnsureFullFilledWithTimeout()
        }

        let chatSteps = ChatSteps()
            .isScreenPresented()

        return chatSteps
    }

    @discardableResult
    private func allowNotificationsOnSettingsScreen() -> NotificationSettingsSteps {
        let notificationsActivity = SystemDialogs.makePushNotificationsActivity(self)
            .optional()
            .activate()

        let settingSteps = NotificationSettingsSteps()
            .isScreenPresented()
            .enablePushNotificationsIfNeeded()

        notificationsActivity
            .tapOnButton(.allow)
            .deactivate()

        return settingSteps
    }
}
