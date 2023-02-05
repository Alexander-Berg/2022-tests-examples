import FormKit
import MarketUI
import UIUtils
import XCTest

class CartKgtPopupPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CartKgtTextCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var titleLabel: XCUIElement {
        XCUIApplication()
            .staticTexts.matching(identifier: PopupEmdeddingAccessibility.navigationHeaderTitle)
            .firstMatch
    }

    var okButton: XCUIElement {
        collectionView.buttons
            .matching(identifier: CartKgtPopupAccessibility.okButton)
            .firstMatch
    }

    var bodyText: XCUIElement {
        collectionView.textViews
            .matching(identifier: CartKgtPopupAccessibility.bodyText)
            .firstMatch
    }
}
