import FormKit
import MarketMapSearchFeature
import MarketOrderEditMapFeature
import UIUtils
import XCTest

final class OrderEditAddressMapPage: PageObject {

    static var current: OrderEditAddressMapPage {
        let elem = XCUIApplication().otherElements[OrderEditAccessibility.root]
        return OrderEditAddressMapPage(element: elem)
    }

    var search: SearchPage {
        let elem = XCUIApplication().otherElements[MarketMapSearchAccessibility.SearchView.root]
        return SearchPage(element: elem)
    }

    var summary: SummaryPage {
        let elem = XCUIApplication().otherElements[OrderEditAddressMapAccessibility.SummaryView.root]
        return SummaryPage(element: elem)
    }

    var segmentedButtons: SegmentedButtonPage {
        let elem = XCUIApplication().otherElements[SegmentedButtonViewAccessibility.root]
        return SegmentedButtonPage(element: elem)
    }

    var hintView: XCUIElement {
        XCUIApplication()
            .otherElements[OrderEditAddressMapAccessibility.hintView]
            .staticTexts
            .firstMatch
    }
}

// MARK: - Nested types

extension OrderEditAddressMapPage {

    final class SummaryPage: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = OrderEditAddressMapCollectionViewCellAccessibility
        typealias BaseAccessibility = OrderEditAddressMapAccessibility.SummaryView

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
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
    }

    final class SearchPage: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = MarketMapSearchCellsAccessibility
        typealias BaseAccessibility = MarketMapSearchAccessibility.SearchView

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
    }

    final class SegmentedButtonPage: PageObject {

        var outletButton: XCUIElement {
            element
                .buttons
                .element(boundBy: 0)
                .firstMatch
        }

        var serviceButton: XCUIElement {
            element
                .buttons
                .element(boundBy: 1)
                .firstMatch
        }

        var count: Int {
            element.buttons.count
        }
    }
}
