//
//  MessageApp.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 22/11/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

/// The behavior depends on iOS version, so this class is not recommended to use.
/// See `DeeplinkSteps` and `CommunicationAgent` for more details.
final class MessageApp {
    static func launch() -> XCUIApplication {
        let localizedIdentifiers = Self.localizedIdentifiers()

        // Open iMessage App
        let messageApp = XCUIApplication(bundleIdentifier: "com.apple.MobileSMS")

        // Launch iMessage app
        messageApp.launch()

        // Wait some seconds for launch
        messageApp.yreEnsureExistsWithTimeout(timeout: 10, message: "Message app doesn't exists")

        // Continues Apple Pay if present
        let continueButton = messageApp.buttons[localizedIdentifiers.continueButton]
        if continueButton.yreWaitForExistence() {
            continueButton.tap()
        }

        // Removes New Messages Sheet on iOS 13
        let cancelButton = messageApp.navigationBars.buttons[localizedIdentifiers.cancelButton]
        if cancelButton.yreWaitForExistence() {
            cancelButton.tap()
        }

        // Return application handle
        return messageApp
    }

    static func open(URLString urlString: String,
                     inMessageApp app: XCUIApplication,
                     shouldWaitInBackground: Bool) {
        let localizedIdentifiers = Self.localizedIdentifiers()

        XCTContext.runActivity(named: "Open URL \(urlString) in iMessage") { _ in
            // Find Simulator Message
            let kateBell = app.cells.staticTexts["Kate Bell"]
            kateBell.yreEnsureExistsWithTimeout(timeout: 10, message: "Cell with title Kate Bell doesn't exists")
            kateBell.tap()

            // Tap message field
            app.textFields[localizedIdentifiers.iMessageTextField].tap()

            // Enter the URL string
            app.typeText("Open Link:\n")
            app.typeText(urlString)

            // Simulate sending link
            app.buttons[localizedIdentifiers.sendButton].tap()

            // Wait message completely send
            // The first link on the page
            let messageBubble = app.cells.links["com.apple.messages.URLBalloonProvider"]
            messageBubble.yreEnsureExistsWithTimeout(timeout: 10, message: "MessageBubble doesn't exist")

            // Message label will be changed after full loading deeplink
            // Messsge ballon isn't tapable during switching this state
            // Wait full loading then tap on message
            // Result doesn't matter. Just wait changing label and then try to tap it anyway
            let currentMessageLabel = messageBubble.label
            _ = messageBubble.yreWait(\.label, .notEqualTo, currentMessageLabel, timeout: 10)
            messageBubble.tap()
            // Wait until we move to app. If it doesn't happen, probably we failed on deeplink tap
            // Tap again
            if shouldWaitInBackground, XCUIApplication().wait(for: .runningForeground, timeout: 10) == false {
                messageBubble.tap()
            }
        }
    }

    static func openFromPasteboard(inMessageApp app: XCUIApplication) {
        let localizedIdentifiers = Self.localizedIdentifiers()

        XCTContext.runActivity(named: "Open URL from Pasetboard in iMessage") { _ in
            // Find Simulator Message
            let kateBell = app.cells.staticTexts["Kate Bell"]
            kateBell.yreEnsureExistsWithTimeout(timeout: 10, message: "Cell with title Kate Bell doesn't exists")
            kateBell.tap()

            // Add dummy text to workaround issue with link-only bubbles in iOS 12.4+
            app.textFields[localizedIdentifiers.iMessageTextField].tap()
            app.typeText("Open Link:\n")

            // Tap message field to open paste menu
            app.textFields[localizedIdentifiers.iMessageTextField].tap()

            sleep(1)
            app.menuItems[localizedIdentifiers.pasteMenuItem].tap()

            // Simulate sending link
            sleep(1)
            app.buttons[localizedIdentifiers.sendButton].tap()

            // Wait for Main App to finish launching
            sleep(3)

            // The first link on the page
            let messageBubble = app.cells.links["com.apple.messages.URLBalloonProvider"]
            messageBubble
                .yreEnsureExistsWithTimeout(timeout: 10, message: "MessageBubble doesn't exist")
                .tap()
        }
    }

    // MARK: Private

    private struct LocalizedIdentifiers {
        let continueButton: String
        let cancelButton: String

        let iMessageTextField: String
        let sendButton: String

        let pasteMenuItem: String

        static let ruVersion: Self = {
            return .init(
                continueButton: "Продолжить",
                cancelButton: "Отменить",
                iMessageTextField: "iMessage",
                sendButton: "Отправить",
                pasteMenuItem: "Вставить"
            )
        }()

        static let enVersion: Self = {
            return .init(
                continueButton: "Continue",
                cancelButton: "Cancel",
                iMessageTextField: "iMessage",
                sendButton: "sendButton",
                pasteMenuItem: "Paste"
            )
        }()
    }

    private static func localizedIdentifiers() -> LocalizedIdentifiers {
        guard let locale = NSLocale.current.languageCode else {
            return LocalizedIdentifiers.enVersion
        }
        switch locale {
            case "en":
                return LocalizedIdentifiers.enVersion
            case "ru":
                return LocalizedIdentifiers.ruVersion
            default:
                XCTFail("Unsupported locale \(locale)")
                return LocalizedIdentifiers.enVersion
        }
    }
}
