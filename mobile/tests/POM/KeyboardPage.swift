import FormKit
import XCTest

class KeyboardPage: PageObject {
    /// Current element
    static var current: KeyboardPage {
        let elem = XCUIApplication().keyboards.firstMatch
        return KeyboardPage(element: elem)
    }

    func tapDone() {
        XCUIApplication().buttons
            .matching(identifier: KeyboardAccessoryAccessibility.doneButton).element.tap()
    }
}
