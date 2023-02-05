import UIUtils
import XCTest

final class MissingOrderItemsPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static var rootIdentifier: String = MissingOrderItemsAccessibility.popUp

    var choiceButton: XCUIElement {
        cellUniqueElement(withIdentifier: MissingOrderItemsAccessibility.choiceButtonCell).buttons.firstMatch
    }

    var orderButton: DetailButton {
        let el = cellUniqueElement(withIdentifier: MissingOrderItemsAccessibility.orderButtonCell).buttons.firstMatch
        return DetailButton(element: el)
    }

    var textLabel: XCUIElement {
        cellUniqueElement(withIdentifier: MissingOrderItemsAccessibility.textLabelCell).staticTexts.firstMatch
    }

    var itemsCollectionView: XCUIElement {
        cellUniqueElement(withIdentifier: MissingOrderItemsAccessibility.itemsCell).collectionViews.firstMatch
    }
}

// MARK: - Nested Types

extension MissingOrderItemsPopupPage {
    final class DetailButton: PageObject, OrderDetailsEntryPoint {}
}

// MARK: - CollectionViewPage

extension MissingOrderItemsPopupPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = MissingOrderItemsCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
