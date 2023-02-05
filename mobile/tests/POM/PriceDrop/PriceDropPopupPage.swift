import UIUtils
import XCTest

class PriceDropPopupPage: PageObject {

    static var currentPopup: Self {
        let element = XCUIApplication().any.matching(identifier: PriceDropPopupAccessibility.collectionView).firstMatch
        return .init(element: element)
    }

    /// Коллекция Поп-ап прайсдропа
    var collectionView: CollectionView {
        CollectionView(element: element)
    }

    var header: PriceDropPopupHeaderPage {
        let el = element.otherElements.matching(identifier: PriceDropPopupAccessibility.Header.root).firstMatch
        return PriceDropPopupHeaderPage(element: el)
    }

    /// Промокод
    var promocode: XCUIElement {
        element.staticTexts.matching(identifier: PriceDropPopupAccessibility.Header.promocode).firstMatch
    }

    /// Кнопка "Каунтер"
    var counterButton: CartButtonPage {
        let elem = element.buttons.matching(identifier: CartPopupAccessibility.counterButton).firstMatch
        return CartButtonPage(element: elem)
    }

    class CollectionView: PageObject, UniformCollectionViewPage {

        typealias AccessibilityIdentifierProvider = PriceDropPopupSnippetCellsAccessibility
        typealias CellPage = PriceDropPopupSnippetPage

        var collectionView: XCUIElement {
            element
        }
    }
}
