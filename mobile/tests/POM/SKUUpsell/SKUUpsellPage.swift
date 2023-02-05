import MarketRecommendationsFeature
import XCTest

final class SKUUpsellPage: PageObject {

    static var current: SKUUpsellPage {
        let el = XCUIApplication().otherElements[UpsellAccessibility.root]
        return SKUUpsellPage(element: el)
    }

    var productInCartWidget: ProductInCartPage {
        ProductInCartPage(
            element: cellUniqueElement(withIdentifier: ProductInCartAccessibility.root)
        )
    }

    var repeatPurchasesTitle: XCUIElement {
        let indexPath = IndexPath(item: 0, section: 3)
        let element = cellElement(at: indexPath)
        return element.staticTexts.firstMatch
    }

    func repeatPurchasesSnippetAt(index: Int) -> SnippetPage {
        let indexPath = IndexPath(item: index, section: 3)
        let element = cellElement(at: indexPath)
        return SnippetPage(element: element)
    }
}

// MARK: - CollectionViewPage

extension SKUUpsellPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = UpsellCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
