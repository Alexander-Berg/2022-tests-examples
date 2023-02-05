import FormKit
import MarketUI
import XCTest

class TextFieldPage: PageObject {

    var textField: XCUIElement {
        element.textFields
            .matching(identifier: TextFieldAccessibility.textField).element
    }

    var clearButton: XCUIElement {
        element.buttons
            .matching(identifier: TextFieldAccessibility.clearButton).element
    }

    func tapClearIfNeeded() {
        if !textField.text.isEmpty && clearButton.isHittable {
            clearButton.tap()
        }
    }

    func typeText(_ text: String) {
        textField.tap()
        textField.typeText(text)
    }

    func setText(_ text: String) {
        textField.tap()
        tapClearIfNeeded()
        textField.typeText(text)
    }
}
