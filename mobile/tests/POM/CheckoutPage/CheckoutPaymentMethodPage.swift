import UIUtils
import XCTest

final class CheckoutPaymentMethodPage: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = CheckoutDeliveryCollectionViewCellAccessibility

    class ContinueButton: PageObject {
        func tap() -> CheckoutSummaryPage {
            element.tap()
            let el = XCUIApplication().otherElements[CheckoutSummaryAccessibility.root]
            return CheckoutSummaryPage(element: el)
        }
    }

    /// Ячейка с типом оплаты и radioButton
    class PaymentMethodCell: PageObject {
        var image: XCUIElement {
            element.images.firstMatch
        }

        var title: XCUIElement {
            element.descendants(matching: .staticText).firstMatch
        }
    }

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var continueButton: ContinueButton {
        let el = cellUniqueElement(withIdentifier: CheckoutPaymentMethodAccessibility.continueButton)
        return ContinueButton(element: el)
    }

    func paymentMethod(at index: Int) -> PaymentMethodCell {
        let element = cellUniqueElement(withIdentifier: CheckoutPaymentMethodAccessibility.paymentOption, index: index)
        return PaymentMethodCell(element: element)
    }
}
