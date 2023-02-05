import XCTest

class PageObject {

    let element: XCUIElement

    required init(element: XCUIElement) {
        self.element = element
    }
}
