//
//  ComposeApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 16.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import XCTest

public final class ComposeApplication: Compose, ComposeRecipientFields, ComposeRecipientSuggest, ComposeSenderSuggest, ComposeSubject, ComposeBody {

    private let messageListPage = MessageListPage()
    private let composeViewPage = ComposeViewPage()
    private let messageViewPage = MessageViewPage()
    private let messageActionsPage = MessageActionsPage()

    public func openCompose() throws {
        try XCTContext.runActivity(named: "Opening Compose") {_ in
            try self.messageListPage.fab.tapCarefully()
        }
    }

    public func sendMessage() throws {
        try XCTContext.runActivity(named: "Sending message") {_ in
            try self.composeViewPage.sendButton.tapCarefully()
        }
    }

    public func isSendButtonEnabled() throws -> Bool {
        XCTContext.runActivity(named: "Check is send button enabled") {_ in
            return self.composeViewPage.sendButton.isEnabled
        }
    }

    public func closeCompose(_ saveDraft: Bool) throws {
        try XCTContext.runActivity(named: "Closing Compose") {_ in
            try self.composeViewPage.closeButton.tapCarefully()
            saveDraft
                ? try self.composeViewPage.saveDraft.tapCarefully()
                : try self.composeViewPage.deleteDraft.tapCarefully()
        }
    }

    public func isComposeOpened() throws -> Bool {
        XCTContext.runActivity(named: "Check is Compose opened") {_ in
            return self.composeViewPage.view.exists
        }
    }

    public func tapOnRecipientField(_ field: ComposeRecipientFieldType) throws {
        try XCTContext.runActivity(named: "Tap on \(field)") {_ in
            let element = field.toXCUIElement
            try element.forceTap()
        }
    }

    public func pasteToRecipientField(_ field: ComposeRecipientFieldType, _ value: String, _ generateYabble: Bool) throws {
        try XCTContext.runActivity(named: "Paste \(value) to \(field)") {_ in
            let element = field.toXCUIElement
            try element.pasteText(value)
            if generateYabble {
                element.tapOnReturnButton()
            }
        }
    }

    public func setRecipientField(_ field: ComposeRecipientFieldType, _ value: String, _ generateYabble: Bool) throws {
        XCTContext.runActivity(named: "Set \(value) to \(field). Generate yabble: \(generateYabble)") {_ in
            let element = field.toXCUIElement
            element.typeText(value)
            if generateYabble {
                self.composeViewPage.suggestView.waitForHittable()
                element.tapOnReturnButton()
            }
        }
    }

    public func generateYabbleByTapOnEnter() throws {
        XCTContext.runActivity(named: "Generating yabble") {_ in
            self.composeViewPage.keyboard.buttons["Return"].tap()
        }
    }

    public func getRecipientFieldValue(_ field: ComposeRecipientFieldType) throws -> YSArray<Yabble> {
        XCTContext.runActivity(named: "Get \(field) field value") {_ in
            let yabbles = self.composeViewPage.yabbles(from: field.toXCUIElement).filter { !$0.isNewYabbleWithoutLabel }.map { $0.toModelYabble }
            return YSArray(array: yabbles)
        }
    }

    public func getCompactRecipientFieldValue() throws -> String {
        XCTContext.runActivity(named: "Get to field value with minimized Cc Bcc From fields") {_ in
            let yabbleLabels = self.composeViewPage.shownYabblesLabels
            let moreLabelElement = self.composeViewPage.moreLabel
            return !moreLabelElement.exists || (moreLabelElement.exists && moreLabelElement.label.isEmpty)
                ? yabbleLabels
                : "\(yabbleLabels) \(moreLabelElement.label)"
        }
    }

    public func tapOnRecipient(_ field: ComposeRecipientFieldType, _ index: Int32) throws {
        try XCTContext.runActivity(named: "Tap on recipient in \(field) field by \(index)") {_ in
            try self.composeViewPage.yabble(from: field.toXCUIElement, by: Int(index)).tap()
        }
    }

    public func deleteRecipientByTapOnCross(_ field: ComposeRecipientFieldType, _ index: Int32) throws {
        try XCTContext.runActivity(named: "Deleting recipient in \(field) field at \(index) position by tap on cross") {_ in
            try self.composeViewPage.yabble(from: field.toXCUIElement, by: Int(index)).deleteButton.tapCarefully()
        }
    }

    public func deleteLastRecipientByTapOnBackspace(_ field: ComposeRecipientFieldType) throws {
        try XCTContext.runActivity(named: "Deleting last recipient in \(field) field by tap on backspace") {_ in
            guard let recipient = self.composeViewPage.yabbles(from: field.toXCUIElement).last(where: { !$0.isNewYabbleWithoutLabel })?.toModelYabble else {
                throw YSError("There is no yabbles in field \(field)")
            }
            field.toXCUIElement.deleteCharacters(count: recipient.type == YabbleType.suggested ? 2 : Int(recipient.emailOrName().count) + 2 )
        }
    }

    public func tapOnSenderField() throws {
        try XCTContext.runActivity(named: "Tap on from field") {_ in
            try self.composeViewPage.fromField.tapCarefully()
        }
    }

    public func getSenderFieldValue() throws -> String {
        XCTContext.runActivity(named: "Get from field value") {_ in
            return self.composeViewPage.fromField.label
        }
    }

    public func expandExtendedRecipientForm() throws {
        try XCTContext.runActivity(named: "Expanding extended recipient form") {_ in
            try self.composeViewPage.expandButton.tapCarefully()
        }
    }

    public func minimizeExtendedRecipientForm() throws {
        try XCTContext.runActivity(named: "Minimizing extended recipient form") {_ in
            try self.composeViewPage.expandButton.tapCarefully()
        }
    }

    public func isExtendedRecipientFormShown() throws -> Bool {
        XCTContext.runActivity(named: "Check is extended recipient form shown") {_ in
            return self.composeViewPage.ccField.exists
                && self.composeViewPage.bccField.exists
                && self.composeViewPage.fromField.exists
        }
    }

    public func isRecipientSuggestShown() throws -> Bool {
        XCTContext.runActivity(named: "Check is recipient suggest shown") {_ in
            return self.composeViewPage.suggestView.exists && self.composeViewPage.suggestView.isHittable
        }
    }

    public func getRecipientSuggest() throws -> YSArray<Contact> {
        XCTContext.runActivity(named: "Getting recipient suggest") {_ in
            let suggests: [Contact] = self.composeViewPage.suggests.map { Contact($0.userName.exists ? $0.userName.label : "", $0.email.label) }
            return YSArray(array: suggests)
        }
    }

    public func tapOnRecipientSuggestByEmail(_ email: String) throws {
        try XCTContext.runActivity(named: "Tap on recipient suggest with email \(email)") {_ in
            try self.composeViewPage.suggest(by: email).tap()
        }
    }

    public func tapOnRecipientSuggestByIndex(_ index: Int32) throws {
        try XCTContext.runActivity(named: "Tap on recipient suggest with \(index) index") {_ in
            try self.composeViewPage.suggest(by: Int(index)).tap()
        }
    }

    public func isSenderSuggestShown() throws -> Bool {
        XCTContext.runActivity(named: "Checking is Sender suggest shown") {_ in
            return self.composeViewPage.fromSuggestView.exists && self.composeViewPage.fromSuggestView.isHittable
        }
    }

    public func getSenderSuggest() throws -> YSArray<String> {
        XCTContext.runActivity(named: "Getting Sender suggest") {_ in
            return YSArray(array: self.composeViewPage.fromSuggestLabels)
        }
    }

    public func tapOnSenderSuggestByEmail(_ email: String) throws {
        try XCTContext.runActivity(named: "Tap on Sender suggest with \(email) email") {_ in
            try self.composeViewPage.fromSuggest(by: email).tap()
        }
    }

    public func tapOnSenderSuggestByIndex(_ index: Int32) throws {
        try XCTContext.runActivity(named: "Tap on Sender suggest with \(index) index") {_ in
            try self.composeViewPage.fromSuggest(by: Int(index)).tap()
        }
    }

    public func getSubject() throws -> String {
        XCTContext.runActivity(named: "Getting subject") {_ in
            return self.composeViewPage.subject.exists ? self.composeViewPage.subject.value as! String : self.composeViewPage.subjectSingleLineLabel.label
        }
    }

    public func setSubject(_ subject: String) throws {
        XCTContext.runActivity(named: "Set \(subject) to subject") {_ in
            self.composeViewPage.subject.clearField()
            self.composeViewPage.subject.typeText(subject)
        }
    }

    public func tapOnSubjectField() throws {
        try XCTContext.runActivity(named: "Tapping on subject field") {_ in
            try self.composeViewPage.subjectWrapper.tapCarefully()
        }
    }

    public func getBody() throws -> String {
        XCTContext.runActivity(named: "Getting body") {_ in
            return (self.composeViewPage.bodyTextField.value as! String).replacingOccurrences(of: "\u{00A0}", with: " ")
        }
    }

    public func setBody(_ body: String) throws {
        XCTContext.runActivity(named: "Set \(body) to body") {_ in
            self.composeViewPage.bodyTextField.typeText(body)
        }
    }

    public func pasteBody(_ body: String) throws {
        try XCTContext.runActivity(named: "Paste \(body) to body") {_ in
            try self.composeViewPage.bodyTextField.pasteText(body)
        }
    }

    public func clearBody() throws {
        try XCTContext.runActivity(named: "Clear body") {_ in
            try self.composeViewPage.bodyWebView.tapCarefully()
            try self.composeViewPage.bodyWebView.shouldExist().longTap()
            self.composeViewPage.bodyWebView.selectAll()
            self.composeViewPage.bodyWebView.deleteCharacters(count: 1)
        }
    }

    public func tapOnBodyField() throws {
        try XCTContext.runActivity(named: "Tapping on body field") {_ in
            try self.composeViewPage.bodyWebView.tapCarefully()
        }
    }
}

private extension ComposeRecipientFieldType {
    var toXCUIElement: XCUIElement {
        let composeViewPage = ComposeViewPage()
        switch self {
        case .to:
            return composeViewPage.toField
        case .cc:
            return composeViewPage.ccField
        case .bcc:
            return composeViewPage.bccField
        }
    }
}

private extension AppYabbleType {
    var toModelYabbleType: YabbleType {
        switch self {
        case .suggested:
            return .suggested
        case .manual:
            return .manual
        case .invalid:
            return .invalid
        case .new:
            return .new
        }
    }
}

private extension YabbleElement {
    var toModelYabble: Yabble {
        let yabbleType = self.yabbleType
        let isActive = self.isActive
        let emailOrName = self.emailOrName
        let email = (yabbleType == AppYabbleType.suggested) && isActive ? "" : emailOrName
        let name = (yabbleType == AppYabbleType.suggested) && isActive ? emailOrName : ""
        return Yabble(email, name, yabbleType.toModelYabbleType, isActive )
    }
}
