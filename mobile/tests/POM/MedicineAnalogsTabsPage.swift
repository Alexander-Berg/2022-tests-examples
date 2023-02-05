import XCTest

final class MedicineAnalogsTabsPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = MedicineAnalogsCollectionCellAccessibility

    // MARK: - Properties

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    func tabItem(at position: Int) -> XCUIElement {
        let element = allCellUniqueElement(withIdentifier: MedicineAnalogsAccessibility.tab)[position]
            .otherElements
            .firstMatch

        return element
    }

    func tabText(at position: Int) -> XCUIElement {
        let element = tabItem(at: position)
        return element.staticTexts.firstMatch
    }

}
