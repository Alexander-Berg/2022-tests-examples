import XCTest

class CartButtonPage: PageObject, CartEntryPoint {

    /// Кнопка "+" - увеличение товара в корзине
    var plusButton: XCUIElement {
        element.buttons.matching(identifier: CartButtonAccessibility.plusButton).firstMatch
    }

    /// Кнопка "-" - уменьшение товара в корзине
    var minusButton: XCUIElement {
        element.buttons.matching(identifier: CartButtonAccessibility.minusButton).firstMatch
    }
}
