import UIUtils
import XCTest

class ComparisonListPage: PageObject {

    class CollectionView: PageObject, UniformCollectionViewPage {
        typealias AccessibilityIdentifierProvider = ComparisonListCollectionViewCellsAccessibility
        typealias CellPage = ComparisonListCellPage

        var collectionView: XCUIElement {
            element
        }
    }

    final class ComparisonListCellPage: PageObject {

        /// Происходит переход в экран со сравнением
        func tap() -> ComparisonPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: ComparisonAccessibility.root)
                .firstMatch
            return ComparisonPage(element: elem)
        }

        var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: ComparisonListAccessibility.Category.title)
                .firstMatch
        }

        var count: XCUIElement {
            element
                .staticTexts
                .matching(identifier: ComparisonListAccessibility.Category.count)
                .firstMatch
        }
    }

    var root: XCUIElement {
        element
            .otherElements
            .matching(identifier: ComparisonListAccessibility.root)
            .firstMatch
    }

    var collectionView: CollectionView {
        let elem = element
            .collectionViews
            .matching(identifier: ComparisonListAccessibility.collectionView)
            .firstMatch
        return CollectionView(element: elem)
    }

    var emptyView: BarrierViewPage {
        let elem = element
            .otherElements
            .matching(identifier: ComparisonListAccessibility.empty)
            .firstMatch

        return BarrierViewPage(element: elem)
    }

    var comparisonCell: ComparisonListCellPage {
        let elem = collectionView
            .cellUniqueElement(withIdentifier: ComparisonListAccessibility.Category.cell)
            .firstMatch

        return ComparisonListCellPage(element: elem)
    }

    func comparisonCell(with index: Int) -> ComparisonListCellPage {
        let elem = collectionView
            .cellUniqueElement(
                withIdentifier: ComparisonListAccessibility.Category.cell,
                index: index
            )
            .firstMatch

        return ComparisonListCellPage(element: elem)
    }

    var fistComparisonCell: ComparisonListCellPage {
        comparisonCell(with: 0)
    }

    var loginButton: XCUIElement {
        element
            .buttons
            .matching(identifier: ComparisonListAccessibility.Login.button)
            .firstMatch
    }

}
