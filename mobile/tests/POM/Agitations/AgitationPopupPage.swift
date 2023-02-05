import UIUtils
import XCTest

final class AgitationPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static var rootIdentifier: String = AgitationsAccessibility.popUp

    var firstButton: XCUIElement {
        cellUniqueElement(withIdentifier: AgitationsAccessibility.firstButtonCell).buttons.firstMatch
    }

    var lastButton: XCUIElement {
        cellUniqueElement(withIdentifier: AgitationsAccessibility.lastButtonCell).buttons.firstMatch
    }

    var descriptionLabel: XCUIElement {
        cellUniqueElement(withIdentifier: AgitationsAccessibility.descriptionLabelCell).staticTexts.firstMatch
    }

    var itemsCollectionView: XCUIElement {
        cellUniqueElement(withIdentifier: AgitationsAccessibility.itemsCell).collectionViews.firstMatch
    }

    var orderButton: DetailButton {
        let el = cellUniqueElement(withIdentifier: AgitationsAccessibility.firstButtonCell).buttons.firstMatch
        return DetailButton(element: el)
    }

    var orderEditButton: OrderEditButton {
        let el = cellUniqueElement(withIdentifier: AgitationsAccessibility.lastButtonCell).buttons.firstMatch
        return OrderEditButton(element: el)
    }
}

// MARK: - Nested Types

extension AgitationPopupPage {
    final class DetailButton: PageObject, OrderDetailsEntryPoint {}
    final class OrderEditButton: PageObject, OrderEditEntryPoint {}
}

// MARK: - CollectionViewPage

extension AgitationPopupPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = AgitationsCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
