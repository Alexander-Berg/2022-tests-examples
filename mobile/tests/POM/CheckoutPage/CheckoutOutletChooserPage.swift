import FormKit
import MarketUI
import UIUtils
import XCTest

class CheckoutOutletChooserPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CheckoutOutletChooserCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews
            .firstMatch
    }

    var selectedOutletCell: CheckoutOutletChooserCell {
        let elem = element.cells.containing(.image, identifier: RadioButtonViewCellAccessibility.Selectability.selected)
            .element
        return CheckoutOutletChooserCell(element: elem)
    }

    var doneButton: XCUIElement {
        element
            .collectionViews.firstMatch
            .otherElements.matching(identifier: CheckoutOutletChooserAccessibility.doneButton).firstMatch
    }

    func outletCell(at index: Int) -> CheckoutOutletChooserCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutOutletChooserAccessibility.outletCell, index: index)
        return CheckoutOutletChooserCell(element: elem)
    }

    func tapAdd() -> CheckoutMapViewPage {
        cellUniqueElement(withIdentifier: CheckoutOutletChooserAccessibility.addButton).tap()

        let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.root]
        return CheckoutMapViewPage(element: elem)
    }
}

// MARK: - Nested types

extension CheckoutOutletChooserPage {

    final class CheckoutOutletChooserCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .firstMatch
        }

        var longCashbackLabel: XCUIElement {
            element.textViews
                .matching(identifier: CheckoutOutletInfoViewAccessibility.longCashbackInfoLabel)
                .element
        }

        func tapToMap() -> CheckoutMapViewPage {
            element.buttons
                .firstMatch
                .tap()
            let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.root]
            return CheckoutMapViewPage(element: elem)
        }
    }
}
