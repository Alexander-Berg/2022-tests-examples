import FormKit
import UIUtils
import XCTest

final class OrderEditPaymentPage: PageObject {

    // MARK: - Properties

    var continueButton: XCUIElement {
        cellUniqueElement(withIdentifier: OrderEditPaymentAccessibility.continueButton)
    }

    // MARK: - Public

    func paymentMethod(withIdentifier identifier: String) -> RadioButtonPage {
        let element = cellUniqueElement(withIdentifier: identifier)
        return RadioButtonPage(element: element)
    }

    func selectPaymentMethod(withIdentifier identifier: String) {
        paymentMethod(withIdentifier: identifier).element.tap()
    }
}

// MARK: - CollectionViewPage

extension OrderEditPaymentPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OrderEditPaymentCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
