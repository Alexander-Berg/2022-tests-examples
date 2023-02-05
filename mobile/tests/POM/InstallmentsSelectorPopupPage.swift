import FormKit
import MarketCheckoutFeature
import MarketInstallments
import MarketUI
import UIUtils
import XCTest

/// Попап селектора срока рассрочки
final class InstallmentsSelectorPopupPage: PageObject, PopupPage {

    static var rootIdentifier: String = InstallmentsSelectorPopupAccessibility.root

    /// Коллекция сроков рассрочки
    var collectionView: CollectionView {
        let elem = element
            .collectionViews
            .matching(identifier: InstallmentsSelectorPopupAccessibility.collectionView)
            .firstMatch
        return CollectionView(element: elem)
    }

    /// Кнопка Выбрать
    var selectButton: XCUIElement {
        element
            .buttons
            .matching(identifier: PopupButtonAccessibility.button)
            .firstMatch
    }
}

// MARK: - Nested Types

extension InstallmentsSelectorPopupPage {

    final class CollectionView: PageObject, UniformCollectionViewPage {

        typealias AccessibilityIdentifierProvider = InstallmentsSelectorPopupCellsAccessibility
        typealias CellPage = InstallmentsSelectorPopupTileCellPage

        var collectionView: XCUIElement {
            element
        }

        /// Ячейка с выбранным сроком рассрочки
        var selectedCell: CellPage {
            let elem = element
                .otherElements
                .matching(identifier: TiledSelectorAccessibility.Selectability.selected)
                .firstMatch
            return CellPage(element: elem)
        }
    }
}
