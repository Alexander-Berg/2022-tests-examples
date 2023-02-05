import MarketOrderEditMapFeature
import UIUtils
import XCTest

final class OrderEditAddressPage: PageObject {

    // MARK: - Public

    var dateSelector: SelectorPage {
        let el = cellUniqueElement(withIdentifier: OrderEditAddressAccessibility.dateSelectorCell)
        return SelectorPage(element: el)
    }

    var timeSelector: SelectorPage {
        let el = cellUniqueElement(withIdentifier: OrderEditAddressAccessibility.timeSelectorCell)
        return SelectorPage(element: el)
    }

    var apartmentInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: OrderEditAddressAccessibility.apartmentInput)
        return TextFieldPage(element: elem)
    }

    var entranceInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: OrderEditAddressAccessibility.entranceInput)
        return TextFieldPage(element: elem)
    }

    var intercomInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: OrderEditAddressAccessibility.intercomInput)
        return TextFieldPage(element: elem)
    }

    var floorInput: TextFieldPage {
        let elem = cellUniqueElement(withIdentifier: OrderEditAddressAccessibility.floorInput)
        return TextFieldPage(element: elem)
    }

    var commentInput: TextViewPage {
        let elem = cellUniqueElement(withIdentifier: OrderEditAddressAccessibility.commentInput)
        return TextViewPage(element: elem)
    }

    var saveButton: SaveButton {
        let el = element
            .otherElements
            .matching(identifier: OrderEditAddressAccessibility.saveButtonCell)
            .descendants(matching: .button)
            .element
        return SaveButton(element: el)
    }

    static var current: OrderEditAddressPage {
        let item = XCUIApplication().otherElements[OrderEditAddressAccessibility.root]
        return OrderEditAddressPage(element: item)
    }
}

// MARK: - CollectionViewPage

extension OrderEditAddressPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OrderEditAddressCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}

// MARK: - Nested types

extension OrderEditAddressPage {

    class SaveButton: PageObject, OrderEditFinishedEntryPoint {}
}
