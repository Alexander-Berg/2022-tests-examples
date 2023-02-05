import XCTest

final class PriceDropPopupSnippetPage: PageObject, SKUEntryPoint {

    // Скелет загрузки сниппета
    var skeleton: XCUIElement {
        element
            .otherElements
            .matching(identifier: PriceDropPopupAccessibility.Snippet.skeleton)
            .firstMatch
    }

    // Картинка товара
    var image: XCUIElement {
        element
            .images
            .matching(identifier: PriceDropPopupAccessibility.Snippet.image)
            .element(boundBy: 0)
    }

    // Старая цена
    var oldPrice: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceDropPopupAccessibility.Snippet.oldPrice)
            .firstMatch
    }

    // Новая цена
    var newPrice: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceDropPopupAccessibility.Snippet.newPrice)
            .firstMatch
    }

    // Название товара
    var name: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceDropPopupAccessibility.Snippet.name)
            .firstMatch
    }

    class CartButton: PageObject {
        func tap() -> CartPage {
            element.tap()
            let subElem = XCUIApplication().any[CartAccessibility.root]
            return CartPage(element: subElem)
        }
    }

    // Кнопка "В корзину"
    var cartButton: CartButton {
        let elem = element
            .buttons
            .matching(identifier: FeedSnippetAccessibility.addToCart)
            .firstMatch
        return CartButton(element: elem)
    }

    var saleBadge: DiscountBadgeViewPage {
        let page = element.otherElements
            .matching(identifier: DiscountBadgeViewAccessibility.root)
            .element(boundBy: 0)
        return DiscountBadgeViewPage(element: page)
    }

    // TODO: Фикс теста тапом вне области фото, убрать после фикса BLUEMARKETAPPS-36303
    @discardableResult
    func tap() -> SKUPage {
        element.staticTexts.firstMatch.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let skuElem = XCUIApplication().otherElements[SKUAccessibility.root]
        XCTAssertTrue(skuElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return SKUPage(element: skuElem)
    }
}
