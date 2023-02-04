//
//  DevChatTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 21.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class DevChatTests: BaseTestCase {
    func testPhoneConfirmation() {
        let sendMessageExpectation = XCTestExpectation(
            description: "отправка сообщения после подтверждения номера"
        )
        let sendChatOpenedExpectation = XCTestExpectation(
            description: "отправка события открытия чата"
        )
        
        self.chatsStubConfigurator.setupListWithOneSiteDevChatRoom()
        self.chatsStubConfigurator.setupCommonSiteDevChatRoom()
        self.chatsStubConfigurator.setupPhoneConfirmationMessagesInSiteDevChatRoom()
        self.chatsStubConfigurator.setupSendMessageWithError(expectation: sendMessageExpectation)
        self.chatsStubConfigurator.setupSendSiteDevChatOpened(expectation: sendChatOpenedExpectation)
        
        APIStubConfigurator.setupPhoneBinding(using: self.dynamicStubs)
        APIStubConfigurator.setupPhoneConfirmation(using: self.dynamicStubs)
        
        self.relaunchApp(with: .chatRoomsListTests)
        
        ChatRoomsSteps()
            .isScreenPresented()
            .tapOnCell(row: 1)

        sendChatOpenedExpectation.yreEnsureFullFilledWithTimeout()

        ChatSteps()
            .isScreenPresented()
            .isContentViewPresented()
            .selectFirstButton()
        
        NewContactSteps()
            .isScreenPresented()
            .typeText("88005553535")
            .tapSendButton()
            .typeText("123456")
            .tapSendButton()
        
        sendMessageExpectation.yreEnsureFullFilledWithTimeout()
    }
    
    func testBlockedInputViewForSiteDisabledChat() {
        self.chatsStubConfigurator.setupListWithOneSiteDevChatRoom()
        self.chatsStubConfigurator.setupSiteDisabledChatDevChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInSiteDevChatRoom()
        let sendChatOpenedExpectation = XCTestExpectation(description: "отправка события открытия чата")
        self.chatsStubConfigurator.setupSendSiteDevChatOpened(expectation: sendChatOpenedExpectation)
        
        self.relaunchApp(with: .chatRoomsListTests)
        
        ChatRoomsSteps()
            .isScreenPresented()
            .tapOnCell(row: 1)
        
        sendChatOpenedExpectation.yreEnsureFullFilledWithTimeout()

        ChatSteps()
            .isScreenPresented()
            .isContentViewPresented()
            .tapCallOfferButton()
        
        SiteCardSteps()
            .isScreenPresented()
    }
    
    func testBlockedInputViewForOfferDisabledChat() {
        self.chatsStubConfigurator.setupListWithOneOfferDevChatRoom()
        self.chatsStubConfigurator.setupOfferDisabledChatDevChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInOfferDevChatRoom()
        let sendChatOpenedExpectation = XCTestExpectation(description: "отправка события открытия чата")
        self.chatsStubConfigurator.setupSendOfferDevChatOpened(expectation: sendChatOpenedExpectation)
        
        self.relaunchApp(with: .chatRoomsListTests)
        
        ChatRoomsSteps()
            .isScreenPresented()
            .tapOnCell(row: 1)
        
        sendChatOpenedExpectation.yreEnsureFullFilledWithTimeout()

        ChatSteps()
            .isScreenPresented()
            .isContentViewPresented()
            .tapCallOfferButton()
        
        OfferCardSteps()
            .isOfferCardPresented()
    }
    
    func testBlockedInputViewForOutdatedOfferDisabledChat() {
        self.chatsStubConfigurator.setupListWithOneOfferDevChatRoom()
        self.chatsStubConfigurator.setupOutdatedOfferDisabledChatDevChatRoom()
        self.chatsStubConfigurator.setupCommonMessagesInOfferDevChatRoom()
        let sendChatOpenedExpectation = XCTestExpectation(description: "отправка события открытия чата")
        self.chatsStubConfigurator.setupSendOfferDevChatOpened(expectation: sendChatOpenedExpectation)
        // @l-saveliy: Just some sites not empty list. Replace with any other if needed
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)
        
        self.relaunchApp(with: .chatRoomsListTests)
        
        ChatRoomsSteps()
            .isScreenPresented()
            .tapOnCell(row: 1)
        
        sendChatOpenedExpectation.yreEnsureFullFilledWithTimeout()

        ChatSteps()
            .isScreenPresented()
            .isContentViewPresented()
            .tapOpenOtherSitesButton()
        
        SearchResultsListSteps()
            .isScreenPresented()
            .withSiteList()
            .isListNonEmpty()
    }
    
    private lazy var chatsStubConfigurator = ChatsAPIStubConfigurator(dynamicStubs: self.dynamicStubs)
}
