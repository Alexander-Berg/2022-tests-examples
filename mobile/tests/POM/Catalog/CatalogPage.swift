import MarketUI
import UIUtils
import XCTest

final class CatalogPage: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = CatalogCollectionViewAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    /// Заголовок виджета рекомендаций
    var recommendationsTitle: XCUIElement {
        let indexPath = IndexPath(item: 1, section: 1)
        let titleCell = cellElement(at: indexPath)
        return titleCell.staticTexts.firstMatch
    }

    var searchButton: SearchButtonPage {
        let el = element
            .descendants(matching: .any)
            .matching(identifier: NavigationBarAccessibility.searchViewSearchButton)
            .firstMatch

        return SearchButtonPage(element: el)
    }

    func recommendationCell(at index: IndexPath) -> RecommendationCell {
        let el = cellElement(at: index)
        return RecommendationCell(element: el)
    }

    func departmentCell(at index: IndexPath) -> DepartamentCell {
        let el = cellElement(at: index)
        return DepartamentCell(element: el)
    }

    func departmentTitle(matching text: String) -> DepartmentTitle {
        let element = collectionView
            .descendants(matching: .staticText)
            .matching(identifier: "mainCatalogCellTitle")
            .matching(NSPredicate(format: "label MATCHES '\(text)'"))
            .firstMatch
        return DepartmentTitle(element: element)
    }
}

// MARK: - Nested types

extension CatalogPage {

    class DepartamentCell: PageObject {
        var image: XCUIElement {
            element.images.firstMatch
        }

        var title: XCUIElement {
            element.staticTexts.firstMatch
        }

        func tap() -> SubcategoryPage {
            element.tap()
            let subElem = XCUIApplication().otherElements[SubcategoryAccessibility.root]
            return SubcategoryPage(element: subElem)
        }
    }

    class DepartmentTitle: PageObject {
        func tap() -> SubcategoryPage {
            element.tap()
            let subcategoryPageElement = XCUIApplication().otherElements[SubcategoryAccessibility.root]
            return SubcategoryPage(element: subcategoryPageElement)
        }
    }

    class RecommendationCell: PageObject, SKUEntryPoint {

        var cartButton: CartButtonPage {
            let elem = element.buttons.matching(identifier: CatalogAccessibility.productSnippetBuyButton).firstMatch
            return CartButtonPage(element: elem)
        }

        var image: XCUIElement {
            element.images.firstMatch
        }

        var name: XCUIElement {
            element
                .buttons
                .matching(identifier: CatalogAccessibility.productSnippetName)
                .firstMatch
        }

        var price: XCUIElement {
            element
                .staticTexts
                .matching(identifier: CatalogAccessibility.productSnippetPrice)
                .firstMatch
        }
    }
}
