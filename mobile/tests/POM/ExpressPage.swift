import MarketUI
import XCTest

final class ExpressPage: PageObject, UniformCollectionViewPage {

    typealias AccessibilityIdentifierProvider = ExpressCollectionViewCellsAccessibility

    typealias CellPage = ExpressCellPage

    var collectionView: XCUIElement {
        element.collectionViews.matching(identifier: ExpressAccessibility.collection)
            .firstMatch
    }

    var plusButton: XCUIElement {
        element
            .buttons
            .matching(identifier: MordaAccessibility.plusButton)
            .firstMatch
    }

    var searchCell: XCUIElement {
        cellUniqueElement(withIdentifier: ExpressAccessibility.searchCell)
    }

    func goToSearch() -> SearchPage {
        searchCell.tap()
        let subElem = XCUIApplication().otherElements[SearchAccessibility.root]
        XCTAssertTrue(subElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return SearchPage(element: subElem)
    }

    class ExpressCellPage: PageObject {}
}
