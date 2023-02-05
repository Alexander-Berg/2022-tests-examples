import XCTest

final class CancelOrderPage: PageObject {
    var confirmButton: XCUIElement {
        element
            .buttons
            .matching(identifier: CancelOrderAccessibility.confirmButton)
            .firstMatch
    }

    var finishButton: XCUIElement {
        element
            .buttons
            .matching(identifier: CancelOrderAccessibility.reasonFinishButton)
            .firstMatch
    }
}
