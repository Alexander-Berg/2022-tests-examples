import MarketUITestMocks
import XCTest

final class MissingOrderItemsTests: LocalMockTestCase {

    enum Constants {
        static let total: Int = 102
    }

    var root: RootPage!
    var popupPage: MissingOrderItemsPopupPage!
    var similarsFeedPage: SimilarsFeedPage!
    var orderDetails: OrderDetailsPage!

    private let postPaymentDescription =
        "К сожалению, этих товаров не оказалось на складе. Сумма заказа теперь \(Constants.total) ₽."
    private let prePaymentDescription =
        "К сожалению, этих товаров не оказалось на складе. 110 ₽ за них вернём на карту в течение нескольких дней."

    private let postPaymentOrderDetailsMessage = "Вычли из заказа стоимость этих товаров в 110 ₽"
    private let prePaymentOrderDetailsMessage = "110 ₽ за эти товары вернём на карту в течение нескольких дней"

    func test_MissingOrderItems_PostPayment_PopupAppears_OpenOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4472")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Продавец не может доставить. Морда + детали заказа")

        setup(bundleName: "Orders_MissingOrderItems_PostPayment")
        setupOrderDetails(orderId: "32398970", isPrepaid: false)
        openPopup(description: postPaymentDescription)
        openOrderDetailsFromPopup()
    }

    func test_MissingOrderItems_PostPayment_PopupAppears_OpenSimilars() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4473")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Продавец не может доставить. Морда + аналоги")

        setup(bundleName: "Orders_MissingOrderItems_PostPayment")
        setupOrderDetails(orderId: "32398970", isPrepaid: false)
        openPopup(description: postPaymentDescription)
        chooseSimilarItem()
        addSimilarItemToCart()
    }

    func test_MissingOrderItems_PrePayment_PopupAppears_OpenOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4475")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, предоплата. Продавец не может доставить. Морда + детали заказа")

        setup(bundleName: "Orders_MissingOrderItems_PrePayment")
        setupOrderDetails(orderId: "32399201", isPrepaid: true)
        openPopup(description: prePaymentDescription)
        openOrderDetailsFromPopup()
    }

    func test_MissingOrderItems_PrePayment_PopupAppears_OpenSimilars() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4476")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, предоплата. Продавец не может доставить. Морда + аналоги")

        setup(bundleName: "Orders_MissingOrderItems_PrePayment")
        setupOrderDetails(orderId: "32399201", isPrepaid: true)
        openPopup(description: prePaymentDescription)
        chooseSimilarItem()
        addSimilarItemToCart()
    }

    func test_MissingOrderItems_PostPayment_OrderDetails_OpenSimilars() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4474")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Продавец не может доставить. Детали заказа + аналоги")

        setup(bundleName: "Orders_MissingOrderItems_PostPayment_OrderDetails")
        setupOrderDetails(orderId: "32398970", isPrepaid: false)
        openOrderDetails(orderId: "32398970")
        checkRemovedItems(message: postPaymentOrderDetailsMessage)
        openSimilarItems()
    }

    func test_MissingOrderItems_PrePayment_OrderDetails_OpenSimilars() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4477")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, предоплата. Продавец не может доставить. Детали заказа + аналоги")

        setup(bundleName: "Orders_MissingOrderItems_PrePayment_OrderDetails")
        setupOrderDetails(orderId: "32399201", isPrepaid: true)
        openOrderDetails(orderId: "32399201")
        checkRemovedItems(message: prePaymentOrderDetailsMessage)
        openSimilarItems()
    }

    // MARK: - Old cases

    func test_MissingOrderItems_PostPayment_PopupAppears() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3758")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удален товар из заказа при постоплате")

        setup(bundleName: "Orders_MissingOrderItems_PostPayment")
        setupOrderDetails(orderId: "32398970", isPrepaid: false)
        openPopup(description: postPaymentDescription)
        chooseSimilarItem()
        addSimilarItemToCart()
    }

    func test_MissingOrderItems_PrePayment_PopupAppears() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3759")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удален товар из заказа при предоплате")

        setup(bundleName: "Orders_MissingOrderItems_PrePayment")
        setupOrderDetails(orderId: "32399201", isPrepaid: true)
        openPopup(description: prePaymentDescription)
        chooseSimilarItem()
        addSimilarItemToCart()
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    func setup(bundleName: String) {
        "Мокаем данные".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }
    }

    func setupOrderDetails(orderId: String, isPrepaid: Bool) {
        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .delivery,
                    payment: isPrepaid ? .prepaid : .cardOnDelivery,
                    total: Constants.total
                )
            ],
            orderEditRequest: [.removeRequest(orderId: orderId)]
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .recent(), .byId])

        stateManager?.setState(newState: orderState)
    }

    func openPopup(description: String) {
        "Ждем попап".ybm_run { _ in
            popupPage = MissingOrderItemsPopupPage.currentPopup
            ybm_wait(forVisibilityOf: [popupPage.textLabel])
            XCTAssertEqual(popupPage.textLabel.label, description)
        }

        "Мокаем пустой ответ резолвера агитаций".run {
            let agitationsRule = MockMatchRule(
                id: "EMPTY_AGITATIONS_RULE",
                matchFunction:
                isPOSTRequest &&
                    isFAPIRequest &&
                    hasExactFAPIResolvers(["resolveOrderAgitations"]),
                mockName: "resolveOrderAgitations_empty"
            )
            mockServer?.addRule(agitationsRule)
        }
    }

    private func openOrderDetails(orderId: String) {
        var profile: ProfilePage!
        var orders: OrdersListPage!

        "Открываем профиль".run {
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.element)
        }

        "Открываем список заказов".run {
            profile.element.ybm_swipeCollectionView(toFullyReveal: profile.myOrders.element)
            orders = profile.myOrders.tap()
            wait(forVisibilityOf: orders.element)
        }

        "Открываем заказ".run {
            let detailsButton = orders.detailsButton(orderId: orderId)
            orders.element.ybm_swipeCollectionView(toFullyReveal: detailsButton.element)
            orderDetails = detailsButton.tap()
        }
    }

    private func checkRemovedItems(message: String) {
        "Листаем до удаленных товаров".run {
            orderDetails.element.ybm_swipeCollectionView(toFullyReveal: orderDetails.changesMessage)
            XCTAssertEqual(orderDetails.changesMessage.text, message)
        }
    }

    private func openSimilarItems() {
        "Открываем похожие товары".run {
            orderDetails.element.ybm_swipeCollectionView(toFullyReveal: orderDetails.analogsButton)
            orderDetails.analogsButton.tap()
        }

        "Ждем открытия похожих товаров".ybm_run { _ in
            similarsFeedPage = SimilarsFeedPage.current
            ybm_wait(forVisibilityOf: [similarsFeedPage.textLabel])

            XCTAssertEqual(
                similarsFeedPage.textLabel.label,
                "Товар закончился, но есть\nпохожие — выбирайте"
            )
        }
    }

    private func openOrderDetailsFromPopup() {
        "Открываем заказ".run {
            let orderDetails = popupPage.orderButton.tap()
            ybm_wait(forVisibilityOf: [orderDetails.status])
        }
    }

    private func chooseSimilarItem() {
        "Выберем похожий товар".ybm_run { _ in
            ybm_wait(forVisibilityOf: [popupPage.choiceButton])
            popupPage.choiceButton.tap()
        }

        "Ждем открытия похожих товаров".ybm_run { _ in
            similarsFeedPage = SimilarsFeedPage.current
            ybm_wait(forVisibilityOf: [
                similarsFeedPage.navigationLabel,
                similarsFeedPage.textLabel,
                similarsFeedPage.bottomButton
            ])

            XCTAssertEqual(
                similarsFeedPage.textLabel.label,
                "Товар закончился, но есть\nпохожие — выбирайте"
            )

            XCTAssertEqual(
                similarsFeedPage.bottomButton.label,
                "Перейти в корзину"
            )

            XCTAssertTrue(similarsFeedPage.navigationLabel.label.contains("Похожие товары"))
        }
    }

    private func addSimilarItemToCart() {
        "Добавляем его в корзину".ybm_run { _ in
            let snippet = similarsFeedPage.snippetCell(at: 0)
            similarsFeedPage.collectionView.swipe(to: snippet.addToCartButton.element)
            snippet.addToCartButton.element.tap()
            ybm_wait(forFulfillmentOf: { snippet.addToCartButton.element.label == "1" })
        }
    }
}
