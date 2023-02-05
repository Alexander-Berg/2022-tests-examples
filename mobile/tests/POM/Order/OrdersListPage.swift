import XCTest

final class OrdersListPage: PageObject {

    // MARK: - Public

    static var current: OrdersListPage {
        let element = XCUIApplication().any.matching(identifier: OrdersListAccessibility.root).firstMatch
        return .init(element: element)
    }

    var lockCode: XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.lockCode).textViews.firstMatch
    }

    func editRequests(orderId: String) -> [NoticeInfoPage] {
        let elements = allCellUniqueElement(withIdentifier: OrdersCommonAccessibility.editRequest(orderId: orderId))
        return elements.map(NoticeInfoPage.init(element:))
    }

    func serviceInfo(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.serviceInfo(orderId: orderId))
        return el.staticTexts.firstMatch
    }

    func status(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.status(orderId: orderId))
        return el.staticTexts.firstMatch
    }

    func substatus(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.substatus(orderId: orderId))
        return el.staticTexts.firstMatch
    }

    func total(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.total(orderId: orderId))
        return el.staticTexts.firstMatch
    }

    func storagePeriod(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.storagePeriod(orderId: orderId))
        return el.staticTexts.firstMatch
    }

    func stillNoButton(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.stillNoButton(orderId: orderId))
        return el.buttons.firstMatch
    }

    func alreadyGotItButton(orderId: String) -> XCUIElement {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.alreadyGotItButton(orderId: orderId))
        return el.buttons.firstMatch
    }

    func detailsButton(orderId: String) -> DetailsButton {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.detailsButton(orderId: orderId))
        return DetailsButton(element: el)
    }

    func showOnMapButton(orderId: String) -> ShowOnMapButton {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.showOnMapButton(orderId: orderId))
        return ShowOnMapButton(element: el)
    }

    func payButton(orderId: String) -> PayButton {
        let el = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.payButton(orderId: orderId))
        return PayButton(element: el)
    }

    func consultationButton(orderId: String) -> XCUIElement {
        let e1 = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.consultationDSBSButton)
        return e1
    }

    func callCourierButton(orderId: String) -> XCUIElement {
        let e1 = cellUniqueElement(withIdentifier: OrdersCommonAccessibility.callToActionButton(orderId: orderId))
            .buttons
            .firstMatch
        return e1
    }

    func consultationFbsOrExpressButton(orderId: String) -> XCUIElement {
        let e1 = cellUniqueElement(withIdentifier: OrderDetailsAccessibility.consultationFbsOrExpressButton)
        return e1
    }

    func rate() {
        element.buttons.matching(identifier: OrdersCommonAccessibility.rateMeButton).firstMatch.tap()
    }

    func showBarcodeButton(orderId: String) -> XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.barcodeButton(orderId: orderId))
    }

    func howOnDemandWorksButton(orderId: String) -> XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.howOnDemandWorksButton(orderId: orderId))
    }

    func repeatOrderButton(orderId: String) -> XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.repeatOrderButton(orderId: orderId))
    }

    func cancellationButton(orderId: String) -> XCUIElement {
        cellUniqueElement(withIdentifier: OrdersCommonAccessibility.cancellationButton(orderId: orderId)).buttons
            .firstMatch
    }

    func orderItemTitleLabel(itemId: String) -> XCUIElement {
        element.staticTexts.matching(identifier: OrdersCommonAccessibility.orderItemTitle(orderItemId: itemId))
            .firstMatch
    }

    func orderItemPriceLabel(itemId: String) -> XCUIElement {
        element.staticTexts.matching(identifier: OrdersCommonAccessibility.orderItemPrice(orderItemId: itemId))
            .firstMatch
    }

    func orderItemCountLabel(itemId: String) -> XCUIElement {
        element.staticTexts.matching(identifier: OrdersCommonAccessibility.orderItemCount(orderItemId: itemId))
            .firstMatch
    }

    func cartButton(itemId: String) -> XCUIElement {
        element.buttons.matching(identifier: OrdersCommonAccessibility.cartButton(orderItemId: itemId))
            .element
    }
}

// MARK: - CollectionViewPage

extension OrdersListPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OrdersListCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - Nested types

extension OrdersListPage {

    final class DetailsButton: PageObject, OrderDetailsEntryPoint {}
    final class PayButton: PageObject, OrderEditPaymentEntryPoint {}
    final class ShowOnMapButton: PageObject, OutletOnMapEntryPoint {}

}
