import UIUtils
import XCTest

final class CheckoutDeliveryPage: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = CheckoutDeliveryCollectionViewCellAccessibility

    final class ContinueButton: PageObject {
        func tapToNextShipment() -> CheckoutDeliveryPage {
            element.buttons.firstMatch.tap()
            let elem = XCUIApplication().otherElements[CheckoutDeliveryAccessibility.root]
            return CheckoutDeliveryPage(element: elem)
        }

        func tap() -> CheckoutPaymentMethodPage {
            element.buttons.firstMatch.tap()
            let elem = XCUIApplication().otherElements[CheckoutPaymentMethodAccessibility.root]
            return CheckoutPaymentMethodPage(element: elem)
        }
    }

    final class PickupPointsButton: PageObject {

        func tap() -> PickupPointsMapPage {
            element.coordinate(withNormalizedOffset: CGVector(dx: 0.1, dy: 0.1)).tap()
            let elem = XCUIApplication().otherElements[PickupPointsMapAccessibility.root]
            return PickupPointsMapPage(element: elem)
        }
    }

    final class EditAddressButton: PageObject, OrderEditFinishedEntryPoint {}

    final class SwitchToPickupButton: PageObject {
        func tap() -> PickupPointsMapPage {
            element.buttons.firstMatch.tap()
            let elem = XCUIApplication().otherElements[PickupPointsMapAccessibility.root]
            return PickupPointsMapPage(element: elem)
        }
    }

    final class TextFieldCell: PageObject {

        var textField: XCUIElement {
            element.textFields.firstMatch
        }
    }

    final class PickupInfo: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: CheckoutPickupInfoViewAccessibility.title)
                .firstMatch
        }

        var address: XCUIElement {
            element.staticTexts
                .matching(identifier: CheckoutPickupInfoViewAccessibility.address)
                .firstMatch
        }

        var storagePeriod: XCUIElement {
            element.staticTexts
                .matching(identifier: CheckoutPickupInfoViewAccessibility.storagePeriod)
                .firstMatch
        }
    }

    final class OutletInfo: PageObject {
        final class DetailsButton: PageObject {
            func tap() -> CheckoutPickupInfoPage {
                element.tap()
                let el = XCUIApplication().scrollViews[CheckoutPickupInfoViewControllerAccessibility.root]
                return CheckoutPickupInfoPage(element: el)
            }
        }

        var pickupInfo: PickupInfo {
            let el = element.otherElements
                .matching(identifier: CheckoutOutletViewAccessibility.pickupInfo)
                .firstMatch
            return PickupInfo(element: el)
        }

        var detailsButton: DetailsButton {
            let el = element.buttons
                .matching(identifier: CheckoutOutletViewAccessibility.detailsButton)
                .firstMatch
            return DetailsButton(element: el)
        }
    }

    /// Current element
    static var current: CheckoutDeliveryPage {
        let elem = XCUIApplication().otherElements[CheckoutDeliveryAccessibility.root]
        return CheckoutDeliveryPage(element: elem)
    }

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var serviceDeliveryTypeCell: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.serviceTypeCell)
    }

    var separatedDeliveryControl: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.separatedDeliveryControl)
            .buttons
            .firstMatch
    }

    var streetInput: TextFieldCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.streetInput)
        return TextFieldCell(element: elem)
    }

    var streetSuggestView: FlowPopupPage {
        let el = element.collectionViews.matching(identifier: FlowPopupAccessibility.streetRoot).firstMatch
        return FlowPopupPage(element: el)
    }

    var houseInput: TextFieldCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.houseInput)
        return TextFieldCell(element: elem)
    }

    var apartmentInput: TextFieldCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.apartmentInput)
        return TextFieldCell(element: elem)
    }

    var entranceIntercomExpander: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.entranceIntercomExpander)
    }

    var entranceInput: TextFieldCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.entranceInput)
        return TextFieldCell(element: elem)
    }

    var intercomInput: TextFieldCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.intercomInput)
        return TextFieldCell(element: elem)
    }

    var addNewAddressButton: EditAddressButton {
        let elem = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.addNewAddressControl).buttons
            .firstMatch
        return EditAddressButton(element: elem)
    }

    var nameTextField: XCUIElement {
        cellElement(at: IndexPath(item: 1, section: 9))
    }

    var emailTextField: XCUIElement {
        cellElement(at: IndexPath(item: 4, section: 9))
    }

    var phoneTextField: XCUIElement {
        cellElement(at: IndexPath(item: 6, section: 9))
    }

    // address presets
    func addressPreset(at index: Int = 0) -> XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.addressPreset, index: index)
    }

    var continueButton: ContinueButton {
        let el = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.continueButton)
        return ContinueButton(element: el)
    }

    var outletInfo: OutletInfo {
        let el = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.outletInfo)
        return OutletInfo(element: el)
    }

    var chooseAnotherPickupPointButton: PickupPointsButton {
        let el = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.pickupPointsButton)
        return PickupPointsButton(element: el)
    }

    var switchToPickupButton: SwitchToPickupButton {
        let el = cellUniqueElement(withIdentifier: CheckoutDeliveryAccessibility.switchToPickupButton)
        return SwitchToPickupButton(element: el)
    }
}
