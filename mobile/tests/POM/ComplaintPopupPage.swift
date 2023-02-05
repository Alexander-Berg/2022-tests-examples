import UIUtils
import XCTest

/// PopUp для жалоб
class ComplaintPopupPage: PageObject, PopupPage {

    static let rootIdentifier = ComplaintPopupAccessibility.root

    var complainButton: XCUIElement {
        cellUniqueElement(withIdentifier: ComplaintPopupAccessibility.complainButton)
            .buttons
            .firstMatch
    }

    var spam: XCUIElement {
        cellUniqueElement(withIdentifier: ComplaintPopupAccessibility.spam)
    }

    var offensiveContent: XCUIElement {
        cellUniqueElement(withIdentifier: ComplaintPopupAccessibility.offensiveContent)
    }

    var other: XCUIElement {
        cellUniqueElement(withIdentifier: ComplaintPopupAccessibility.other)
    }

}

extension ComplaintPopupPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = ComplaintPopupCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
