//
//  QuickReplyApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 14.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class QuickReplyApplication: QuickReply, SmartReply {
    private let messageViewPage = MessageViewPage()
    private var quickReplyTextFieldHeight: CGFloat = 31.0
    
    public func tapOnTextField() throws {
        try XCTContext.runActivity(named: "Tap on quick reply text field") { _ in
            try self.messageViewPage.quickReplyTextField.tapCarefully()
        }
    }
    
    public func isQuickReplyTextFieldExpanded() throws -> Bool {
        XCTContext.runActivity(named: "Check is Quick reply text field expanded") { _ in
            return self.messageViewPage.quickReplyTextField.frame.height > quickReplyTextFieldHeight
        }
    }
    
    public func setTextFieldValue(_ message: String) throws {
        XCTContext.runActivity(named: "Set value to quick reply text field") { _ in
            self.messageViewPage.quickReplyTextField.typeText(message)
        }
    }
    
    public func pasteTextFieldValue(_ message: String) throws {
        try XCTContext.runActivity(named: "Paste value to quick reply text field") { _ in
            try self.messageViewPage.quickReplyTextField.pasteText(message)
        }
    }
    
    public func getTextFieldValue() throws -> String {
        XCTContext.runActivity(named: "Get value of quick reply text field") { _ in
            let quickReplyTextField = self.messageViewPage.quickReplyTextField
            let label = quickReplyTextField.value as! String
            let placeholder = quickReplyTextField.placeholderValue!
            return label == placeholder ? "" : label
        }
    }
    
    public func tapOnComposeButton() throws {
        try XCTContext.runActivity(named: "Tap on quick reply compose button") { _ in
            try self.messageViewPage.quickReplyComposeButton.tapCarefully()
        }
    }
    
    public func tapOnSendButton() throws {
        try XCTContext.runActivity(named: "Tap on quick reply send button") { _ in
            try self.messageViewPage.quickReplySendButton.tapCarefully()
        }
    }
    
    public func isSendButtonEnabled() throws -> Bool {
        XCTContext.runActivity(named: "Check is quick reply send button enabled") { _ in
            return self.messageViewPage.quickReplySendButton.isEnabled
        }
    }
    
    public func isQuickReplyShown() throws -> Bool {
        XCTContext.runActivity(named: "Check is quick reply shown") { _ in
            return self.messageViewPage.quickReplyTextField.exists
        }
    }
    
    public func tapOnSmartReply(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Tap on smart reply with index \(order)") { _ in
            try self.messageViewPage.smartReplyItemBy(index: Int(order)).tap()
        }
    }
    
    public func getSmartReply(_ order: Int32) throws -> String {
        XCTContext.runActivity(named: "Get smart reply with index \(order)") { _ in
            self.messageViewPage.smartReplyItemBy(index: Int(order)).label.label
        }
    }
    
    public func getSmartReplies() throws -> YSArray<String> {
        XCTContext.runActivity(named: "Get smart replies") { _ in
            return YSArray(array: self.messageViewPage.smartReplies)
        }
    }
    
    public func closeSmartReply(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Close smart reply with index \(order)") { _ in
            try self.messageViewPage.smartReplyItemBy(index: Int(order)).closeButton.tapCarefully()
        }
    }
    
    public func closeAllSmartReplies() throws {
        try XCTContext.runActivity(named: "Close all smart replies") { _ in
            guard let closeAll = self.messageViewPage.smartRepliesCloseAll else {
                throw YSError("There is no Close all button")
            }
            try closeAll.tap()
        }
    }
    
    public func isSmartRepliesShown() throws -> Bool {
        XCTContext.runActivity(named: "Check is smart replies shown") { _ in
            return self.messageViewPage.smartReplyContainer.exists
        }
    }
}
