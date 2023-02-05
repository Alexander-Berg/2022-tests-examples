import UIUtils
import XCTest

/// PopUp для контекстного меню
class ContextMenuPopupPage: PageObject, PopupPage {

    static let rootIdentifier = ContextMenuPopupAccessibility.root

    var complain: XCUIElement {
        collectionView.cells.firstMatch
    }

    var delete: XCUIElement {
        cellUniqueElement(withIdentifier: ContextMenuPopupAccessibility.deleteItem)
    }

    var edit: XCUIElement {
        cellUniqueElement(withIdentifier: ContextMenuPopupAccessibility.editItem)
    }

}

extension ContextMenuPopupPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = ContextMenuPopupCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
