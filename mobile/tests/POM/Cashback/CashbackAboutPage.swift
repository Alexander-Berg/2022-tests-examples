import MarketCashback
import UIUtils
import XCTest

final class CashbackAboutPage: PageObject, UniformCollectionViewPage {

    typealias CellPage = AboutCellPage

    typealias AccessibilityIdentifierProvider = CashbackAboutCellsAccessibility

    static var current: CashbackAboutPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: CashbackAboutAccessibility.root)
            .firstMatch
        return CashbackAboutPage(element: elem)
    }

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: CashbackAboutAccessibility.title).textViews.firstMatch
    }

    var linkButton: XCUIElement {
        cellUniqueElement(withIdentifier: CashbackAboutAccessibility.linkButton).buttons.firstMatch
    }

    var closeButton: XCUIElement {
        cellUniqueElement(withIdentifier: CashbackAboutAccessibility.closeButton).buttons.firstMatch
    }

    func descriptionText(at index: Int) -> XCUIElement {
        allCellUniqueElement(withIdentifier: CashbackAboutAccessibility.text)[index].textViews.firstMatch
    }

    class AboutCellPage: PageObject {}
}
