import UIUtils
import XCTest

/// PageObject listBox-виджета с дженерик типом сниппета и провайдера идентификаторов.
class ListBoxWidgetPage
<AccessibilityType: CollectionViewCellsAccessibility, CellPageType: PageObject>: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = AccessibilityType

    var collectionView: XCUIElement {
        element
    }

    var title: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: ListBoxAccessibility.title)
        return elem.staticTexts.firstMatch
    }

    func cellPage(after elements: [String] = []) -> CellPageType {
        let elem = cellUniqueElement(withIdentifier: ListBoxAccessibility.snippet, after: elements)
        return CellPageType(element: elem)
    }
}
