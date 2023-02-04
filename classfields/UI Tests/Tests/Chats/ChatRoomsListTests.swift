//
//  ChatRoomsListTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 14.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig

final class ChatRoomsListTests: BaseTestCase {
    func testUnauthorizedUser() {
        let config = ExternalAppConfiguration.chatRoomsListTests
        config.isAuthorized = false
        self.relaunchApp(with: config)

        ChatRoomsSteps()
            .isScreenPresented()
            .isUnauthorizedViewPresented()
            .isLoginButtonHittable()
    }

    func testFirstLoadingError() {
        self.relaunchApp(with: .chatRoomsListTests)

        let chatRoomsSteps = ChatRoomsSteps()
            .isScreenPresented()
            .isErrorViewPresented()

        let expectation = XCTestExpectation(description: "Повторный запрос списка чатов")
        self.chatsStubConfigurator.setupChatRoomsListError(expectation: expectation)

        chatRoomsSteps.tapRetryButton()

        expectation.yreEnsureFullFilledWithTimeout()
    }

    private lazy var chatsStubConfigurator = ChatsAPIStubConfigurator(dynamicStubs: self.dynamicStubs)
}
