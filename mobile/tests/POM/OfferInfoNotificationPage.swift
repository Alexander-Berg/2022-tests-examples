import XCTest

final class OfferInfoNotificationPage: PageObject {

    static var current: OfferInfoNotificationPage {
        let item = XCUIApplication().otherElements[OfferInfoNotificationAccessibility.root]
        return OfferInfoNotificationPage(element: item)
    }

    var imageView: XCUIElement {
        element.images
            .matching(identifier: OfferInfoNotificationAccessibility.imageView)
            .firstMatch
    }

    var priceLabel: XCUIElement {
        element.staticTexts
            .matching(identifier: OfferInfoNotificationAccessibility.priceLabel)
            .firstMatch
    }

    var oldPriceLabel: XCUIElement {
        element.staticTexts
            .matching(identifier: OfferInfoNotificationAccessibility.oldPriceLabel)
            .firstMatch
    }

    var titleLabel: XCUIElement {
        element.staticTexts
            .matching(identifier: OfferInfoNotificationAccessibility.titleLabel)
            .firstMatch
    }

    var closeButton: XCUIElement {
        element.buttons
            .matching(identifier: OfferInfoNotificationAccessibility.closeButton)
            .firstMatch
    }
}
