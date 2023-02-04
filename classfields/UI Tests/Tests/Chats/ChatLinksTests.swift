//
//  ChatLinksTests.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 28.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class ChatLinksTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        self.chatsStubConfigurator.setupListWithOneUserChatRoom()
        self.chatsStubConfigurator.setupCommonUserChatRoom()
    }

    func testOpenDeeplinkFromChat() {
        let linkMessage: String = "https://bzfk.adj.st/services?adj_t=4inllqd_xgufl8n&adj_deep_link=yandexrealty%3A%2F%2Fservices"
        self.chatsStubConfigurator.setupWebLinkMessageInUserChatRoom(with: linkMessage)
        self.openFirstUserChat()
            .tapFirstMessageLink()

        InAppServicesSteps()
            .isScreenPresented()
    }

    func testOpenYandexLinkFromChat() {
        let linkMessage: String = "https://arenda.yandex.ru"
        self.chatsStubConfigurator.setupWebLinkMessageInUserChatRoom(with: linkMessage)
        self.openFirstUserChat()
            .tapFirstMessageLink()

        WrappedBrowserSteps()
            .isEmbeddedBrowserPresented()
    }

    func testOpenUnknownLinkFromChat() {
        let linkMessage: String = "https://internet.com"
        self.chatsStubConfigurator.setupWebLinkMessageInUserChatRoom(with: linkMessage)
        self.openFirstUserChat()
            .tapFirstMessageLink()

        SafariSteps()
            .isOpened()
    }

    // MARK: - Private

    private lazy var chatsStubConfigurator = ChatsAPIStubConfigurator(dynamicStubs: self.dynamicStubs)

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
}
