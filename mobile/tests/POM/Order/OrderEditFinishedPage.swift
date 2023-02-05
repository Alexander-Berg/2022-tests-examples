import UIUtils
import XCTest

final class OrderEditFinishedPage: PageObject {

    // MARK: - Public

    static var current: OrderEditFinishedPage {
        let item = XCUIApplication().otherElements[OrderEditFinishedAccessibility.root]
        return OrderEditFinishedPage(element: item)
    }

    var image: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderEditFinishedAccessibility.image)
        return el.images.firstMatch
    }

    var titleCell: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditFinishedAccessibility.title)
            .textViews
            .firstMatch
    }

    var subtitleCell: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditFinishedAccessibility.subtitle)
            .textViews
            .firstMatch
    }

    var nextButton: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditFinishedAccessibility.nextButtonCell)
            .buttons
            .firstMatch
    }

    var items: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditFinishedAccessibility.items)
            .collectionViews
            .firstMatch
    }
}

// MARK: - CollectionViewPage

extension OrderEditFinishedPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OrderEditFinishedCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
