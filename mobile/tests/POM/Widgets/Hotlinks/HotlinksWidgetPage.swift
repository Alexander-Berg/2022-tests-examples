import XCTest

final class HotlinksGridWidgetPage: PageObject {

    class HotlinksGridWidgetCell: PageObject {}

    var cell: HotlinksGridWidgetCell {
        let format = "identifier CONTAINS '\(HotlinkGridWidgetCellAccessibility.baseIdentifier)'"
        let predicate = NSPredicate(format: format)
        let elem = element.cells.matching(predicate).firstMatch
        return HotlinksGridWidgetCell(element: elem)
    }

}

final class HotlinksScrollWidgetPage<CellPageType: PageObject>: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = HotlinkScrollWidgetCellsAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }

    var container: HotlinksScrollContainerPage<CellPageType> {
        let elem = cellUniqueElement(withIdentifier: HotlinkScrollWidgetAccessibility.container)
        return HotlinksScrollContainerPage(element: elem)
    }

}

class HotlinksScrollContainerPage<CellPageType: PageObject>: PageObject, UniformCollectionViewPage {
    typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility
    typealias CellPage = CellPageType

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
