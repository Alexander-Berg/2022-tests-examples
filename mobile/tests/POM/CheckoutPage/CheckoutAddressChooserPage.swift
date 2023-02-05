import FormKit
import MarketUI
import UIUtils
import XCTest

class CheckoutAddressChooserPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CheckoutAddressChooserCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews
            .firstMatch
    }

    var selectedAddressCell: CheckoutAddressChooserCell {
        let elem = element.cells.containing(.image, identifier: RadioButtonViewCellAccessibility.Selectability.selected)
            .element
        return CheckoutAddressChooserCell(element: elem)
    }

    var doneButton: XCUIElement {
        element
            .collectionViews.firstMatch
            .otherElements.matching(identifier: CheckoutAddressChooserAccessibility.doneButton).firstMatch
    }

    func addressCell(at index: Int) -> CheckoutAddressChooserCell {
        let elem = cellUniqueElement(withIdentifier: RadioButtonViewCellAccessibility.root, index: index)
        return CheckoutAddressChooserCell(element: elem)
    }

    func tapAdd() -> CheckoutMapViewPage {
        cellUniqueElement(withIdentifier: CheckoutAddressChooserAccessibility.addButton).tap()

        let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.root]
        return CheckoutMapViewPage(element: elem)
    }
}

// MARK: - Nested types

extension CheckoutAddressChooserPage {

    final class CheckoutAddressChooserCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .firstMatch
        }

        func tapEdit() -> EditAddressPage {
            element.buttons
                .firstMatch
                .tap()

            let elem = XCUIApplication().otherElements[EditAddressAccessibility.root]
            return EditAddressPage(element: elem)
        }
    }
}
