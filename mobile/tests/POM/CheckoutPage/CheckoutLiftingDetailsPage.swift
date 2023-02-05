import MarketUI
import XCTest

class CheckoutLiftingDetailsPage: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = CheckoutLiftingTypeCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var doneButton: XCUIElement {
        element.otherElements
            .matching(identifier: CheckoutLiftingDetailsAccessibility.doneButton)
            .element
    }

    var floorInput: TextFieldPage {
        TextFieldPage(element: cellUniqueElement(withIdentifier: CheckoutLiftingDetailsAccessibility.floorInput))
    }

    var errorMessage: TextLabelPage {
        let element = cellUniqueElement(withIdentifier: ValidableTextFieldCellGroupAccessibility.messageCell)
        return TextLabelPage(element: element)
    }

    var commentInput: TextViewPage {
        TextViewPage(element: cellUniqueElement(withIdentifier: CheckoutLiftingDetailsAccessibility.commentInput))
    }

    func liftingType(with identifier: String) -> RadioButtonPage {
        let element = cellUniqueElement(withIdentifier: identifier)
        return RadioButtonPage(element: element)
    }

    func selectLiftingType(with identifier: String) {
        liftingType(with: identifier).element.tap()
    }
}
