import FormKit
import MarketUI
import XCTest

class FieldViewPage: PageObject {

    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FieldViewAccessibility.title)
            .firstMatch
    }

    var value: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FieldViewAccessibility.value)
            .firstMatch
    }

    var button: XCUIElement {
        element
            .buttons
            .matching(identifier: FieldViewAccessibility.button)
            .firstMatch
    }

}
