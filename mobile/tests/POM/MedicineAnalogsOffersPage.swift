import MarketUI
import XCTest

final class MedicineAnalogsOffersPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = MedicineAnalogsCollectionCellAccessibility

    // MARK: - Properties

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    func item(at position: Int) -> XCUIElement {
        let element = allCellUniqueElement(withIdentifier: MedicineAnalogsAccessibility.analogCell)[position]
            .otherElements
            .firstMatch

        return element
    }

    func itemText(at position: Int) -> XCUIElement {
        item(at: position)
            .otherElements
            .matching(identifier: RedesignedSnippetViewAccessebility.descriptionView)
            .buttons
            .firstMatch
    }

}
