import MarketRecommendationsFeature
import XCTest

final class ProductInCartPage: PageObject {

    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProductInCartAccessibility.title)
            .firstMatch
    }

    var name: XCUIElement {
        element
            .buttons
            .matching(identifier: ProductInCartAccessibility.name)
            .firstMatch
    }

    var supplier: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProductInCartAccessibility.supplier)
            .firstMatch
    }

    var image: XCUIElement {
        element
            .images
            .matching(identifier: ProductInCartAccessibility.image)
            .firstMatch
    }

    var price: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProductInCartAccessibility.price)
            .firstMatch
    }

    var oldPrice: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProductInCartAccessibility.oldPrice)
            .firstMatch
    }

    var cashback: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProductInCartAccessibility.cashback)
            .firstMatch
    }

    var promocode: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProductInCartAccessibility.promocode)
            .firstMatch
    }

    var cartButton: CartButtonPage {
        CartButtonPage(
            element: element
                .buttons
                .matching(identifier: ProductInCartAccessibility.cartButton)
                .firstMatch
        )
    }

    var goToCartButton: XCUIElement {
        element
            .buttons
            .matching(identifier: ProductInCartAccessibility.goToCartButton)
            .firstMatch
    }

    var resale: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProductInCartAccessibility.resale)
            .firstMatch
    }
}
