import XCTest

class PricePage: PageObject {
    var price: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceAccessibility.price)
            .firstMatch
    }

    var oldPrice: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceAccessibility.oldPrice)
            .firstMatch
    }

    var discountBadge: DiscountBadgeViewPage {
        let elem = element
            .otherElements
            .matching(identifier: DiscountBadgeViewAccessibility.root)
            .firstMatch
        return DiscountBadgeViewPage(element: elem)
    }

    var bnplLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceAccessibility.bnplLabel)
            .firstMatch
    }

    var unitPrice: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceAccessibility.unitPrice)
            .firstMatch
    }
}
