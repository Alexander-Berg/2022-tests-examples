import XCTest

class CompactOfferViewPage: PageObject {

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: CompactOfferViewAccessibility.root).firstMatch
        return .init(element: element)
    }

    var cartButton: CartButtonPage {
        let elem = element.buttons.matching(identifier: CompactOfferViewAccessibility.cartButton).firstMatch
        return CartButtonPage(element: elem)
    }

    var creditInfo: XCUIElement {
        element
            .staticTexts.matching(identifier: CompactOfferViewAccessibility.creditInfo).firstMatch
    }

    /// Лейбл ценника
    var priceLabel: XCUIElement {
        element.staticTexts.matching(identifier: CompactOfferViewAccessibility.priceLabel).firstMatch
    }
}
