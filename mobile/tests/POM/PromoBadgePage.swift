import XCTest

class PromoBadgePage: PageObject {

    var root: XCUIElement {
        element
            .otherElements
            .matching(identifier: PromoBadgeViewAccessibility.root)
            .firstMatch
    }

    var promocode: XCUIElement {
        element
            .images
            .matching(identifier: PromoBadgeViewAccessibility.promocode)
            .firstMatch
    }
}
