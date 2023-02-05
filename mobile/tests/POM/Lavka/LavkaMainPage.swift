import MarketLavkaFeature
import MarketUI
import XCTest

final class LavkaMainPage: PageObject {

    // MARK: - Properties

    static var current: LavkaMainPage {
        let item = XCUIApplication().otherElements[LavkaMainAccessibility.root]
        return LavkaMainPage(element: item)
    }

    var collectionView: XCUIElement {
        element.collectionViews.matching(identifier: LavkaMainAccessibility.collectionView).firstMatch
    }

    /// Ячейки типа Tile
    var categoryTileCollectionView: CategoryTileCollectionViewPage {
        CategoryTileCollectionViewPage(element: collectionView)
    }

    /// Ячейки типа GroupHeader
    var categoryGroupHeaderCollectionView: CategoryGroupHeaderCollectionViewPage {
        CategoryGroupHeaderCollectionViewPage(element: collectionView)
    }
}

// MARK: - Nested types

extension LavkaMainPage {

    class CategoryTileCollectionViewPage: PageObject, UniformCollectionViewPage {
        typealias CellPage = CategoryTileCellPage

        typealias AccessibilityIdentifierProvider = LavkaMainTileCellsAccessibility

        var collectionView: XCUIElement { element }
    }

    /// Класс cell категории Tile
    class CategoryTileCellPage: PageObject {

        var titleLabel: XCUIElement {
            element.staticTexts.matching(identifier: LavkaMainAccessibility.titleTileCell).firstMatch
        }
    }

    class CategoryGroupHeaderCollectionViewPage: PageObject, UniformCollectionViewPage {
        typealias CellPage = CategoryGroupHeaderCellPage

        typealias AccessibilityIdentifierProvider = LavkaMainGroupHeaderCellsAccessibility

        var collectionView: XCUIElement { element }
    }

    /// Класс cell категории GroupHeader
    class CategoryGroupHeaderCellPage: PageObject {

        var textView: XCUIElement {
            element.textViews.matching(identifier: TextLabelAccessibility.textView).firstMatch
        }
    }
}
