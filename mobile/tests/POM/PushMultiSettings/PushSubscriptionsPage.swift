import UIUtils
import XCTest

final class PushSubscriptionsPage: PageObject {

    // MARK: - Properties

    static var current: Self {
        let element = XCUIApplication()
            .any
            .matching(identifier: PushMultiSettingsAccessibility.root)
            .firstMatch
        return .init(element: element)
    }

    var switches: XCUIElementQuery {
        cellUniqueElement(withIdentifier: PushMultiSettingsAccessibility.switcher)
            .switches
    }

    var disabledSectionButton: XCUIElement {
        cellUniqueElement(withIdentifier: PushMultiSettingsAccessibility.disabledSectionButton)
            .buttons
            .firstMatch
    }

    var errorView: BarrierViewPage {
        BarrierViewPage(element: element)
    }

}

// MARK: - CollectionViewPage

extension PushSubscriptionsPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = PushMultiSettingsCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
