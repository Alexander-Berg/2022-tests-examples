import MarketUI
import XCTest

/// Сниппет товара в скроллбоксах
final class SnippetPage: PageObject, SKUEntryPoint {

    var image: XCUIElement {
        element.images.matching(identifier: SnippetsAccessibility.image).firstMatch
    }

    var priceLabel: XCUIElement {
        element.staticTexts.matching(identifier: SnippetsAccessibility.priceLabel).firstMatch
    }

    var oldPriceLabel: XCUIElement {
        element.staticTexts.matching(identifier: SnippetsAccessibility.oldPriceLabel).firstMatch
    }

    var discountBadge: DiscountBadgeViewPage {
        let page = element
            .otherElements.matching(identifier: DiscountBadgeViewAccessibility.root)
            .firstMatch
        return DiscountBadgeViewPage(element: page)
    }

    var titleLabel: XCUIElement {
        element.staticTexts.matching(identifier: SnippetsAccessibility.titleLabel).firstMatch
    }

    var subtitleLabel: XCUIElement {
        element.staticTexts.matching(identifier: SnippetsAccessibility.subtitleLabel).firstMatch
    }

    var ratingView: XCUIElement {
        element.images.matching(identifier: SnippetsAccessibility.ratingView).firstMatch
    }

    var reviewsCountLabel: XCUIElement {
        element.staticTexts.matching(identifier: SnippetsAccessibility.reviewsCountLabel).firstMatch
    }

    var addToCartButton: CartButtonPage {
        let elem = element.buttons.matching(identifier: SnippetsAccessibility.addToCartButton).firstMatch
        return CartButtonPage(element: elem)
    }

    var payButton: PayButton {
        let elem = element.buttons.matching(identifier: SnippetsAccessibility.payButton).firstMatch
        return .init(element: elem)
    }

    var reasonToBuy: XCUIElement {
        element.staticTexts.matching(identifier: SnippetsAccessibility.reasonToBuy).firstMatch
    }

    var soldOutView: XCUIElement {
        element.otherElements.matching(identifier: SnippetsAccessibility.soldOutView).firstMatch
    }

    var soldOutImage: XCUIElement {
        element.images.matching(identifier: SnippetsAccessibility.soldOutImage).firstMatch
    }

    var giftView: GiftViewPage {
        let page = element.otherElements
            .matching(identifier: GiftViewAccessibility.root)
            .firstMatch
        return GiftViewPage(element: page)
    }

    var cheapestAsGiftView: CheapestAsGiftViewPage {
        let page = element.otherElements
            .matching(identifier: CheapestAsGiftAccessibility.root)
            .firstMatch
        return CheapestAsGiftViewPage(element: page)
    }
}

// MARK: - Nested Types

extension SnippetPage {

    final class PayButton: PageObject {
        func tap() -> OrderEditPaymentPage {
            element.tap()

            let elem = XCUIApplication().otherElements[OrderEditPaymentAccessibility.root]
            return .init(element: elem)
        }
    }
}

struct SnippetInfo {

    let price: String
    let discountPercent: String?
    let oldPrice: String?
    let starsValue: String
    let reviewsCountLabel: String
    let skuName: String
    let isInCart: Bool
    let reasonsText: String?
    let isSoldOut: Bool
    let hasGift: Bool
    let hasCheapestAsGift: Bool

    static func makeEmpty() -> SnippetInfo {
        SnippetInfo(
            price: "",
            discountPercent: "",
            oldPrice: "",
            starsValue: "",
            reviewsCountLabel: "",
            skuName: "",
            isInCart: false,
            reasonsText: nil,
            isSoldOut: false,
            hasGift: false,
            hasCheapestAsGift: false
        )
    }
}
