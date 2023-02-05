import FormKit
import MarketUI
import UIUtils
import XCTest

/// PageObject Новых деталей заказа
final class OrderDetailsPage: PageObject {

    // MARK: - Public

    static var current: OrderEditAddressMapPage {
        let elem = XCUIApplication().otherElements[OrderDetailsAccessibility.root]
        return OrderEditAddressMapPage(element: elem)
    }

    class SummaryRowCell: PageObject {
        var title: XCUIElement {
            element.staticTexts.matching(identifier: HorizontalTitleDetailsAccessibility.title).firstMatch
        }

        var amount: XCUIElement {
            element.staticTexts.matching(identifier: HorizontalTitleDetailsAccessibility.details).firstMatch
        }
    }

    var editRequests: [NoticeInfoPage] {
        let elements = allCellUniqueElement(withIdentifier: OrdersCommonAccessibility.editRequest)
        return elements.map(NoticeInfoPage.init(element:))
    }

    var status: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.status)
        return el.staticTexts.firstMatch
    }

    var substatus: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.substatus)
        return el.staticTexts.firstMatch
    }

    var courierTrackingButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.courierTrackingButton)
        return el.buttons.firstMatch
    }

    var refundButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.refundButton)
        return el.buttons.firstMatch
    }

    func payButton(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.payButton(orderId: orderId))
        return el.buttons.firstMatch
    }

    var stillNoButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.stillNoButton)
        return el.buttons.firstMatch
    }

    var alreadyGotItButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.alreadyGotItButton)
        return el.buttons.firstMatch
    }

    var showOnMapButton: ShowOnMapButton {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.showOnMapButton)
        return ShowOnMapButton(element: el.buttons.firstMatch)
    }

    var deliveryServiceInfo: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.additionalInfo)
        return el.textViews.firstMatch
    }

    var creationDate: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.creationDate)
        return ConcreteInfoPage(element: el)
    }

    var paymentType: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.paymentType)
        return ConcreteInfoPage(element: el)
    }

    var deliveryDate: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.deliveryDate)
        return ConcreteInfoPage(element: el)
    }

    var lifting: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.lifting)
        return ConcreteInfoPage(element: el)
    }

    var storagePeriod: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.storagePeriod)
        return ConcreteInfoPage(element: el)
    }

    var deliveryAddress: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.deliveryAddress)
        return ConcreteInfoPage(element: el)
    }

    var seller: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.seller)
        return ConcreteInfoPage(element: el)
    }

    var recipient: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.recipient)
        return ConcreteInfoPage(element: el)
    }

    var buyer: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.buyer)
        return ConcreteInfoPage(element: el)
    }

    var shopOrderId: ConcreteInfoPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.shopOrderId)
        return ConcreteInfoPage(element: el)
    }

    var merchantButton: MerchantButtonPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.merchantButton)
        return MerchantButtonPage(element: el)
    }

    var receiptsButton: ReceiptsButtonPage {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.receiptsButton)
        return ReceiptsButtonPage(element: el)
    }

    var refundDisabledTitle: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.refundDisabledTitle)
        return el.textViews.firstMatch
    }

    var refundDisabledSubtitle: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.refundDisabledSubtitle)
        return el.textViews.firstMatch
    }

    var cashbackDetailedTitle: SummaryRowCell {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.cashback)
        return SummaryRowCell(element: el)
    }

    var liftingSummaryTitle: SummaryRowCell {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.liftingSummary)
        return SummaryRowCell(element: el)
    }

    var servicesSummaryTitle: SummaryRowCell {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.servicesSummary)
        return SummaryRowCell(element: el)
    }

    var itemsSummaryTitle: SummaryRowCell {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.itemsSummary)
        return SummaryRowCell(element: el)
    }

    var totalSummaryTitle: SummaryRowCell {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.totalSummary)
        return SummaryRowCell(element: el)
    }

    var contactSupportButton: ContactSupportButton {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.contactSupportButton)
        return ContactSupportButton(element: el.buttons.firstMatch)
    }

    var consultationButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.consultationDSBSButton)
        return el.buttons.firstMatch
    }

    var supportChatButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.supportChatButton)
        return el.buttons.firstMatch
    }

    var callCourierButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.callCourierButton).buttons.firstMatch
        return el
    }

    var cancellationButton: CancellationButton {
        let el = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.cancellationButton)
        return CancellationButton(element: el.buttons.firstMatch)
    }

    var rateButton: XCUIElement {
        element.buttons.matching(identifier: OrdersCommonAccessibility.rateMeButton).firstMatch
    }

    var changesMessage: XCUIElement {
        cellUniqueElement(withIdentifier: OrderDetailsAccessibility.changesMessage).textViews.firstMatch
    }

    var analogsButton: XCUIElement {
        element.buttons.matching(identifier: OrdersCommonAccessibility.analogsButton).firstMatch
    }

    var showBarcodeButton: XCUIElement {
        cellUniqueElement(withIdentifier: OrderDetailsAccessibility.barcodeButton)
    }

    var lockCode: XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.lockCode).textViews.firstMatch
    }

    func howOnDemandWorksButton(orderId: String) -> XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.howOnDemandWorksButton(orderId: orderId))
    }

    func repeatOrderButton(orderId: String) -> XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.repeatOrderButton(orderId: orderId))
    }

    func orderItemTitleLabel(itemId: String) -> XCUIElement {
        element.staticTexts.matching(
            identifier: OrdersCommonAccessibility.orderItemTitle(orderItemId: itemId)
        ).firstMatch
    }

    func orderItemPriceLabel(itemId: String) -> XCUIElement {
        element.staticTexts.matching(
            identifier: OrdersCommonAccessibility.orderItemPrice(orderItemId: itemId)
        ).firstMatch
    }

    func orderItemCountLabel(itemId: String) -> XCUIElement {
        element.staticTexts.matching(
            identifier: OrdersCommonAccessibility.orderItemCount(orderItemId: itemId)
        )
        .firstMatch
    }

    func cartButton(itemId: String) -> XCUIElement {
        element.buttons.matching(
            identifier: OrdersCommonAccessibility.cartButton(orderItemId: itemId)
        ).element
    }
}

// MARK: - CollectionViewPage

extension OrderDetailsPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = OrderDetailsCellAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}

// MARK: - Nested types

extension OrderDetailsPage {

    class ConcreteInfoPage: FieldViewPage {

        var editButton: EditButttonPage {
            EditButttonPage(element: button)
        }

        var extendButton: ExtendButtonPage {
            ExtendButtonPage(element: button)
        }
    }

    class ExtendButtonPage: PageObject, OrderEditEntryPoint, OrderEditFinishedEntryPoint {}

    class EditButttonPage: PageObject, OrderEditEntryPoint {}

    class MerchantButtonPage: PageObject, MerchantPopupEntryPoint {}

    class ReceiptsButtonPage: PageObject, OrderReceiptsEntryPoint {}

    class ContactSupportButton: PageObject, ContactSupportEntryPoint {}

    class ShowOnMapButton: PageObject, OutletMapInfoEntryPoint {}

    class CancellationButton: PageObject {
        func tap() -> CancelOrderPage {
            element.tap()
            let cancelOrderElement = XCUIApplication().otherElements[CancelOrderAccessibility.root]
            return CancelOrderPage(element: cancelOrderElement)
        }
    }

}
