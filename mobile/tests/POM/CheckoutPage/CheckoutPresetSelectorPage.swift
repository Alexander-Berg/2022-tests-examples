import FormKit
import MarketUI
import UIUtils
import XCTest

class CheckoutPresetSelectorPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CheckoutPresetSelectorCollectionViewCellsAccessibility

    // MARK: - Private

    private var checkoutMapViewPage: CheckoutMapViewPage {
        let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.root]
        return CheckoutMapViewPage(element: elem)
    }

    // MARK: - Public

    var collectionView: XCUIElement {
        element.collectionViews
            .firstMatch
    }

    var noSuitableTitle: String {
        cellUniqueElement(withIdentifier: CheckoutOutletChooserAccessibility.noSuitableTitleCell)
            .textViews.firstMatch.label
    }

    var selectedChipTitle: String {
        element
            .buttons
            .matching(identifier: SegmentedButtonViewAccessibility.selectedButton)
            .firstMatch
            .label
    }

    var selectedAddressCell: CheckoutAddressChooserPage.CheckoutAddressChooserCell {
        let elem = element.cells.containing(.image, identifier: RadioButtonViewCellAccessibility.Selectability.selected)
            .element
        return CheckoutAddressChooserPage.CheckoutAddressChooserCell(element: elem)
    }

    func selectChip(at index: Int) {
        collectionView
            .otherElements
            .matching(identifier: SegmentedButtonViewAccessibility.root)
            .descendants(matching: .button)
            .element(boundBy: index)
            .tap()
    }

    func addressCell(at index: Int) -> CheckoutAddressChooserPage.CheckoutAddressChooserCell {
        let elem = cellUniqueElement(withIdentifier: RadioButtonViewCellAccessibility.root, index: index)
        return CheckoutAddressChooserPage.CheckoutAddressChooserCell(element: elem)
    }

    func addressDetailsCell(at index: Int = 0) -> PresetDetailTextLabelPage {
        let elem = cellUniqueElement(
            withIdentifier: CheckoutAddressChooserAccessibility.addressDetailsCell,
            index: index
        )
        return PresetDetailTextLabelPage(element: elem)
    }

    func outletCell(at index: Int) -> CheckoutOutletChooserPage.CheckoutOutletChooserCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutOutletChooserAccessibility.outletCell, index: index)
        return CheckoutOutletChooserPage.CheckoutOutletChooserCell(element: elem)
    }

    func outletDetailsCell(at index: Int = 0) -> PresetDetailTextLabelPage {
        let elem = cellUniqueElement(
            withIdentifier: CheckoutOutletChooserAccessibility.outletDetailsCell,
            index: index
        )
        return PresetDetailTextLabelPage(element: elem)
    }

    func tapAddOutlet() -> CheckoutMapViewPage {
        cellUniqueElement(withIdentifier: CheckoutOutletChooserAccessibility.addButton).tap()
        return checkoutMapViewPage
    }

    func tapAddAddressFromHeader() -> CheckoutMapViewPage {
        headerStackView.addAddressButton.tap()
        return checkoutMapViewPage
    }
}

extension CheckoutPresetSelectorPage {

    class HeaderStackView: PageObject {
        var addAddressButton: XCUIElement {
            element
                .buttons
                .matching(identifier: CheckoutAddressChooserAccessibility.addButton)
                .firstMatch
        }

        var selectedChipButton: XCUIElement {
            element
                .buttons
                .matching(identifier: SegmentedButtonViewAccessibility.selectedButton)
                .firstMatch
        }

        func selectedChipButton(at index: Int) -> XCUIElement {
            element
                .otherElements
                .matching(identifier: SegmentedButtonViewAccessibility.root)
                .descendants(matching: .button)
                .element(boundBy: index)
        }
    }

}

extension CheckoutPresetSelectorPage {

    class PresetDetailTextLabelPage: TextLabelPage {
        func tap() -> CheckoutShipmentPopupPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutShipmentPopupViewControllerAccessibility.root]
            return CheckoutShipmentPopupPage(element: elem)
        }
    }
}
