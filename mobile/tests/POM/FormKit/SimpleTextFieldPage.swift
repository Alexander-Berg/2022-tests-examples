import FormKit
import MarketUI
import XCTest

class SimpleTextFieldPage: PageObject {

    var textField: XCUIElement {
        element.textFields
            .matching(identifier: SimpleTextFieldAccessibility.textField).element
    }

    var clearButton: XCUIElement {
        element.buttons
            .matching(identifier: SimpleTextFieldAccessibility.clearButton).element
    }

    func tapClearIfNeeded() {
        if !textField.text.isEmpty {
            clearButton.tap()
        }
    }

}
