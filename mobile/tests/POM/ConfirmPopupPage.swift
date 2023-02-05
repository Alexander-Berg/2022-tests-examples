import UIUtils
import XCTest

/// PopUp для контекстного меню
class ConfirmPopupPage: PageObject, PopupPage {

    static let rootIdentifier = ConfirmPopupAccessibility.root

    var leftButton: XCUIElement {
        cellUniqueElement(withIdentifier: ConfirmPopupAccessibility.leftButton)
    }

    var rightButton: XCUIElement {
        cellUniqueElement(withIdentifier: ConfirmPopupAccessibility.rightButton)
    }

}

extension ConfirmPopupPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = ConfirmPopupCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
