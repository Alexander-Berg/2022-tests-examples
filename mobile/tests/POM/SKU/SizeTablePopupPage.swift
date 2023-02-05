import UIUtils
import XCTest

/// Попап размерных сеток
class SizeTablePopupPage: PageObject, PopupPage {
    static let rootIdentifier = SKUCardAccessibility.sizeTablePopup

    /// Кнопка "Готово"
    var doneButton: XCUIElement {
        element.buttons
            .matching(identifier: SKUCardAccessibility.sizeTablePopupDoneButton)
            .firstMatch
    }

    /// Класс для коллекции размеров
    class SizeTableCollection: PageObject, UniformCollectionViewPage {
        typealias AccessibilityIdentifierProvider = SizeTableCollectionViewCellsAccessibility
        typealias CellPage = SizeCell

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        class SizeCell: PageObject {
            var title: XCUIElement {
                element.descendants(matching: .staticText).firstMatch
            }
        }
    }

    /// Коллекция размеров
    var sizeTableCollection: SizeTableCollection {
        let el = element.collectionViews.firstMatch
        return SizeTableCollection(element: el)
    }
}
