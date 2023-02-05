import UIUtils
import XCTest

final class RecentProductsPage: PageObject {}

extension RecentProductsPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = RecentProductsCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
