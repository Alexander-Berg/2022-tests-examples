import XCTest

class HyperlocalMapPage: PageObject {

    // MARK: - Properties

    static var current: HyperlocalMapPage {
        let element = XCUIApplication().otherElements[HyperlocalMapAccessibility.root]
        return HyperlocalMapPage(element: element)
    }
}
