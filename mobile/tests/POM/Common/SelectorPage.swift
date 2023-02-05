import FormKit
import MarketUI
import XCTest

final class SelectorPage: PageObject {

    // MARK: - Public

    var placeholder: XCUIElement {
        element
            .staticTexts
            .matching(identifier: SelectorAccessibility.placeholder)
            .firstMatch
    }

    var value: XCUIElement {
        element
            .staticTexts
            .matching(identifier: SelectorAccessibility.value)
            .firstMatch
    }

    var border: XCUIElement {
        element
            .otherElements
            .matching(identifier: SelectorAccessibility.border)
            .firstMatch
    }

}
