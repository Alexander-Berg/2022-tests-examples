import Foundation
import XCTest
import Snapshots

final class iMessage {
    static func terminate() {
        let messageApp = XCUIApplication(bundleIdentifier: "com.apple.MobileSMS")
        messageApp.terminate()
    }
    static func launch() -> XCUIApplication {
        // Open iMessage App
        let messageApp = XCUIApplication(bundleIdentifier: "com.apple.MobileSMS")

        // Launch iMessage app
        messageApp.launch()
        messageApp.activate()

        // Wait some seconds for launch
        XCTAssertTrue(messageApp.waitForExistence(timeout: 10))

        // Continues Apple Pay if present
        let continueButton = messageApp.buttons["Continue"]
        if (continueButton.waitForExistence(timeout: 1)) {
            continueButton.tap()
        }

        // Removes New Messages Sheet on iOS 13
        let cancelButton = messageApp.navigationBars.buttons["Cancel"]
        if cancelButton.waitForExistence(timeout: 1) {
            cancelButton.tap()
        }

        // Return application handle
        return messageApp
    }

    static func open(URLString urlString: String, inMessageApp app: XCUIApplication) {
        XCTContext.runActivity(named: "Open URL \(urlString) in iMessage") { _ in
            // Find Simulator Message
            let kateBell = app.cells.staticTexts["Kate Bell"]
            XCTAssertTrue(kateBell.waitForExistence(timeout: 10))
            kateBell.tap()

            // Tap message field
            app.textFields["iMessage"].tap()

            // Enter the URL string
            app.typeText("Open Link:\n")
            app.typeText(urlString)

            // Simulate sending link
            app.buttons["sendButton"].tap()

            // Wait for message to load
            sleep(10)

            // The first link on the page
            let messageBubble = app.cells.links["com.apple.messages.URLBalloonProvider"]
            XCTAssertTrue(messageBubble.waitForExistence(timeout: 10))
            messageBubble.tap()
            sleep(3)
        }
    }

    static func openFromPasteboard(inMessageApp app: XCUIApplication) {
        XCTContext.runActivity(named: "Open URL from Pasetboard in iMessage") { _ in
            // Find Simulator Message
            let kateBell = app.cells.staticTexts["Kate Bell"]
            XCTAssertTrue(kateBell.waitForExistence(timeout: 10))
            kateBell.tap()

            // Add dummy text to workaround issue with link-only bubbles in iOS 12.4+
            app.textFields["iMessage"].tap()
            app.typeText("Open Link:\n")

            // Tap message field to open paste menu
            app.textFields["iMessage"].tap()

            sleep(1)
            app.menuItems["Paste"].tap()

            // Simulate sending link
            sleep(1)
            app.buttons["sendButton"].tap()

            // Wait for Main App to finish launching
            sleep(3)

            // The first link on the page
            let messageBubble = app.cells.links["com.apple.messages.URLBalloonProvider"]
            XCTAssertTrue(messageBubble.waitForExistence(timeout: 10))
            messageBubble.tap()
        }
    }
}
