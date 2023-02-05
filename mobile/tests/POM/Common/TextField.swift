import XCTest

final class TextField: PageObject {

    var placeholder: XCUIElement {
        element.staticTexts.firstMatch
    }

    var field: XCUIElement {
        element.textFields.firstMatch
    }

    var clearButton: XCUIElement {
        element.buttons.firstMatch
    }

    var validationMark: XCUIElement {
        element.images.firstMatch
    }

}
