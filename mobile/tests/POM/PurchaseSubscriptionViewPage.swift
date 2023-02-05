import XCTest

class PurchaseSubscriptionViewPage: PageObject {

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: PurchaseSubscriptionViewAccessibility.root).firstMatch
        return .init(element: element)
    }

    var purchaseSubscriptionButton: SKUPage.CheckoutButton {
        .init(
            element: element
                .buttons
                .matching(identifier: PurchaseSubscriptionViewAccessibility.purchaseSubscriptionButton)
                .firstMatch
        )
    }

    var price: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PurchaseSubscriptionViewAccessibility.price)
            .firstMatch
    }
}
