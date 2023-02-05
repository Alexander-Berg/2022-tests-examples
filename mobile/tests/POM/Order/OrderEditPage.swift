import UIUtils
import XCTest

final class OrderEditPage: PageObject {

    // MARK: - Public

    var deliveryDateTitle: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.deliveryDateTitleCell)
            .textViews
            .firstMatch
    }

    var recipientTitle: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.recipientTitleCell)
            .textViews
            .firstMatch
    }

    var storagePeriodTitle: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.storagePeriodTitleCell)
            .textViews
            .firstMatch
    }

    var warning: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.warningCell)
            .staticTexts
            .firstMatch
    }

    var dateSelector: SelectorPage {
        let el = cellUniqueElement(withIdentifier: OrderEditAccessibility.dateSelectorCell)
        return SelectorPage(element: el)
    }

    var timeSelector: SelectorPage {
        let el = cellUniqueElement(withIdentifier: OrderEditAccessibility.timeSelectorCell)
        return SelectorPage(element: el)
    }

    var nameField: TextField {
        let el = cellUniqueElement(withIdentifier: OrderEditAccessibility.nameFieldCell)
        return TextField(element: el)
    }

    var nameMessage: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.nameMessageCell)
            .textViews
            .firstMatch
    }

    var phoneField: TextField {
        let el = cellUniqueElement(withIdentifier: OrderEditAccessibility.phoneFieldCell)
        return TextField(element: el)
    }

    var phoneMessage: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.phoneMessageCell)
            .textViews
            .firstMatch
    }

    var disclaimer: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.disclaimerCell)
            .textViews
            .firstMatch
    }

    var hourIntervalsTitle: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.hourIntervalsTitleCell)
            .textViews
            .firstMatch
    }

    var hourIntervalsDisclaimer: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.hourIntervalsDisclaimerCell)
            .textViews
            .firstMatch
    }

    var hourIntervalsItems: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.hourIntervalsItemsCell)
            .collectionViews
            .firstMatch
    }

    var callCourierTitle: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.callCourierTitleCell)
            .textViews
            .firstMatch
    }

    var callCourierDisclaimer: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.callCourierDisclaimerCell)
            .textViews
            .firstMatch
    }

    var callCourierItems: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditAccessibility.callCourierItemsCell)
            .collectionViews
            .firstMatch
    }

    var saveButton: SaveButton {
        let el = cellUniqueElement(withIdentifier: OrderEditAccessibility.confirmButtonCell)
        return SaveButton(element: el)
    }

    var cancelButton: CancelButton {
        let el = cellUniqueElement(withIdentifier: OrderEditAccessibility.rejectButtonCell)
        return CancelButton(element: el)
    }

    static var current: OrderEditPage {
        let item = XCUIApplication().otherElements[OrderEditAccessibility.root]
        return OrderEditPage(element: item)
    }

}

// MARK: - CollectionViewPage

extension OrderEditPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OrderEditCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - Nested types

extension OrderEditPage {

    class SaveButton: PageObject, OrderEditFinishedEntryPoint {

        var button: XCUIElement {
            element.buttons.firstMatch
        }

    }

    class CancelButton: PageObject, OrderDetailsEntryPoint {

        var button: XCUIElement {
            element.buttons.firstMatch
        }

    }

}
