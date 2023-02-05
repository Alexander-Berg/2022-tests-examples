import FormKit
import MarketBNPL
import MarketCashback
import MarketUI
import UIUtils
import XCTest

final class CartPage: PageObject, UniformCollectionViewPage {
    typealias CellPage = CartCellPage

    typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

    typealias PriceDropWidget = ScrollBoxWidgetPage<PriceDropWidgetCellsAccessibility, SnippetPage>
    typealias HistoryWidget = ScrollBoxWidgetPage<HistoryWidgetCellsAccessibility, SnippetPage>

    class Threshold: PageObject {
        var deliveryText: XCUIElement {
            element
                .staticTexts
                .matching(identifier: CartAccessibility.Threshold.deliveryText)
                .firstMatch
        }

        var image: XCUIElement {
            element
                .images
                .matching(identifier: CartAccessibility.Threshold.image)
                .firstMatch
        }
    }

    class CartNotification: PageObject {

        class SelectButton: PageObject, PriceDropPopupEntryPoint {}

        var selectButton: SelectButton {
            let elem = element
                .buttons
                .matching(identifier: NoticeAccessibility.button)
                .firstMatch
            return SelectButton(element: elem)
        }

        var closeButton: XCUIElement {
            element
                .buttons
                .matching(identifier: NoticeAccessibility.closeButton)
                .firstMatch
        }

        var image: XCUIElement {
            element
                .images
                .matching(identifier: NoticeAccessibility.image)
                .firstMatch
        }

        var text: XCUIElement {
            element
                .staticTexts
                .matching(identifier: NoticeAccessibility.text)
                .firstMatch
        }
    }

    final class ServiceButton: PageObject {
        func tap() -> ServicesPopupPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CartServicesPopupAccessibility.root]
            return ServicesPopupPage(element: elem)
        }
    }

    class CartItem: PageObject, SKUEntryPoint {

        var inStockLabelShown: Bool {
            element
                .staticTexts["В наличии на складе"]
                .exists
        }

        var notInStockLabelShow: Bool {
            element
                .staticTexts["Нет в продаже"]
                .exists
        }

        func waitForNotInStockLabel() -> Bool {
            element
                .staticTexts["Нет в продаже"]
                .waitForExistence(timeout: XCTestCase.defaultTimeOut)
        }

        var price: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.price).element(boundBy: 0)
        }

        var oldPrice: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.oldPrice).element(boundBy: 0)
        }

        var priceDiscount: XCUIElement {
            element.staticTexts.matching(identifier: DiscountBadgeViewAccessibility.discount).firstMatch
        }

        var wishlistButton: XCUIElement {
            element.buttons.matching(identifier: CartAccessibility.CartItem.wishlistButton).element(boundBy: 0)
        }

        var title: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.title).element(boundBy: 0)
        }

        var additionalInfo: XCUIElement {
            element.any.matching(identifier: CartAccessibility.CartItem.additionalInfo).element(boundBy: 0)
        }

        var supplier: XCUIElement {
            element.any.matching(identifier: CartAccessibility.CartItem.supplier).element(boundBy: 0)
        }

        var disclaimer: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.disclaimer).element(boundBy: 0)
        }

        var removeButton: XCUIElement {
            element.buttons.matching(identifier: CartAccessibility.CartItem.removeButton).element(boundBy: 0)
        }

        var serviceButton: ServiceButton {
            let elem = element.buttons.matching(identifier: CartAccessibility.CartItem.serviceButton)
                .element(boundBy: 0)
            return ServiceButton(element: elem)
        }

        /// Количество товара
        var countInfo: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.countInfo).element(boundBy: 0)
        }

        var giftView: GiftViewPage {
            let page = element.otherElements
                .matching(identifier: GiftViewAccessibility.root)
                .firstMatch
            return GiftViewPage(element: page)
        }

        var giftCountInfo: XCUIElement {
            element.staticTexts
                .matching(identifier: CartAccessibility.CartItem.giftCountInfo)
                .firstMatch
        }

        var separator: XCUIElement {
            element.otherElements.matching(identifier: CartAccessibility.CartItem.separator).element(boundBy: 0)
        }

        var itemImage: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.image).element(boundBy: 0)
        }

        var countPicker: XCUIElement {
            element.otherElements.allElementsBoundByIndex.first {
                $0.identifier == CartAccessibility.CartItem.countPicker
            } ?? element.otherElements.matching(identifier: CartAccessibility.CartItem.countPicker).firstMatch
        }

        var cartButton: XCUIElement {
            element.buttons.matching(identifier: CartAccessibility.CartItem.cartButton).firstMatch
        }

        var cartButtonRedesign: CartButtonPage {
            let elem = element.buttons.matching(identifier: CartAccessibility.CartItem.cartButton)
                .firstMatch
            return CartButtonPage(element: elem)
        }

        var units: XCUIElement {
            element.otherElements
                .matching(identifier: CartAccessibility.CartItem.unitsFlowContainer)
                .staticTexts.element(boundBy: 0)
        }

        var cashback: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.cashback).firstMatch
        }

        var quantityDiscount: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CartItem.quantityDiscount).firstMatch
        }
    }

    class CartCellPage: PageObject, SKUEntryPoint {}

    class SummaryPrimaryCell: CartCellPage {
        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.title)
                .firstMatch
        }

        var details: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.details)
                .firstMatch
        }
    }

    class CashbackCellPage: SummaryPrimaryCell {
        func tap() -> CashbackAboutPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: CashbackAboutAccessibility.root)
                .firstMatch

            return CashbackAboutPage(element: elem)
        }
    }

    final class UnpaidOrder: PageObject {

        final class PayButton: PageObject {

            func tap() -> OrderEditPaymentPage {
                element.tap()

                let elem = XCUIApplication().otherElements[OrderEditPaymentAccessibility.root]
                return .init(element: elem)
            }
        }

        var payButton: PayButton {
            let elem = element
                .buttons
                .matching(identifier: CartAccessibility.UnpaidOrder.payButton)
                .firstMatch
            return .init(element: elem)
        }
    }

    var unpaidOrder: UnpaidOrder {
        let elem = element
            .otherElements
            .matching(identifier: CartAccessibility.UnpaidOrder.root)
            .firstMatch
        return .init(element: elem)
    }

    final class UnpaidOrderButton: PageObject, OrderEditPaymentEntryPoint {}

    var unpaidOrderButton: UnpaidOrderButton {
        let actionButtonElement = element
            .buttons
            .matching(identifier: HoveringSnippetAccessibility.actionButton)
            .firstMatch
        return UnpaidOrderButton(element: actionButtonElement)
    }

    class Coins: PageObject, CollectionViewPage {
        typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

        class Item: PageObject, CollectionViewPage {
            typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

            var collectionView: XCUIElement {
                element.collectionViews.firstMatch
            }

            var title: XCUIElement {
                element
                    .staticTexts
                    .matching(identifier: CartAccessibility.Coins.Item.title)
                    .firstMatch
            }

            var subtitle: XCUIElement {
                element
                    .staticTexts
                    .matching(identifier: CartAccessibility.Coins.Item.subtitle)
                    .firstMatch
            }

            var endDate: XCUIElement {
                element
                    .staticTexts
                    .matching(identifier: CartAccessibility.Coins.Item.endDate)
                    .firstMatch
            }

            var image: XCUIElement {
                element
                    .staticTexts
                    .matching(identifier: CartAccessibility.Coins.Item.image)
                    .firstMatch
            }

            var checkbox: XCUIElement {
                cellUniqueElement(withIdentifier: CartAccessibility.Coins.Item.checkbox)
                    .images
                    .firstMatch
            }

            var timer: XCUIElement {
                element
                    .staticTexts
                    .matching(identifier: CartAccessibility.Coins.Item.timer)
                    .firstMatch
            }
        }

        // Заголовок
        var title: XCUIElement {
            cellUniqueElement(withIdentifier: CartAccessibility.Coins.title)
        }

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        func coinsItem(at: Int) -> Item {
            let cellElement = element
                .otherElements
                .matching(identifier: CartAccessibility.Coins.Item.root)
                .element(boundBy: at)
            return Item(element: cellElement)
        }
    }

    class Promocode: PageObject {
        var input: XCUIElement {
            element.textFields.matching(identifier: CartAccessibility.Promocode.value).firstMatch
        }

        var applyButton: XCUIElement {
            element.buttons.matching(identifier: CartAccessibility.Promocode.applyButton).firstMatch
        }

        var error: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.Promocode.error).firstMatch
        }
    }

    class MakeOrderButton: PageObject, CheckoutEntryPoint {
        func tap() -> CheckoutMapViewPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutMapViewControllerAccessibility.root]
            return CheckoutMapViewPage(element: elem)
        }

        func tap() -> CheckoutDeliveryPage {
            element.tap()
            let el = XCUIApplication().otherElements[CheckoutDeliveryAccessibility.root]
            return CheckoutDeliveryPage(element: el)
        }
    }

    class AdvertisingCampaignThreshold: PageObject {
        var descriptionText: CashbackCellPage {
            CashbackCellPage(
                element: element
                    .staticTexts
                    .matching(identifier: CartThresholdAccessibility.descriptionText)
                    .firstMatch
            )
        }

        var threshold: XCUIElement {
            element
                .otherElements
                .matching(identifier: CartThresholdAccessibility.threshold)
                .firstMatch
        }
    }

    final class PlusSubscriptionCell: PageObject {

        var title: XCUIElement {
            element.staticTexts
                .firstMatch
        }
    }

    class Summary: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        /// Вес заказа
        var weight: SummaryPrimaryCell {
            let elem = cellUniqueElement(withIdentifier: CartAccessibility.Summary.weight)
            return SummaryPrimaryCell(element: elem)
        }

        /// Товары (1)
        var totalItems: SummaryPrimaryCell {
            let elem = cellUniqueElement(withIdentifier: CartAccessibility.Summary.totalItems)
            return SummaryPrimaryCell(element: elem)
        }

        /// Установки (1)
        var totalServices: SummaryPrimaryCell {
            let elem = cellUniqueElement(withIdentifier: CartAccessibility.Summary.totalServices)
            return SummaryPrimaryCell(element: elem)
        }

        /// Скидка на товары
        var discount: SummaryPrimaryCell {
            let elem = cellUniqueElement(withIdentifier: CartAccessibility.Summary.discount)
            return SummaryPrimaryCell(element: elem)
        }

        /// Скидка по промокоду
        var promocodeDiscount: SummaryPrimaryCell {
            let elem = cellUniqueElement(withIdentifier: CartAccessibility.Summary.promocodeDiscount)
            return SummaryPrimaryCell(element: elem)
        }

        /// Всего
        var totalPrice: SummaryPrimaryCell {
            let elem = cellUniqueElement(withIdentifier: CartAccessibility.Summary.totalPrice)
            return SummaryPrimaryCell(element: elem)
        }

        /// Всего кешбэка
        var totalCashback: SummaryPrimaryCell {
            let elem = cellUniqueElement(withIdentifier: CartAccessibility.Summary.cashback)
            return SummaryPrimaryCell(element: elem)
        }

        /// Блок с кредитом
        var creditInfo: CreditInfoCellPage {
            CreditInfoCellPage(
                element: cellUniqueElement(withIdentifier: CartAccessibility.Credit.creditInfo)
                    .otherElements
                    .element(boundBy: 1)
            )
        }

        /// Стоимость доставки
        var delivery: XCUIElement {
            element.otherElements.matching(identifier: CartAccessibility.Summary.delivery).firstMatch
        }

        /// Перейти к оформлению
        var orderButton: MakeOrderButton {
            let buttonElement = cellUniqueElement(withIdentifier: CartAccessibility.Summary.orderButton)
            return MakeOrderButton(element: buttonElement)
        }
    }

    class BNPL: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        /// Блок с BNPL
        var bnplPlanConstructor: BNPLPlanConstructorPage {
            let element = cellUniqueElement(withIdentifier: CartAccessibility.BNPL.root)
                .firstMatch
                .otherElements
                .matching(identifier: BNPLPlanConstructorAccessibility.root)
                .firstMatch
            return BNPLPlanConstructorPage(element: element)
        }
    }

    class Installments: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        /// Блок рассрочки
        var installmentsInfo: InstallmentsInfoCellPage {
            InstallmentsInfoCellPage(
                element: cellUniqueElement(withIdentifier: CartAccessibility.Installments.root)
                    .firstMatch
            )
        }
    }

    class InstallmentsInfoCellPage: PageObject {

        /// Лейбл описания рассрочки
        var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: CartAccessibility.Installments.title)
                .firstMatch
        }

        /// Селектор рассрочки
        var selector: InstallmentsSelectorPage {
            let elem = element
                .collectionViews
                .matching(identifier: CartAccessibility.Installments.selector)
                .firstMatch
            return InstallmentsSelectorPage(element: elem)
        }

        /// Лейбл ежемесячного платежа
        var monthlyPayment: XCUIElement {
            element
                .staticTexts
                .matching(identifier: CartAccessibility.Installments.monthlyPayment)
                .firstMatch
        }

        /// Кнопка Оформить
        var checkoutButton: CheckoutButton {
            CheckoutButton(
                element: element
                    .buttons
                    .matching(identifier: CartAccessibility.Installments.checkoutButton)
                    .firstMatch
            )
        }
    }

    class InstallmentsSelectorPage: PageObject, InstallmentsSelectorPopupEntryPoint {

        /// Выбранная ячейка на барабане
        var selectedCell: SelectedInstallmentCellPage {
            let elem = element
                .cells
                .matching(identifier: CartAccessibility.Installments.selectedCell)
                .firstMatch
            return SelectedInstallmentCellPage(element: elem)
        }
    }

    class SelectedInstallmentCellPage: PageObject {

        /// Лейбл выбранного срока рассрочки на барабане
        var term: XCUIElement {
            element.staticTexts.firstMatch
        }
    }

    class Credit: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        /// Блок с кредитом
        var creditInfo: CreditInfoCellPage {
            CreditInfoCellPage(
                element: cellUniqueElement(withIdentifier: CartAccessibility.Credit.creditInfo)
                    .otherElements
                    .element(boundBy: 1)
            )
        }
    }

    class CreditInfoCellPage: CartCellPage {

        var buyInCreditButton: CheckoutButton {
            CheckoutButton(
                element: element
                    .buttons
                    .matching(identifier: CartAccessibility.Summary.buyInCreditButton)
                    .firstMatch
            )
        }

        var monthlyPayment: XCUIElement {
            element
                .staticTexts
                .matching(identifier: CartAccessibility.Summary.monthlyPayment)
                .firstMatch
        }
    }

    /// Кнопка перехода в чекаут с финансовых виджетов
    class CheckoutButton: PageObject, CheckoutEntryPoint {

        func tap() -> CheckoutPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutAccessibility.root]
            return CheckoutPage(element: elem)
        }
    }

    class CompactSummary: PageObject {
        /// Всего
        var totalPrice: XCUIElement {
            element.staticTexts.matching(identifier: CartAccessibility.CompactSummary.totalPrice).firstMatch
        }

        /// Перейти к оформлению
        var orderButton: MakeOrderButton {
            let buttonElement = element
                .buttons
                .matching(identifier: CartAccessibility.CompactSummary.orderButton)
                .firstMatch
            return MakeOrderButton(element: buttonElement)
        }
    }

    // Заголовок посылки
    class BucketHeader: PageObject {
        var text: XCUIElement {
            element.staticTexts.firstMatch
        }
    }

    class YandexCard: PageObject, CollectionViewPage {
        typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

        var collectionView: XCUIElement {
            element
        }

        var cell: XCUIElement {
            cellUniqueElement(withIdentifier: CartAccessibility.Plus.yandexCard).firstMatch
        }
    }

    final class PaymentSystemCampaignCell: PageObject {

        var title: XCUIElement {
            element.textViews.firstMatch
        }
    }

    class UpsalePlus: PageObject, CollectionViewPage {
        typealias AccessibilityIdentifierProvider = CartCollectionViewCellsAccessibility

        var collectionView: XCUIElement {
            element
        }

        var cell: XCUIElement {
            cellUniqueElement(withIdentifier: CartAccessibility.Plus.upsalePlus).firstMatch
        }
    }

    class ScrollableHeader: PageObject {
        func button(at index: Int) -> XCUIElement {
            element
                .buttons
                .element(boundBy: index)
        }
    }

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var root: XCUIElement {
        element
            .descendants(matching: .any)
            .matching(identifier: CartAccessibility.root)
            .firstMatch
    }

    // Скроллер корзин
    var scrollableHeader: ScrollableHeader {
        let elem = element
            .descendants(matching: .any)
            .matching(identifier: CartAccessibility.scrollableHeader)
            .firstMatch
        return ScrollableHeader(element: elem)
    }

    // Трешхолд
    var threshold: Threshold {
        let elem = collectionView
            .descendants(matching: .any)
            .matching(identifier: CartAccessibility.Threshold.root)
            .firstMatch
        return Threshold(element: elem)
    }

    // ББ
    var coins: Coins {
        Coins(element: element)
    }

    // Все монетки
    var allCoins: [Coins.Item] {
        element
            .otherElements
            .matching(identifier: CartAccessibility.Coins.Item.root)
            .allElementsBoundByIndex
            .map { Coins.Item(element: $0) }
    }

    // Промокод
    var promocode: Promocode {
        Promocode(
            element: cellUniqueElement(withIdentifier: CartAccessibility.Promocode.root)
        )
    }

    // Трешхолд рекламной компании
    var advertisingCampaignThreshold: AdvertisingCampaignThreshold {
        let elem = collectionView
            .descendants(matching: .any)
            .matching(identifier: CartThresholdAccessibility.root)
            .firstMatch
        return AdvertisingCampaignThreshold(element: elem)
    }

    var plusSubscriptionCell: PlusSubscriptionCell {
        let elem = cellUniqueElement(withIdentifier: CartAccessibility.plusSubscription)
        return PlusSubscriptionCell(element: elem)
    }

    /// Блок саммари
    var summary: Summary {
        Summary(element: element)
    }

    /// Блок BNPL
    var bnpl: BNPL {
        BNPL(element: element)
    }

    /// Блок рассрочки
    var installments: Installments {
        Installments(element: element)
    }

    /// Блок кредитов
    var credit: Credit {
        Credit(element: element)
    }

    /// Залипающий блок саммари
    var compactSummary: CompactSummary {
        let elem = element
            .otherElements
            .matching(identifier: CartAccessibility.CompactSummary.root)
            .firstMatch
        return CompactSummary(element: elem)
    }

    /// Колесо пикера
    var pickerWheel: XCUIElement {
        XCUIApplication().pickerWheels.firstMatch
    }

    /// Кнопка "Готово" в пикере количества товаров
    var countPickerDoneButton: XCUIElement {
        XCUIApplication().buttons[CartAccessibility.countPickerDoneButton]
    }

    /// Виджет PriceDrop внизу cms секции
    var priceDropWidget: PriceDropWidget {
        let elem = element.collectionViews.firstMatch
        return PriceDropWidget(element: elem)
    }

    var historyWidget: HistoryWidget {
        let elem = element.collectionViews.firstMatch
        return HistoryWidget(element: elem)
    }

    var yandexCard: YandexCard {
        YandexCard(element: element.collectionViews.firstMatch)
    }

    var paymentSystemCampaignCell: PaymentSystemCampaignCell {
        let elem = cellUniqueElement(withIdentifier: CartAccessibility.Plus.paymentSystemCampaign)
        return PaymentSystemCampaignCell(element: elem)
    }

    var upsalePlus: UpsalePlus {
        UpsalePlus(element: element.collectionViews.firstMatch)
    }

    /// Товар в списке товаров в корзине
    func cartItem(at: Int) -> CartItem {
        let cellElem = element
            .otherElements
            .matching(identifier: CartAccessibility.CartItem.root)
            .element(boundBy: at)
        return CartItem(element: cellElem)
    }

    func cartItem(with index: Int) -> CartItem {
        let cellElem = cellUniqueElement(withIdentifier: CartAccessibility.root, index: index)
        return CartItem(element: cellElem)
    }

    /// Заголовок бизнес группы
    func businessGroupHeader(at index: Int) -> BucketHeader {
        let cellElem = cellUniqueElement(withIdentifier: CartAccessibility.CartBusinessGroup.header, index: index)
        return BucketHeader(element: cellElem)
    }

    func sectionHeaderTitle(at: Int) -> XCUIElement {
        element
            .otherElements
            .matching(identifier: WidgetsAccessibility.decoratedTitle)
            .element(boundBy: at)
    }

    func retailOrderButton(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: CartAccessibility.Retail.orderButton, index: index)
    }

}
