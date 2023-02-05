import MarketUI
import UIUtils
import XCTest

final class CheckoutRecipientPage: PageObject {

    static var current: CheckoutRecipientPage {
        let elem = XCUIApplication().otherElements[CheckoutRecipientViewAccessibility.root]
        return CheckoutRecipientPage(element: elem)
    }

    var continueButton: XCUIElement {
        element.otherElements
            .matching(identifier: CheckoutRecipientViewAccessibility.continueButton)
            .element
    }

    var nameTextField: TextFieldPage {
        TextFieldPage(element: cellUniqueElement(withIdentifier: CheckoutRecipientViewAccessibility.nameField))
    }

    var emailTextField: TextFieldPage {
        TextFieldPage(element: cellUniqueElement(withIdentifier: CheckoutRecipientViewAccessibility.emailField))
    }

    var phoneTextField: TextFieldPage {
        TextFieldPage(element: cellUniqueElement(withIdentifier: CheckoutRecipientViewAccessibility.phoneField))
    }

    var deleteButton: XCUIElement {
        XCUIApplication().buttons
            .matching(identifier: CheckoutRecipientViewAccessibility.deleteButton)
            .element
    }

    func message(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: ValidableTextFieldCellGroupAccessibility.messageCell, index: index)
            .staticTexts
            .element
    }
}

// MARK: - CollectionViewPage

extension CheckoutRecipientPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CheckoutRecipientViewCollectionViewCellsAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}
