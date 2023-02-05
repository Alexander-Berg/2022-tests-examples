import MarketCashback
import MarketUI
import UIUtils
import XCTest

final class CashbackDetailsAboutPage: PageObject, UniformCollectionViewPage {

    typealias CellPage = DetailsAboutCellPage

    typealias AccessibilityIdentifierProvider = CashbackDetailsAboutCellsAccessibility

    static var current: CashbackDetailsAboutPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: CashbackDetailsAboutAccessibility.root)
            .firstMatch
        return CashbackDetailsAboutPage(element: elem)
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

    class DetailsAboutCellPage: PageObject {}

    func detailsItem(at index: Int) -> CashbackDetailsItem {
        let element = allCellUniqueElement(withIdentifier: CashbackDetailsAboutAccessibility.item)[index].otherElements
            .firstMatch
        return CashbackDetailsItem(element: element)
    }

    func groupTitle(at index: Int) -> XCUIElement {
        allCellUniqueElement(withIdentifier: CashbackDetailsAboutAccessibility.groupTitle)[index].textViews
            .firstMatch
    }

    func groupDescription(at index: Int) -> XCUIElement {
        allCellUniqueElement(withIdentifier: CashbackDetailsAboutAccessibility.groupDescription)[index].textViews
            .firstMatch
    }

    class CashbackDetailsItem: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.title)
                .firstMatch
        }

        var value: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.details)
                .firstMatch
        }
    }
}
