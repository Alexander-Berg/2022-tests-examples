import MarketUI
import UIUtils
import XCTest

final class SubcategoryPage: PageObject {

    static var current: SubcategoryPage {
        let el = XCUIApplication().otherElements[SubcategoryAccessibility.root]
        return SubcategoryPage(element: el)
    }

    var backButton: XCUIElement {
        element
            .buttons
            .matching(identifier: SubcategoryAccessibility.backButton)
            .firstMatch
    }

    var searchButton: SearchButtonPage {
        let el = element
            .descendants(matching: .any)
            .matching(identifier: SubcategoryAccessibility.navSearch)
            .firstMatch

        return SearchButtonPage(element: el)
    }

    var header: SubcategoryHeader {
        let elem = cellUniqueElement(withIdentifier: GridAccessibility.title)
        return SubcategoryHeader(element: elem)
    }

    var listHeader: SubcategoryHeader {
        let elem = cellUniqueElement(withIdentifier: ListBoxAccessibility.title)
        return SubcategoryHeader(element: elem)
    }

    func subcategoryTreeCell(index: Int) -> SubcategoryCell {
        let elem = cellElement(at: index)
        return SubcategoryCell(element: elem)
    }

    func subcategoryTitle(matching text: String) -> SubcategoryTitle {
        let element = collectionView
            .descendants(matching: .staticText)
            .matching(NSPredicate(format: "label MATCHES '\(text)'"))
            .firstMatch
        return SubcategoryTitle(element: element)
    }

    func recommendationCell(index: IndexPath) -> RecommendationCell {
        let elem = cellElement(at: index)
        return RecommendationCell(element: elem)
    }
}

// MARK: - CollectionViewPage

extension SubcategoryPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = SubcategoryCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - Nested types

extension SubcategoryPage {

    class SubcategoryTitle: PageObject {
        func goToFeed() -> FeedPage {
            element.tap()
            let subElem = XCUIApplication().otherElements[FeedAccessibility.root]
            return FeedPage(element: subElem)
        }

        func goToSubcategory() -> SubcategoryPage {
            element.tap()
            let subElem = XCUIApplication().otherElements[SubcategoryAccessibility.root]
            return SubcategoryPage(element: subElem)
        }
    }

    class SubcategoryHeader: PageObject {
        var title: XCUIElement {
            element.staticTexts.firstMatch
        }

        var showAllButton: SubcategoryCell {
            let elem = element.buttons.firstMatch
            return SubcategoryCell(element: elem)
        }
    }

    /// Класс ячейки сабкатегории
    class SubcategoryCell: PageObject {
        var title: XCUIElement {
            element.staticTexts.firstMatch
        }

        var image: XCUIElement {
            element.images.firstMatch
        }

        func goToSubcategory() -> SubcategoryPage {
            element.tap()
            let subElem = XCUIApplication().otherElements[SubcategoryAccessibility.root]
            return SubcategoryPage(element: subElem)
        }

        func goToFeed() -> FeedPage {
            element.tap()
            let subElem = XCUIApplication().otherElements[FeedAccessibility.root]
            return FeedPage(element: subElem)
        }
    }

    class RecommendationCell: PageObject {
        var cartButton: XCUIElement {
            element.buttons.matching(
                identifier: CatalogAccessibility.productSnippetBuyButton
            ).firstMatch
        }
    }
}
