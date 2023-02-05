import FormKit
import XCTest

class FinishMultiorderPage: PageObject {

    var pushNotificationImage: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.pushNotificationImage)
            .images.firstMatch
    }

    var pushNotificationInfo: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.pushNotificationInfo)
            .textViews.firstMatch
    }

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.title)
            .textViews.firstMatch
    }

    var warning: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.warning)
            .textViews.firstMatch
    }

    var pushNotificationSwitch: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.pushNotificationSwitch)
            .switches.firstMatch
    }

    func paymentStatus(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.paymentStatus, index: index)
            .textViews.firstMatch
    }

    func deliveryStatus(at index: Int = 0) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.deliveryStatus, index: index)
            .textViews.firstMatch
    }

    var plusBadge: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.plusBadge)
            .buttons.firstMatch
    }

    var plusBadgeText: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.plusBadgeText)
            .textViews.firstMatch
    }

    var plusBadgeLink: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.plusBadgeLink)
            .textViews.firstMatch
    }

    var detailSection: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.detailSection)
            .otherElements.firstMatch
    }

    var titleCashbackSummary: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.titleCashbackSummary)
            .textViews.firstMatch
    }

    var cashbackSummary: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.cashbackSummary)
            .textViews.firstMatch
    }

    var referalButton: ReferralButton {
        let elem = cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.referralButton)
            .buttons.firstMatch

        return ReferralButton(element: elem)
    }

    var payButton: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.payButton)
            .buttons.firstMatch
    }

    var choosePaymentButton: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.choosePaymentButton)
            .buttons.firstMatch
    }

    func orderItem(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutShipmentContentGroupAccessibility.item, index: index)
    }

    func titleOfOrderItem(at index: Int) -> XCUIElement {
        orderItem(at: index).textViews
            .firstMatch
    }

    func orderDetailsExpander(at index: Int = 0) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.detailSection, index: index * 2)
    }

    func paymentMethodOfOrderItem(at index: Int = 0) -> TextLabelPage {
        let elem = cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.paymentMethod, index: index)
        return TextLabelPage(element: elem)
    }

    func howOnDemandWorksButton(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.howOnDemandWorksButton, index: index)
            .textViews.firstMatch
    }

    func receiptsButton(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.receiptsButton, index: index)
    }

    /// Services

    func serviceTitle(at index: Int = 0) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.Services.title, index: index)
            .textViews.firstMatch
    }

    func serviceDescription(at index: Int = 0) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.Services.description, index: index)
            .textViews.firstMatch
    }

    func serviceName(at index: Int = 0) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.Services.name, index: index)
            .textViews.firstMatch
    }

    func servicePaymentStatus(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.Services.payment, index: index)
            .textViews.firstMatch
    }

    /// Installments

    var installmentsTitle: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.Installments.title)
            .textViews.firstMatch
    }

    var installmentsTinkoffAppPaymentsTitle: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.Installments.tinkoffAppPayments)
            .textViews.firstMatch
    }

    var installmentsPaymentReminderTitle: XCUIElement {
        cellUniqueElement(withIdentifier: FinishMultiorderAccessibilty.Installments.paymentReminder)
            .textViews.firstMatch
    }

    /// Current element
    static var current: FinishMultiorderPage {
        let elem = XCUIApplication().otherElements[FinishMultiorderAccessibilty.root]
        return FinishMultiorderPage(element: elem)
    }
}

// MARK: - CollectionViewPage

extension FinishMultiorderPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = FinishMultiorderCollectionCellAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}
