import UIUtils
import XCTest

class CheckoutPaymentMethodPopupPage: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = CheckoutPaymentMethodCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var currentPaymentDetails: String {
        allCellUniqueElement(withIdentifier: CheckoutPaymentMethodAccessibility.paymentDetails)
            .map { $0.staticTexts.firstMatch.label }.joined(separator: "\n")
    }

    var continueButton: XCUIElement {
        collectionView.otherElements
            .matching(identifier: CheckoutPaymentMethodAccessibility.continueButton)
            .buttons.firstMatch
    }

    func paymentMethod(at index: Int) -> RadioButtonPage {
        let element = cellElement(at: IndexPath(row: index * 2, section: 0))
        return RadioButtonPage(element: element)
    }

    func paymentMethod(with identifier: String) -> RadioButtonPage {
        let element = cellUniqueElement(withIdentifier: identifier)
        return RadioButtonPage(element: element)
    }

    func selectPaymentMethod(with identifier: String) {
        paymentMethod(with: identifier).element.tap()
    }
}
