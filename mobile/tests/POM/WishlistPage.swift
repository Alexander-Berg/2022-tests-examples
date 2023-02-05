import MarketUI
import UIUtils
import XCTest

class WishlistPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = WishlistCollectionCellAccessibility

    // MARK: - Properties

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var notificationCell: PlusNotificationCellPage {
        let elem = cellUniqueElement(withIdentifier: WishlistAccessibility.plusNotification)
        return PlusNotificationCellPage(element: elem)
    }

    final class PlusNotificationCellPage: PageObject {
        func tap() -> HomePlusPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: PlusAccessibility.homeRoot)
                .firstMatch

            return HomePlusPage(element: elem)
        }
    }

    func wishlistItem(at position: Int) -> FeedSnippetPage {
        let element = allCellUniqueElement(withIdentifier: WishlistAccessibility.snippetCell)[position]
            .otherElements
            .firstMatch

        return FeedSnippetPage(element: element)
    }

}
