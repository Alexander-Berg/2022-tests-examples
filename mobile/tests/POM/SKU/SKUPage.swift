import MarketBNPL
import MarketCashback
import MarketUI
import UIUtils
import XCTest

class SKUPage: PageObject {

    let stickyViewInset = UIEdgeInsets(top: 0, left: 0, bottom: 68, right: 0)

    static var current: SKUPage {
        let element = XCUIApplication().any.matching(identifier: SKUAccessibility.root).firstMatch
        return .init(element: element)
    }

    /// Скрыла ли КМ спиннер и отобразила ли оффер.
    var didFinishLoadingInfo: Bool {
        title.exists
    }

    /// Плашка 18+
    var adultContentView: AdultContentView {
        let elem = XCUIApplication().otherElements[SKUAdultContentViewAccessibility.root]
        return AdultContentView(element: elem)
    }

    /// Шапка
    var navigationBar: NavigationBar {
        NavigationBar(element: NavigationBarPage.current.element)
    }

    /// Галлерея фотографий
    var gallery: Gallery {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.gallery)
        return Gallery(element: elem)
    }

    /// Бейдж "Новинка"
    var newBadge: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.newBadge)
        return elem.images.firstMatch
    }

    /// Кнопка редиректа на выдачу вендора или БЗ
    var vendorLinkButton: VendorLinkButton {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.vendorLinkButton)
        return VendorLinkButton(element: elem.buttons.firstMatch)
    }

    /// Название товара
    var title: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.title)
        return elem.textViews.firstMatch
    }

    /// Наличие на складе
    var stock: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.stock)
        return elem.textViews.firstMatch
    }

    /// 95% Рекомендуют
    var customerChoiseReason: CustomerChoiseReason {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.customerChoiseReason)
        return CustomerChoiseReason(element: elem)
    }

    /// Рекоммендация к покупке
    var compactReason: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.compactReason)
        return elem.textViews.firstMatch
    }

    /// Отображение рейтинга и количества отзывов
    var opinionsFastLink: OpinionsFastLink {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.opinionsFastLink)
        return OpinionsFastLink(element: elem)
    }

    /// Селектор модификаций
    var filter: Filter {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.filter)
        return Filter(element: elem)
    }

    /// Кнопка "Таблица размеров"
    var filterSizeTableButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.filterSizeTableButton)
        return elem.buttons.firstMatch
    }

    /// Цена товара
    var price: PricePage {
        let cell = cellUniqueElement(withIdentifier: SKUAccessibility.OfferPriceSection.priceCell)
        let container = cell.otherElements.matching(identifier: PriceAccessibility.container).firstMatch
        return PricePage(element: container)
    }

    /// Цена товара экспресс оффера
    var expressPrice: PricePage {
        let cell = cellUniqueElement(withIdentifier: SKUAccessibility.ExpressOfferPriceSection.priceCell)
        let container = cell.otherElements.matching(identifier: PriceAccessibility.container).firstMatch
        return PricePage(element: container)
    }

    var unitCalc: XCUIElement {
        let cell = cellUniqueElement(withIdentifier: SKUAccessibility.unitCalculatorSection)
        return cell.textViews.matching(identifier: SKUAccessibility.unitCalculatorText).firstMatch
    }

    /// Скидка за количество
    var quantityDiscount: XCUIElement {
        element.staticTexts.matching(identifier: SKUAccessibility.quantityDiscountTreshold).firstMatch
    }

    /// Ячейка кешбэка
    var cashback: CashbackItem {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.cashback)
        return CashbackItem(element: elem)
    }

    /// Виджет информации о BNPL (Сплит)
    var bnplPlanConstructor: BNPLPlanConstructorPage {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.bnplInfo)
            .firstMatch
            .otherElements
            .matching(identifier: BNPLPlanConstructorAccessibility.root)
            .firstMatch
        return BNPLPlanConstructorPage(element: elem)
    }

    /// Виджет информации о рассрочке Тинькофф
    var installmentsInfo: InstallmentsInfo {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.installmentsInfo)
        return InstallmentsInfo(element: elem)
    }

    /// Виджет информации о кредиту Тинькофф
    var creditInfo: CreditInfo {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.creditInfo)
        return CreditInfo(element: elem)
    }

    /// Пикер подарка
    var giftPicker: GiftPickerViewPage {
        let item = XCUIApplication().otherElements[GiftPickerViewAccessibility.root]
        return GiftPickerViewPage(element: item)
    }

    var deliveryPartner: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryPartnerTypeSeller)
        return elem.textViews.firstMatch
    }

    /// Кнопка "Добавить в корзину" и "В корзину" на одноклике
    var addToCartButton: CartButtonPage {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.OfferPriceSection.addToCartButton)
        return CartButtonPage(element: elem.buttons.firstMatch)
    }

    /// Кнопка "Добавить установку"
    var addServiceButton: AddServiceButton {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.addServiceButton)
        return AddServiceButton(element: elem.staticTexts.firstMatch)
    }

    /// Бейдж 2=3 для товаров по акции
    var cheapestAsGiftView: CheapestAsGiftViewPage {
        let elem = XCUIApplication().otherElements[CheapestAsGiftAccessibility.root]
        return CheapestAsGiftViewPage(element: elem)
    }

    /// Хедер секции 3 = 2
    var cheapestAsGiftHeader: XCUIElement {
        cellUniqueElement(withIdentifier: SKUCheapestAsGiftAccessibility.header)
    }

    /// Хедер секции комплекта
    var setSectionHeader: XCUIElement {
        cellUniqueElement(withIdentifier: SKUSetAccessibility.header)
    }

    /// Кнопка "Все комплекты по акции"
    var setShowAllProductsButton: XCUIElement {
        cellUniqueElement(withIdentifier: SKUSetAccessibility.allProductsButton)
            .buttons
            .firstMatch
    }

    /// Ячейка продукта в секции кмплекта
    var setItemView: SetItemView {
        SetItemView(
            element: cellUniqueElement(withIdentifier: SKUSetAccessibility.items)
                .otherElements
                .matching(identifier: SKUSetAccessibility.itemView)
                .firstMatch
        )
    }

    /// Первый продукт из карусели 3=2
    var cheapestAsGiftProduct: CheapestAsGiftCarouselCell {
        let elem = cellUniqueElement(withIdentifier: SKUCheapestAsGiftAccessibility.scrollBox).collectionViews
            .firstMatch.cells
            .firstMatch
        return CheapestAsGiftCarouselCell(element: elem)
    }

    /// Кнопка "Все товары по акции для 3=2"
    var cheapestAsGiftShowAllProductsButton: XCUIElement {
        cellUniqueElement(withIdentifier: SKUCheapestAsGiftAccessibility.allProductsButton)
    }

    /// Кнопка "Ваш регион: Москва"
    var regionButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.regionButton)
        return elem.buttons.firstMatch
    }

    /// Набор опций доставки
    var deliveryOptions: DeliveryOptions {
        DeliveryOptions(element: collectionView)
    }

    /// Блок "С Яндекс Плюс доставка бесплатная"
    var freeDeliveryByYandexPlus: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.freeDeliveryByYandexPlus)
        return elem
            .textViews
            .firstMatch
    }

    var supplierInfo: SupplierInfo {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.supplierInfo)
        return SupplierInfo(element: elem)
    }

    /// Мультиоффер
    var alternativeOffers: AlternativeOffers {
        AlternativeOffers(element: collectionView)
    }

    /// Кнопка "Добавить в корзину" для комплекта
    var setCartButton: CartButtonPage {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.setCartButton)
        return CartButtonPage(element: elem.buttons.firstMatch)
    }

    /// Заголовок карусели аналогов
    var analogsTitle: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.analogsTitle)
        return elem.staticTexts.firstMatch
    }

    /// Карусель аналогов
    var analogs: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.analogs)
        return elem
    }

    /// Заголовок карусели аксеуссуаров
    var accessoriesTitle: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.accessoriesTitle)
        return elem.staticTexts.firstMatch
    }

    /// Карусель аксеуссуаров
    var accessories: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.accessories)
        return elem
    }

    /// Табы секции аналогов медицины
    var medicineAnalogsTabs: MedicineAnalogsTabsPage {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.medicineAnalogsTabs)
        return MedicineAnalogsTabsPage(element: elem)
    }

    /// Карусель секции аналогов медицины
    var medicineAnalogsOffers: MedicineAnalogsOffersPage {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.medicineAnalogsOffers)
        return MedicineAnalogsOffersPage(element: elem)
    }

    /// Хедер характеристик
    var specsHeader: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.specsHeader)
        return elem.textViews.firstMatch
    }

    /// Ячейки характеристик
    func spec(after elements: [String] = []) -> CollectionViewSearchResult<XCUIElement> {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.spec, after: elements)
        return CollectionViewSearchResult(cell: elem, target: elem)
    }

    /// Все ячейки характеристик
    var specCells: [SpecKeyValue] {
        allCellUniqueElement(withIdentifier: SKUAccessibility.spec).compactMap { SpecKeyValue(element: $0) }
    }

    /// Лейбл ячейки с характеристикой, например "Тип препарата"
    func specLabel(withName name: String) -> XCUIElement? {
        specCells.compactMap { $0.specCell(withText: name) }.first
    }

    /// Кнопка "Подробнее" в характеристиках
    var specsDetailsButton: ShowAllSpecsButton {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.specsDetailsButton)
        return ShowAllSpecsButton(element: elem.buttons.firstMatch)
    }

    /// Хедер инструкций
    var instructionHeader: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.instructionsHeader)
        return elem.textViews.firstMatch
    }

    /// Кнопка "Полные инструкции" в инструкциях
    var instructionDetailsButton: ShowAllInstructionButton {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.instructionsDetailsButton)
        return ShowAllInstructionButton(element: elem.buttons.firstMatch)
    }

    /// Хедер описания
    var descriptionHeader: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.descriptionHeader)
        return elem.textViews.firstMatch
    }

    /// Ячейка описания
    var description: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.description)
        return elem.textViews.firstMatch
    }

    /// Кнопка "Подробнее" в описании
    var descriptionDetailsButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.descriptionDetailsButton)
        return elem.buttons.firstMatch
    }

    /// Общие дисклеймеры
    func warning(after elements: [String] = []) -> CollectionViewSearchResult<XCUIElement> {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.warning, after: elements)
        return CollectionViewSearchResult(cell: elem, target: elem.textViews.firstMatch)
    }

    /// Хедер отзывов
    var opinionsHeader: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.opinionsHeader)
        return elem.textViews.firstMatch
    }

    /// Рейтинг
    var rating: Rating {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.rating)
        return Rating(element: elem)
    }

    /// Лейбл "Нет отзывов"
    var noOpinions: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.noOpinions)
        return elem.textViews.firstMatch
    }

    /// 85% покупателей рекомендуют этот товар
    var recommendedRatio: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.recommendedRatio)
        return elem.textViews.firstMatch
    }

    /// Названия факторов отзывов
    func factTitle(after elements: [String]) -> CollectionViewSearchResult<XCUIElement> {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.factTitle, after: elements)
        return CollectionViewSearchResult(cell: elem, target: elem.textViews.firstMatch)
    }

    /// Оценки факторов отзывов
    func factGrade(after elements: [String]) -> CollectionViewSearchResult<XCUIElement> {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.factGrade, after: elements)
        return CollectionViewSearchResult(cell: elem, target: elem.textViews.firstMatch)
    }

    /// Прогрессы факторов отзывов
    func factProgresses(after elements: [String]) -> CollectionViewSearchResult<XCUIElement> {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.factProgress, after: elements)
        return CollectionViewSearchResult(cell: elem, target: elem.otherElements.firstMatch)
    }

    /// Кнопка "Написать отзыв"
    var leaveOpinionButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.leaveOpinionButton)
        return elem.buttons.firstMatch
    }

    /// Кнопка показа комментария к отзыву
    var showOpinonButton: XCUIElement {
        cellUniqueElement(withIdentifier: SKUAccessibility.showOpinonButton)
            .buttons
            .firstMatch
    }

    /// Ячейки с текстами отзывов
    func opinion(after elements: [String] = []) -> CollectionViewSearchResult<XCUIElement> {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.opinion, after: elements)
        return CollectionViewSearchResult(cell: elem, target: elem.textViews.firstMatch)
    }

    /// Ячейки с текстами отзывов
    func opinionReplyButton(after elements: [String] = []) -> CollectionViewSearchResult<ReplyButton> {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.opinionReplyButton, after: elements)
        return CollectionViewSearchResult(cell: elem, target: ReplyButton(element: elem.textViews.firstMatch))
    }

    /// Кнопка открытия контекстного меню
    var openContextMenuButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.opinionAuthor)
        return elem.buttons.firstMatch
    }

    /// Кнопка "Смотреть все отзывы" в отзывах
    var seeAllOpinionsButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.opinionsSeeAllButton)
        return elem.buttons.firstMatch
    }

    /// Кол-во вопросов под галереей
    var qnaFastLink: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.qnaFastLink)
        return elem.textViews.firstMatch
    }

    /// Хедер вопросов и ответов
    var qnaHeader: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.qnaHeader)
        return elem.textViews.firstMatch
    }

    /// Кнопка "Задать вопрос"
    var qnaAskButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.qnaAskButton)
        return elem.buttons.firstMatch
    }

    /// Кнопка "Перейти к вопросам"
    var qnaSeeAllButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.qnaSeeAllButton)
        return elem.buttons.firstMatch
    }

    /// Кнопка "Ответить" для вопроса
    var qnaAddAnswer: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.qnaAddAnswerButton)
        return elem.textViews.firstMatch
    }

    /// Кнопка "Комментировать" для вопроса
    var qnaAddComment: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.qnaAddCommentButton)
        return elem.textViews.firstMatch
    }

    /// "Другие товары от производителя"
    var vendorLink: Link {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.vendorLink)
        return Link(element: elem.textViews.firstMatch)
    }

    /// "Магазин Apple"
    var vendorBrandzoneLink: Link {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.vendorBrandzoneLink)
        return Link(element: elem.textViews.firstMatch)
    }

    /// "Другие товары категории"
    var categoryLink: Link {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.categoryLink)
        return Link(element: elem.textViews.firstMatch)
    }

    /// Общий для всех карточек дисклеймер
    var disclaimer: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.disclaimer)
        return elem.textViews.firstMatch
    }

    /// Кнопка "Добавить в корзину" и "В корзину" на одноклике
    var promocode: PromocodePage {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.promocodeView)
        return PromocodePage(element: elem.staticTexts.firstMatch)
    }

    /// ML отзывы
    var reviewSummaryPros: XCUIElement {
        cellUniqueElement(withIdentifier: SKUAccessibility.reviewSummaryPros)
            .textViews
            .firstMatch
    }

    var reviewSummaryContra: XCUIElement {
        cellUniqueElement(withIdentifier: SKUAccessibility.reviewSummaryContra)
            .textViews
            .firstMatch
    }

    // Ячейка подарка в ДО
    var giftView: GiftPage {
        GiftPage(element: cellUniqueElement(withIdentifier: SKUGiftAccessibility.root))
    }

    /// Кнопка "Оформить" для подписки на Я.Станцию
    var stationSubscriptionCheckoutButton: CheckoutButton {
        CheckoutButton(
            element: cellUniqueElement(withIdentifier: SKUAccessibility.stationSubscriptionCheckoutButton)
                .buttons
                .firstMatch
        )
    }
}

// MARK: - CollectionViewPage

extension SKUPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = SKUCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - Nested Types

extension SKUPage {

    class CheckoutButton: PageObject, CheckoutEntryPoint {

        func tap() -> CheckoutPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutAccessibility.root]
            return CheckoutPage(element: elem)
        }
    }

    class AdultContentView: PageObject {
        /// Кнопка подтверждения возраста
        var confirmButton: XCUIElement {
            element.buttons.matching(identifier: SKUAdultContentViewAccessibility.confirmButton).firstMatch
        }

        /// Кнопка отмены
        var cancelButton: XCUIElement {
            element.buttons.matching(identifier: SKUAdultContentViewAccessibility.cancelButton).firstMatch
        }
    }

    class NavigationBar: NavigationBarPage {
        override var title: XCUIElement {
            element
                .staticTexts.matching(identifier: SKUAccessibility.barTitle)
                .firstMatch
        }

        var shareButton: XCUIElement {
            element
                .buttons.matching(identifier: SKUAccessibility.barShareButton)
                .firstMatch
        }

        var wishlistButton: XCUIElement {
            element
                .buttons.matching(identifier: SKUAccessibility.barWishlistButton)
                .firstMatch
        }

        var comparisonButton: XCUIElement {
            element
                .buttons.matching(identifier: ComparisonButtonAccessibility.button)
                .firstMatch
        }
    }

    /// Попап фотографии на весь экран
    class OpenedGallery: PageObject {
        var image: XCUIElement {
            element.images.firstMatch
        }

        var closeButton: XCUIElement {
            element
                .buttons
                .matching(identifier: GalleryCollectionViewCellAccessibility.galleryCloseButton)
                .firstMatch
        }

        var pageControl: XCUIElement {
            element
                .any
                .matching(identifier: GalleryCollectionViewCellAccessibility.pageIndicatorInOpenGallery)
                .firstMatch
        }
    }

    /// Карусель с фото товара.
    class Gallery: PageObject, UniformCollectionViewPage {

        typealias AccessibilityIdentifierProvider = GalleryCollectionViewCellAccessibility
        typealias CellPage = PhotoCell

        class PhotoCell: PageObject {
            func tap() -> OpenedGallery {
                element.tap()
                let gallery = XCUIApplication().otherElements[GalleryCollectionViewCellAccessibility.openGallery]
                XCTAssertTrue(gallery.waitForExistence(timeout: XCTestCase.defaultTimeOut))
                return OpenedGallery(element: gallery)
            }

            var giftView: GiftViewPage {
                let page = element.otherElements
                    .matching(identifier: GiftViewAccessibility.root)
                    .firstMatch
                return GiftViewPage(element: page)
            }
        }

        var collectionView: XCUIElement {
            element.collectionViews.firstMatch
        }

        var pageControl: XCUIElement {
            element
                .otherElements
                .matching(identifier: SKUAccessibility.galleryPageControl)
                .firstMatch
        }

        var similarGoodsButton: XCUIElement {
            element
                .buttons
                .matching(identifier: SKUAccessibility.gallerySimilarGoodsButton)
                .firstMatch
        }
    }

    class InstallmentsInfo: PageObject {

        /// Лейбл описания рассрочки
        var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: SKUAccessibility.installmentsTitle)
                .firstMatch
        }

        /// Селектор рассрочки
        var selector: InstallmentsSelectorPage {
            let elem = element
                .collectionViews
                .matching(identifier: SKUAccessibility.installmentsSelector)
                .firstMatch
            return InstallmentsSelectorPage(element: elem)
        }

        /// Лейбл ежемесячного платежа
        var monthlyPayment: XCUIElement {
            element
                .staticTexts
                .matching(identifier: SKUAccessibility.installmentsMonthlyPayment)
                .firstMatch
        }

        /// Кнопка Оформить
        var checkoutButton: CheckoutButton {
            CheckoutButton(
                element: element
                    .buttons
                    .matching(identifier: SKUAccessibility.installmentsCheckoutButton)
                    .firstMatch
            )
        }
    }

    class InstallmentsSelectorPage: PageObject, InstallmentsSelectorPopupEntryPoint {

        /// Выбранная ячейка на барабане
        var selectedCell: SelectedInstallmentCellPage {
            let elem = element
                .cells
                .matching(identifier: SKUAccessibility.installmentsSelectedCell)
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

    class CreditInfo: PageObject {

        /// Лейбл с ежемесячным платежом
        var monthlyPayment: XCUIElement {
            element
                .staticTexts
                .matching(identifier: SKUAccessibility.creditMonthlyPayment)
                .firstMatch
        }

        /// Лейбл описания кредита
        var creditDisclaimer: XCUIElement {
            element
                .staticTexts
                .matching(identifier: SKUAccessibility.creditDisclaimer)
                .firstMatch
        }

        /// Кнопка Оформить
        var checkoutButton: CheckoutButton {
            CheckoutButton(
                element: element
                    .buttons
                    .matching(identifier: SKUAccessibility.creditCheckoutButton)
                    .firstMatch
            )
        }
    }

    class VendorLinkButton: PageObject, FeedEntryPoint {
        func tapLego() -> WebViewPage {
            element.tap()
            return WebViewPage.current
        }
    }

    class AddServiceButton: PageObject, ServicesEntryPoint {}

    class OpinionsFastLink: PageObject {
        var rating: XCUIElement {
            element
                .images.matching(identifier: SKUAccessibility.opinionsFastLinkRating)
                .firstMatch
        }

        var opinionsCount: XCUIElement {
            element
                .staticTexts.matching(identifier: SKUAccessibility.opinionsFastLinkCount)
                .firstMatch
        }

        func tap() -> OpinionsPage {
            element.tap()
            let containerElement = XCUIApplication().otherElements[OpinionsAccessibility.container]
            return OpinionsPage(element: containerElement)
        }
    }

    class ShowAllSpecsButton: PageObject {
        func tap() -> SpecsPage {
            element.tap()
            return SpecsPage.current
        }
    }

    class ShowAllInstructionButton: PageObject {
        func tap() -> InstructionsPage {
            element.tap()
            return InstructionsPage.current
        }
    }

    class CustomerChoiseReason: PageObject {
        var icon: XCUIElement {
            element.images.firstMatch
        }

        var title: XCUIElement {
            element.textViews.firstMatch
        }
    }

    class Filter: PageObject {
        /// Кнопка, открывающая с SKU попап со всеми фильтрами и кратким описанием товара
        var moreButton: XCUIElement {
            let elem = collectionView.cellUniqueElement(withIdentifier: SKUAccessibility.filterMoreButton)
            return elem.staticTexts.firstMatch
        }

        class CollectionView: PageObject, UniformCollectionViewPage {
            var collectionView: XCUIElement {
                element.collectionViews.firstMatch
            }

            typealias AccessibilityIdentifierProvider = FilterCollectionViewCellsAccessibility
            typealias CellPage = FilterCell

            class FilterCell: PageObject {}
        }

        var collectionView: CollectionView {
            let elem = element.collectionViews.firstMatch
            return CollectionView(element: elem)
        }
    }

    class DeliveryOptions: PageObject, CollectionViewPage {

        // MARK: - CollectionViewPage

        typealias AccessibilityIdentifierProvider = SKUCollectionViewCellAccessibility

        var collectionView: XCUIElement {
            element
        }

        // MARK: - Nested Types

        class DeliveryOption: PageObject {
            var title: XCUIElement {
                element
                    .staticTexts.matching(identifier: SKUAccessibility.deliveryOptionTitle).firstMatch
            }
        }

        // MARK: - Properties

        var disclaimer: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionDisclaimer)
            return DeliveryOption(element: elem)
        }

        var clickAndCollect: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionClickAndCollect)
            return DeliveryOption(element: elem)
        }

        var pickup: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionPickup)
            return DeliveryOption(element: elem)
        }

        var bookingPickup: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionBookingPickup)
            return DeliveryOption(element: elem)
        }

        var post: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionPost)
            return DeliveryOption(element: elem)
        }

        var preorder: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionPreorder)
            return DeliveryOption(element: elem)
        }

        var preorderPayment: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionPreorderPayment)
            return DeliveryOption(element: elem)
        }

        var service: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionService)
            return DeliveryOption(element: elem)
        }

        var onDemand: DeliveryOption {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.deliveryOptionOnDemand)
            return DeliveryOption(element: elem)
        }
    }

    class SupplierInfo: PageObject {
        var title: XCUIElement {
            element.staticTexts.matching(identifier: SKUAccessibility.supplierInfoSubtitle).firstMatch
        }

        var infoButton: InfoButton {
            SupplierInfo.InfoButton(
                element: element
                    .buttons
                    .matching(identifier: SKUAccessibility.supplierInfoInfoButton)
                    .firstMatch
            )
        }

        class InfoButton: PageObject {
            func tap() -> SupplierTrustInfoLegalPage {
                element.tap()
                let el = XCUIApplication().otherElements[SupplierTrustInfoLegalAccessibility.root]
                return SupplierTrustInfoLegalPage(element: el)
            }
        }
    }

    class Rating: PageObject {

        var rating: XCUIElement {
            let elem = element
                .images.matching(identifier: SKUAccessibility.ratingRating)
                .firstMatch
            return elem
        }

        var grade: XCUIElement {
            let elem = element
                .staticTexts.matching(identifier: SKUAccessibility.ratingGrade)
                .firstMatch
            return elem
        }

        var gradeCount: XCUIElement {
            let elem = element
                .staticTexts.matching(identifier: SKUAccessibility.ratingGradeCount)
                .firstMatch
            return elem
        }

        var disclaimer: XCUIElement {
            element
                .staticTexts.matching(identifier: SKUAccessibility.ratingDisclaimer)
                .firstMatch
        }

    }

    /// Класс для кнопки "ответить"
    class ReplyButton: PageObject {
        func tap() -> WriteCommentPage {
            element.tap()
            let el = XCUIApplication().otherElements[CommentsAccessibility.commentView]
            return WriteCommentPage(element: el)
        }
    }

    class Link: PageObject, FeedEntryPoint {}

    class CheapestAsGiftCarouselCell: PageObject, SKUEntryPoint {}

    class SetItemView: PageObject, SKUEntryPoint {
        var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: SKUSetAccessibility.itemViewTitle)
                .firstMatch
        }

        var plus: XCUIElement {
            element
                .staticTexts
                .matching(identifier: SKUSetAccessibility.itemViewPlusSign)
                .firstMatch
        }
    }

    class AlternativeOffers: PageObject, CollectionViewPage {

        // MARK: - CollectionViewPage

        typealias AccessibilityIdentifierProvider = SKUCollectionViewCellAccessibility

        var collectionView: XCUIElement {
            element
        }

        // MARK: - Nested types

        class AlternativeOffer: PageObject {
            var title: XCUIElement {
                element.staticTexts
                    .matching(identifier: SKUAccessibility.alternativeOfferTitle)
                    .firstMatch
            }

            var subtitle: XCUIElement {
                element.buttons
                    .matching(identifier: SKUAccessibility.alternativeOfferSubtitle)
                    .firstMatch
            }

            var cartButton: XCUIElement {
                element.buttons
                    .matching(identifier: SKUAccessibility.alternativeOfferCartButton)
                    .firstMatch
            }

            var cashback: XCUIElement {
                element.staticTexts
                    .matching(identifier: SKUAccessibility.alternativeOfferCashback)
                    .firstMatch
            }
        }

        // MARK: - Properties

        var cheaper: AlternativeOffer {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.alternativeOffersCheaper)
            return AlternativeOffer(element: elem)
        }

        /// ячейка альтернативного оффера без reason
        var generic: AlternativeOffer {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.alternativeOfferGeneric)
            return AlternativeOffer(element: elem)
        }

        var deliveryType: AlternativeOffer {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.alternativeOffersDeliveryType)
            return AlternativeOffer(element: elem)
        }

        var faster: AlternativeOffer {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.alternativeOffersFaster)
            return AlternativeOffer(element: elem)
        }

        var gift: AlternativeOffer {
            let elem = cellUniqueElement(withIdentifier: SKUAccessibility.alternativeOffersGift)
            return AlternativeOffer(element: elem)
        }

        var showAllButton: XCUIElement {
            cellUniqueElement(withIdentifier: SKUAccessibility.alternativeOffersLinkButton)
        }
    }

    class SpecKeyValue: PageObject {
        /// матчим по тестку Key, т.к. используется KeyValueView, в котором accessibilityLabel - текст
        func specCell(withText identifier: String) -> XCUIElement {
            element
                .otherElements
                .matching(identifier: identifier)
                .firstMatch
        }
    }

    class PromocodePage: PageObject {
        var label: String {
            element.label
        }

        @discardableResult
        func tap() -> PromocodePopupPage {
            element.tapUnhittable()
            let popup = XCUIApplication().otherElements[PromocodePopupAccessibility.root]
            XCTAssertTrue(popup.waitForExistence(timeout: XCTestCase.defaultTimeOut))
            return PromocodePopupPage(element: popup)
        }
    }

    class GiftPage: PageObject {
        var infoButton: XCUIElement { element.buttons[SKUGiftAccessibility.infoButton] }
        var item: GiftItem { GiftItem(element: element.images[SKUGiftAccessibility.item]) }
        var allProductButton: XCUIElement { element.buttons[SKUGiftAccessibility.allProductsButton] }
    }

    class GiftItem: PageObject, SKUEntryPoint {}

    class CashbackItem: PageObject {
        var text: XCUIElement {
            element.textViews.firstMatch
        }
    }
}
