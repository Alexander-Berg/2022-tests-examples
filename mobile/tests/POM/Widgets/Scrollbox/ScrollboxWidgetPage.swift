import UIUtils
import XCTest

/// PageObject scrollbox-виджета с дженерик типом сниппета.
class LegacyScrollBoxWidgetPage<CellPageType: PageObject>: PageObject, UniformCollectionViewPage {
    typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility
    typealias CellPage = CellPageType

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var title: XCUIElement {
        element
            .any.matching(identifier: WidgetsAccessibility.decoratedTitle)
            .firstMatch
    }
}

/// PageObject scrollbox-виджета с дженерик типом сниппета и провайдера идентификаторов.
class ScrollBoxWidgetPage
<AccessibilityType: CollectionViewCellsAccessibility, CellPageType: PageObject>: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = AccessibilityType

    var collectionView: XCUIElement {
        element
    }

    var container: ScrollBoxSnipppetContainerPage<CellPageType> {
        let elem = cellUniqueElement(withIdentifier: ScrollBoxAccessibility.container)
        return ScrollBoxSnipppetContainerPage<CellPageType>(element: elem)
    }

    var title: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: ScrollBoxAccessibility.title)
        return elem.staticTexts.firstMatch
    }

    var showMoreButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: ScrollBoxAccessibility.title)
        return elem.buttons.firstMatch
    }

}

/// PageObject ScrollBoxCellController с дженерик типом сниппета.
class ScrollBoxSnipppetContainerPage<CellPageType: PageObject>: PageObject, UniformCollectionViewPage {
    typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility
    typealias CellPage = CellPageType

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
