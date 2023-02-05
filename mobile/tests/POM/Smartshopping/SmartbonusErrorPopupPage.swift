import UIUtils
import XCTest

final class SmartbonusErrorPopupPage: PageObject {

    // MARK: - Properties

    var image: XCUIElement {
        cellUniqueElement(withIdentifier: SmartbonusErrorPopupAccessibility.image)
            .images
            .firstMatch
    }

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: SmartbonusErrorPopupAccessibility.title)
            .textViews
            .firstMatch
    }

    var description: XCUIElement {
        cellUniqueElement(withIdentifier: SmartbonusErrorPopupAccessibility.description)
            .textViews
            .firstMatch
    }

    var topButton: XCUIElement {
        cellUniqueElement(withIdentifier: SmartbonusErrorPopupAccessibility.topButton)
            .buttons
            .firstMatch
    }

    var bottomButton: XCUIElement {
        cellUniqueElement(withIdentifier: SmartbonusErrorPopupAccessibility.bottomButton)
            .buttons
            .firstMatch
    }

}

// MARK: - CollectionViewPage

extension SmartbonusErrorPopupPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = SmartbonusErrorPopupCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - PopupPage

extension SmartbonusErrorPopupPage: PopupPage {

    static let rootIdentifier = SmartbonusErrorPopupAccessibility.root

}
