import XCTest

final class CheckoutShipmentPopupItemPage: PageObject {

    var title: XCUIElement {
        element.staticTexts.matching(identifier: CheckoutShipmentPopupViewControllerAccessibility.Item.titleLabel)
            .firstMatch
    }

    var price: XCUIElement {
        element.staticTexts.matching(identifier: CheckoutShipmentPopupViewControllerAccessibility.Item.priceLabel)
            .firstMatch
    }

    var count: XCUIElement {
        element.staticTexts.matching(identifier: CheckoutShipmentPopupViewControllerAccessibility.Item.countLabel)
            .firstMatch
    }
}
