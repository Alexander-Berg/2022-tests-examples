import FormKit
import MarketUI
import UIUtils
import XCTest

final class CheckoutContactsPage: PageObject {

    var selectedContactCell: CheckoutContactsChooserCell {
        let elem = element.cells.containing(.image, identifier: RadioButtonViewCellAccessibility.Selectability.selected)
            .element
        return CheckoutContactsChooserCell(element: elem)
    }

    var addButton: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutContactsChooserAccessibility.addButton)
    }

    var doneButton: XCUIElement {
        element
            .collectionViews.firstMatch
            .otherElements.matching(identifier: CheckoutContactsChooserAccessibility.doneButton).firstMatch
    }

    func contactCell(at index: Int) -> CheckoutContactsChooserCell {
        let elem = cellUniqueElement(withIdentifier: RadioButtonViewCellAccessibility.root, index: index)
        return CheckoutContactsChooserCell(element: elem)
    }

    func tapAdd() -> CheckoutRecipientPage {
        addButton.tap()

        let elem = XCUIApplication().otherElements[CheckoutRecipientViewAccessibility.root]
        return CheckoutRecipientPage(element: elem)
    }
}

// MARK: - CollectionViewPage

extension CheckoutContactsPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CheckoutContactsChooserCollectionViewCellsAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}

// MARK: - Nested types

extension CheckoutContactsPage {

    final class CheckoutContactsChooserCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: RadioButtonViewCellAccessibility.radioTitleLabel)
                .element
        }

        var subtitle: XCUIElement {
            element.staticTexts
                .matching(identifier: RadioButtonViewCellAccessibility.radioSubtitleLabel)
                .element
        }

        var editButton: XCUIElement {
            element.buttons
                .firstMatch
        }

        func tapEdit() -> CheckoutRecipientPage {
            editButton.tap()

            let elem = XCUIApplication().otherElements[CheckoutRecipientViewAccessibility.root]
            return CheckoutRecipientPage(element: elem)
        }
    }
}
