import UIUtils
import XCTest

/// Попап фильтров на КМ для товаров с параметарми.
class FilterPopupPage: PageObject, PopupPage {
    static let rootIdentifier = SKUCardAccessibility.filtersPopUp

    /// Описание товара в хэдере попапа фильтров
    var titleLabel: XCUIElement {
        element
            .descendants(matching: .staticText).matching(identifier: SKUCardAccessibility.descriptionInFilter)
            .firstMatch
    }

    /// Цена товара в хэдере попапа фильтров
    var priceLabel: XCUIElement {
        element
            .descendants(matching: .staticText).matching(identifier: SKUCardAccessibility.priceInFilter)
            .firstMatch
    }

    ///  Кнопка "Готово" в хедере попапа
    var doneButton: XCUIElement {
        XCUIApplication()
            .descendants(matching: .button).matching(identifier: SKUCardAccessibility.filtersPopUpDoneButton)
            .firstMatch
    }

    /// Класс для коллекции фильтров
    class FilterCollection: PageObject, UniformCollectionViewPage {
        typealias AccessibilityIdentifierProvider = FilterCollectionViewCellsAccessibility
        typealias CellPage = FilterCell

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        class FilterCell: PageObject {
            var title: XCUIElement {
                element.descendants(matching: .staticText).firstMatch
            }
        }
    }

    /// Коллекция фильтров
    var filterCollection: FilterCollection {
        let el = element.collectionViews.firstMatch
        return FilterCollection(element: el)
    }
}
