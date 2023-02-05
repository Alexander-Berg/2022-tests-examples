import UIUtils
import XCTest

final class EditAddressPage: PageObject {

    static var current: EditAddressPage {
        let elem = XCUIApplication().otherElements[EditAddressAccessibility.root]
        return EditAddressPage(element: elem)
    }

    var addressCellView: XCUIElement {
        cellUniqueElement(withIdentifier: EditAddressAccessibility.addressTextField)
            .staticTexts
            .firstMatch
    }

    var cityInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.cityInput)
        return TextFieldPage(element: elem)
    }

    var citySuggestView: FlowPopupPage {
        let elem = element
            .collectionViews
            .matching(identifier: FlowPopupAccessibility.cityRoot)
            .firstMatch
        return FlowPopupPage(element: elem)
    }

    var streetInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.streetInput)
        return TextFieldPage(element: elem)
    }

    var streetSuggestView: FlowPopupPage {
        let elem = element
            .collectionViews
            .matching(identifier: FlowPopupAccessibility.streetRoot)
            .firstMatch
        return FlowPopupPage(element: elem)
    }

    var houseInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.houseInput)
        return TextFieldPage(element: elem)
    }

    var apartmentInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.apartmentInput)
        return TextFieldPage(element: elem)
    }

    var entranceIntercomExpander: XCUIElement {
        cellUniqueElement(withIdentifier: EditAddressAccessibility.entranceIntercomExpander)
    }

    var entranceInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.entranceInput)
        return TextFieldPage(element: elem)
    }

    var intercomInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.intercomInput)
        return TextFieldPage(element: elem)
    }

    var floorInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.floorInput)
        return TextFieldPage(element: elem)
    }

    var commentInput: TextViewPage {
        let elem = cellUniqueElement(withIdentifier: EditAddressAccessibility.commentInput)
        return TextViewPage(element: elem)
    }

    var continueButton: XCUIElement {
        element
            .otherElements
            .matching(identifier: EditAddressAccessibility.continueButton)
            .descendants(matching: .button)
            .element
    }

    var backButton: XCUIElement {
        element.buttons
            .matching(identifier: EditAddressAccessibility.closeButton)
            .firstMatch
    }

    var deleteButton: XCUIElement {
        XCUIApplication().buttons
            .matching(identifier: EditAddressAccessibility.deleteButton)
            .element
    }
}

// MARK: - CollectionViewPage

extension EditAddressPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = EditAddressCollectionViewCellAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}
