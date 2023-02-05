import XCTest

final class LavkaSurgePage: PageObject {

    // MARK: - Properties

    static var current: LavkaSurgePage {
        let item = XCUIApplication().otherElements[LavkaSurgeAccessibility.root]
        return LavkaSurgePage(element: item)
    }

    var surgeInfoLabel: XCUIElement {
        element.staticTexts.matching(identifier: LavkaSurgeAccessibility.surgeInfoLabel).firstMatch
    }

}
