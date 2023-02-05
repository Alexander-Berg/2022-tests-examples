import UIUtils
import XCTest

/// PageObject grid-виджета с дженерик типом сниппета и провайдера идентификаторов.
class GridWidgetPage
<AccessibilityType: CollectionViewCellsAccessibility, CellPageType: PageObject>: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = AccessibilityType

    var collectionView: XCUIElement {
        element
    }

    var title: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: GridAccessibility.title)
        return elem.staticTexts.firstMatch
    }

    func cellPage(after elements: [String] = []) -> CellPageType {
        let elem = cellUniqueElement(withIdentifier: GridAccessibility.snippet, after: elements)
        return CellPageType(element: elem)
    }
}
