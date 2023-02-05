import FormKit
import UIUtils
import XCTest

final class CheckoutMapViewPage: PageObject {

    static var current: CheckoutMapViewPage {
        let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.root]
        return CheckoutMapViewPage(element: elem)
    }

    var selectedChipTitle: String {
        element
            .buttons
            .matching(identifier: SegmentedButtonViewAccessibility.selectedButton)
            .firstMatch
            .label
    }

    var pinTitle: XCUIElement {
        element
            .otherElements
            .matching(identifier: CheckoutMapViewControllerAccessibility.pinView)
            .descendants(matching: .staticText)
            .element
    }

    var summary: SummaryPage {
        let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.SummaryView.root]
        return SummaryPage(element: elem)
    }

    var search: SearchPage {
        let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.SearchView.root]
        return SearchPage(element: elem)
    }

    func selectChip(at index: Int) {
        element
            .otherElements
            .matching(identifier: SegmentedButtonViewAccessibility.root)
            .descendants(matching: .button)
            .element(boundBy: index)
            .tap()
    }

    var toCurrentLocationButton: XCUIElement {
        XCUIApplication().buttons.matching(identifier: CheckoutMapViewControllerAccessibility.toCurrentLocationButton)
            .element
    }

    func tapToCurrentLocation() {
        toCurrentLocationButton.tap()
    }
}

// MARK: - Nested types

extension CheckoutMapViewPage {

    final class SummaryPage: PageObject, CollectionViewPage, EditAddressEntryPoint {

        typealias AccessibilityIdentifierProvider = CheckoutMapSummaryCollectionViewCellsAccessibility
        typealias BaseAccessibility = CheckoutMapViewControllerAccessibility.SummaryView

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        var captionText: XCUIElement {
            cellUniqueElement(withIdentifier: BaseAccessibility.captionCell)
                .textViews.firstMatch
        }

        var messageText: XCUIElement {
            cellUniqueElement(withIdentifier: BaseAccessibility.textFieldMessageCell)
                .textViews.firstMatch
        }

        var addressTextField: XCUIElement {
            cellUniqueElement(withIdentifier: BaseAccessibility.addressTextFieldCell)
                .textFields.firstMatch
        }

        var doneButton: XCUIElement {
            cellUniqueElement(withIdentifier: BaseAccessibility.doneButtonCell)
                .buttons.firstMatch
        }

        // MARK: - EditAddressEntryPoint

        @discardableResult
        func tap() -> EditAddressPage {
            doneButton.tap()
            let editAddressElem = XCUIApplication().otherElements[EditAddressAccessibility.root]
            XCTAssertTrue(editAddressElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
            return EditAddressPage(element: editAddressElem)
        }
    }

    final class SearchPage: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = CheckoutMapSearchCollectionViewCellsAccessibility
        typealias BaseAccessibility = CheckoutMapViewControllerAccessibility.SearchView

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        var addressField: SimpleTextFieldPage {
            let elem = cellUniqueElement(withIdentifier: BaseAccessibility.addressTextFieldCell)
            return SimpleTextFieldPage(element: elem)
        }

        func addressSuggestCell(at index: Int) -> XCUIElement {
            cellUniqueElement(
                withIdentifier: BaseAccessibility.suggestCell,
                index: index
            )
        }

        func addressSuggestTitle(at index: Int) -> XCUIElement {
            addressSuggestCell(at: index)
                .staticTexts
                .matching(identifier: PickupPointsMapAccessibility.suggestItemLabel)
                .firstMatch
        }
    }
}
