import UIUtils
import XCTest

final class HelpIsNearPopupPage: PageObject {
    private static var rootIdentifier: String = HelpIsNearAccessibility.root

    // MARK: - Properties

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: HelpIsNearAccessibility.title)
            .textViews
            .firstMatch
    }

    var text: XCUIElement {
        cellUniqueElement(withIdentifier: HelpIsNearAccessibility.text)
            .textViews
            .firstMatch
    }

    func tap() -> WebViewPage {
        cellUniqueElement(withIdentifier: HelpIsNearAccessibility.button)
            .buttons
            .firstMatch
            .tap()

        return WebViewPage.current
    }

}

extension HelpIsNearPopupPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = HelpIsNearCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
