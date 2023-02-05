import UIUtils
import XCTest

final class SettingsPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = SettingsCollectionViewCellsAccessibility

    /// Вся страница SettingsViewController
    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var regionTitle: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SettingsAccessibility.regionTitle)

        return elem.staticTexts.firstMatch
    }

    var regionValue: RegionValue {
        let elem = cellUniqueElement(withIdentifier: SettingsAccessibility.regionValue)

        return RegionValue(element: elem.staticTexts.firstMatch)
    }
}

// MARK: - Nested types

extension SettingsPage {

    final class RegionValue: PageObject, RegionSelectEntryPoint {}

}
