import MarketCashback
import XCTest

final class GrowingCashbackPromoPage: PageObject {

    static var current: GrowingCashbackPromoPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: GrowingCashbackAccessibility.Promo.root)
            .firstMatch
        return GrowingCashbackPromoPage(element: elem)
    }

    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: GrowingCashbackAccessibility.Promo.title)
            .firstMatch
    }

    var inAppLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: GrowingCashbackAccessibility.Promo.inAppLabel)
            .firstMatch
    }

    var date: XCUIElement {
        element
            .staticTexts
            .matching(identifier: GrowingCashbackAccessibility.Promo.date)
            .firstMatch
    }

    var subtitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: GrowingCashbackAccessibility.Promo.subtitle)
            .firstMatch
    }

    var purchaseButton: XCUIElement {
        element
            .buttons
            .matching(identifier: GrowingCashbackAccessibility.Promo.purchaseButton)
            .firstMatch
    }

    var detailsButton: XCUIElement {
        element
            .buttons
            .matching(identifier: GrowingCashbackAccessibility.Promo.detailsButton)
            .firstMatch
    }

    var closeButton: XCUIElement {
        element
            .buttons
            .matching(identifier: GrowingCashbackAccessibility.Promo.closeButton)
            .firstMatch
    }

    var promoOrders: [PromoOrder] {
        element
            .otherElements
            .matching(identifier: GrowingCashbackAccessibility.Promo.Order.root)
            .allElementsBoundByIndex
            .map { PromoOrder(element: $0) }
    }
}

// MARK: - Nested types

extension GrowingCashbackPromoPage {

    final class PromoOrder: PageObject {

        var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: GrowingCashbackAccessibility.Promo.Order.title)
                .firstMatch
        }

        var subtitle: XCUIElement {
            element
                .staticTexts
                .matching(identifier: GrowingCashbackAccessibility.Promo.Order.subtitle)
                .firstMatch
        }

        var cashback: XCUIElement {
            element
                .staticTexts
                .matching(identifier: GrowingCashbackAccessibility.Promo.Order.cashback)
                .firstMatch
        }

        var checkmark: XCUIElement {
            element
                .images
                .matching(identifier: GrowingCashbackAccessibility.Promo.Order.checkmark)
                .firstMatch
        }
    }
}

// MARK: - GrowingCashbackPromoPageEntryPoint

protocol GrowingCashbackPromoPageEntryPoint: PageObject {

    func tap() -> GrowingCashbackPromoPage
}

extension GrowingCashbackPromoPageEntryPoint {

    func tap() -> GrowingCashbackPromoPage {
        element.tap()
        return GrowingCashbackPromoPage.current
    }
}
