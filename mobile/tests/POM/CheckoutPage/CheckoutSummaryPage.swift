import FormKit
import MarketUI
import UIUtils
import XCTest

final class CheckoutSummaryPage: PageObject, CollectionViewPage {
    typealias AccessibilityIdentifierProvider = CheckoutDeliveryCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    class OrderButton: PageObject, FinishMultiorderEntryPoint {}

    class SummaryCell: PageObject {
        var title: XCUIElement {
            element.staticTexts.matching(identifier: HorizontalTitleDetailsAccessibility.title).firstMatch
        }

        var price: XCUIElement {
            element.staticTexts.matching(identifier: HorizontalTitleDetailsAccessibility.details).firstMatch
        }
    }

    /// Ячейка с заказом в составе заказа
    class OrderCell: PageObject {
        var image: XCUIElement {
            element.images.firstMatch
        }

        var title: XCUIElement {
            element.staticTexts.matching(identifier: CheckoutSummaryAccessibility.orderTitle).firstMatch
        }

        var price: XCUIElement {
            element.textViews.matching(identifier: CheckoutSummaryAccessibility.orderPrice).firstMatch
        }

        var amount: XCUIElement {
            element.staticTexts.matching(identifier: CheckoutSummaryAccessibility.orderAmount).firstMatch
        }
    }

    var topOrderButton: OrderButton {
        let el = cellUniqueElement(withIdentifier: CheckoutSummaryAccessibility.topOrderButton)
        return OrderButton(element: el)
    }

    /// Товары (1) --- 330 ₽
    var goodSumInSummaryBlock: SummaryCell {
        let el = cellElement(at: IndexPath(item: 0, section: 2))
        return SummaryCell(element: el)
    }

    /// Доставка (1) --- 99 ₽
    var deliverySumInSummaryBlock: SummaryCell {
        let el = cellElement(at: IndexPath(item: 1, section: 2))
        return SummaryCell(element: el)
    }

    /// Итого --- 429 ₽
    var totalSumInSummaryBlock: SummaryCell {
        let el = cellElement(at: IndexPath(item: 2, section: 2))
        return SummaryCell(element: el)
    }

    /// Метод для получения ячейки заказа
    func getOrder(item: Int) -> OrderCell {
        let el = cellElement(at: IndexPath(item: item + 2, section: 5))
        return OrderCell(element: el)
    }

    /// Адрес получателя, текст
    var recipientAddress: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutSummaryAccessibility.recipientAddressValue)
    }

    var deliveryChangeControl: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutSummaryAccessibility.deliveryChangeControl)
    }
}
