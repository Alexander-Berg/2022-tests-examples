import FormKit
import XCTest

class KeyboardAccessoryPage: PageObject {
    var nextButton: XCUIElement {
        element
            .buttons.matching(identifier: KeyboardAccessoryAccessibility.nextButton)
            .firstMatch
    }

    var prevButton: XCUIElement {
        element
            .buttons.matching(identifier: KeyboardAccessoryAccessibility.prevButton)
            .firstMatch
    }

    var doneButton: XCUIElement {
        element
            .buttons.matching(identifier: KeyboardAccessoryAccessibility.doneButton)
            .firstMatch
    }

    /// Current element
    static var current: KeyboardAccessoryPage {
        let elem = XCUIApplication().otherElements[KeyboardAccessoryAccessibility.root]
        return KeyboardAccessoryPage(element: elem)
    }
}
