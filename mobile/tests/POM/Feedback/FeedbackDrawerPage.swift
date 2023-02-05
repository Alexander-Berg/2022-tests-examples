import UIUtils
import XCTest

final class FeedbackDrawerPage: PageObject {

    static var current: FeedbackDrawerPage {
        let element = XCUIApplication().any.matching(identifier: FeedbackDrawerAccessibility.root).firstMatch
        return .init(element: element)
    }

}

// MARK: - CollectionViewPage

extension FeedbackDrawerPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = FeedbackDrawerCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
