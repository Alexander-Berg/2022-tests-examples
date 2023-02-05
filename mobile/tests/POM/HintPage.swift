import XCTest

final class HintPage: PageObject {

    static var current: HintPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: HintAccessibility.root)
            .firstMatch
        return HintPage(element: elem)
    }

    var title: XCUIElement {
        element
            .staticTexts
            .firstMatch
    }
}
