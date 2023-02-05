import MarketUITestMocks
import XCTest

final class OnDemandOnboardingTests: LocalMockTestCase {

    private var orderId: String?

    override func setUp() {
        super.setUp()
        enable(toggles: FeatureNames.onDemandOnboarding, FeatureNames.onDemandOnSku)
    }

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func test_checkout_closeOnboarding() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5209")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на чекауте")
        Allure.addTitle("Открытие сторис он-деманд на чекауте")

        checkoutTestActions()
        checkStory(action: .close)
    }

    func test_checkout_tellMore() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5222")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на чекауте")
        Allure.addTitle("Кнопка 'Расскажите подробнее' в сторис он-деманд")

        checkoutTestActions()
        checkStory(action: .tapTellMoreButton)
    }

    func test_sku_closeOnboarding() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5223")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на КТ")
        Allure.addTitle("Открытие сторис он-деманд из карточки товара")

        var skuPage: SKUPage!

        "Мокаем состояния".ybm_run { _ in
            setupStateForSKU()
        }

        "Открываем SKU".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Жмем на 'По клику в удобный момент'".ybm_run { _ in
            let onDemandDeliveryOption = skuPage.deliveryOptions.onDemand.element
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: onDemandDeliveryOption)
            onDemandDeliveryOption.tap()
        }

        checkStory(action: .close)
    }

    func test_finishOrder_closeOnboarding() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5225")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на экране 'Спасибо'")
        Allure.addTitle("Открытие сторис он-деманд на экране 'Спасибо'")

        finishOrderTestActions()
        checkStory(action: .close)
    }

    func test_finishOrder_tapISeeButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5226")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на экране 'Спасибо'")
        Allure.addTitle("Кнопка 'Понятно' в сторис он-деманд на экране 'Спасибо'")

        finishOrderTestActions()
        checkStory(action: .tapISeeButton)
    }

    func test_ordersList_closeOnboarding() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5229")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на списке заказов")
        Allure.addTitle("Открытие сторис он-деманд из списка заказов")

        ordersListTestActions()
        checkStory(action: .close)
    }

    func test_ordersList_tapISeeButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5228")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на списке заказов")
        Allure.addTitle("Кнопка 'Понятно' в сторис он-деманд из списка заказов")

        ordersListTestActions()
        checkStory(action: .tapISeeButton)
    }

    func test_ordersList_unpaidOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5230")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на списке заказов")
        Allure.addTitle("Не открытие сторис он-деманд из списка заказов")

        var ordersListPage: OrdersListPage!

        let orderId = "123456789"

        "Мокаем состояния".ybm_run { _ in
            setupOrderDetails(with: .applePay, status: .unpaid, orderId: orderId)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
        }

        "Проверяем отсутствие кнопки 'Как работает доставка по клику'".ybm_run { _ in
            wait(forVisibilityOf: ordersListPage.status(orderId: orderId))
            wait(forInvisibilityOf: ordersListPage.howOnDemandWorksButton(orderId: orderId))
        }
    }

    func test_order_closeOnboarding() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5231")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на деталях заказа")
        Allure.addTitle("Открытие сторис он-деманд из карточки заказа")

        orderTestActions()
        checkStory(action: .close)
    }

    func test_order_tapISeeButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5232")
        Allure.addEpic("Сторис ондеманда")
        Allure.addFeature("Онбординг на деталях заказа")
        Allure.addTitle("Кнопка 'Понятно' в сторис он-деманд из карточки заказа")

        orderTestActions()
        checkStory(action: .tapISeeButton)
    }
}

// MARK: - Common test actions

private extension OnDemandOnboardingTests {

    private func checkoutTestActions() {
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var onDemandSelectorCell: CheckoutPage.OnDemandSelectorCell!

        "Мокаем состояния".ybm_run { _ in
            setupStateForCheckout()
        }

        "Открываем корзину".ybm_run { _ in
            let rootPage = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: rootPage)
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем, что отображается доставка по клику".ybm_run { _ in
            onDemandSelectorCell = checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell
            wait(forVisibilityOf: onDemandSelectorCell.element)
//            wait(forVisibilityOf: checkoutPage.defaultServiceSelectorCell.element)
        }

        "Кликаем на (?) у ондеманда".ybm_run { _ in
            let infoButton = onDemandSelectorCell.infoButton
            wait(forVisibilityOf: infoButton)
            infoButton.tap()
        }
    }

    private func finishOrderTestActions() {
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Мокаем состояния".ybm_run { _ in
            setupStateForFinishOrder()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Подтверждаем заказ".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            finishMultiorderPage = checkoutPage.paymentButton.tap()
            wait(forVisibilityOf: finishMultiorderPage.element)
        }

        "Нажимаем кнопку 'Как работает доставка по клику'".ybm_run { _ in
            let howOnDemandWorksButton = finishMultiorderPage.howOnDemandWorksButton(at: 0)
            finishMultiorderPage.element.swipe(to: .down, untilVisible: howOnDemandWorksButton)
            howOnDemandWorksButton.tap()
        }
    }

    private func ordersListTestActions() {
        var ordersListPage: OrdersListPage!

        let orderId = "123456789"

        "Мокаем состояния".ybm_run { _ in
            setupStateForOrder(orderId: orderId)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
        }

        "Нажимаем кнопку 'Как работает доставка по клику'".ybm_run { _ in
            let button = ordersListPage.howOnDemandWorksButton(orderId: orderId)
            wait(forVisibilityOf: button)
            button.tap()
        }
    }

    private func orderTestActions() {
        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        let orderId = "123456789"

        "Мокаем состояния".ybm_run { _ in
            setupStateForOrder(orderId: orderId)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
        }

        "Нажимаем кнопку 'Как работает доставка по клику'".ybm_run { _ in
            let button = orderDetailsPage.howOnDemandWorksButton(orderId: orderId)
            wait(forVisibilityOf: button)
            button.tap()
        }
    }

    private func checkStory(action: StoryFinalAction) {
        var storyPage: StoryPage!
        var actionButton: XCUIElement!

        "Ждем открытия и прокрутки сторис".ybm_run { _ in
            storyPage = StoryPage.current
            wait(forVisibilityOf: storyPage.element)
            actionButton = storyPage.actionButton
            wait(forVisibilityOf: actionButton)
        }

        switch action {
        case .close:
            "Закрываем сторис".ybm_run { _ in
                storyPage.tapRight()
                ybm_wait { storyPage.element.exists == false }
            }
        case .tapTellMoreButton:
            "Нажимаем 'Расскажите подробнее'".ybm_run { _ in
                XCTAssertEqual(actionButton.label, "Расскажите подробнее")
                actionButton.tap()
                ybm_wait { storyPage.element.exists == false }

                let webViewPage = WebViewPage.current
                wait(forVisibilityOf: webViewPage.element)
            }
        case .tapISeeButton:
            "Нажимаем 'Понятно'".ybm_run { _ in
                XCTAssertEqual(actionButton.label, "Понятно")
                actionButton.tap()
                ybm_wait { storyPage.element.exists == false }
            }
        }
    }
}

// MARK: - Mocks

private extension OnDemandOnboardingTests {

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupStateForCheckout() {
        setupUserAddress()
        setupOrderOptions()
        setupOrderDetails(with: .applePay)
        setupStories(kind: .long)
    }

    private func setupStateForSKU() {
        setupSKU()
        setupStories(kind: .long)
    }

    private func setupStateForFinishOrder() {
        setupUserAddress()
        setupOrderOptions(options: .outlet)
        setupOrderDetails(with: .cashOnDelivery, resolvers: [.all])
        setupCheckoutState()
        setupStories(kind: .short)
    }

    private func setupStateForOrder(orderId: String) {
        setupOrderDetails(with: .applePay, orderId: orderId)
        setupStories(kind: .short)
    }

    // MARK: - Common

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [
            .init(region: .moscow, address: .shabolovka)
        ])
        userState.setContactsState(contacts: [.basic])
        userState.setFavoritePickupPoints(favoritePickups: [.rublevskoye])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions(options: CartState.UserOrderOptions = .onDemandAndService) {
        var cartState = CartState()
        cartState.setUserOrdersState(with: options)
        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails(
        with payment: Order.Payment,
        status: SimpleOrder.Status = .delivery,
        orderId: String? = nil,
        resolvers: [OrdersState.OrderResolvers] = [.all, .byId, .byIds]
    ) {
        var orderState = OrdersState()

        let order = SimpleOrder(
            id: orderId,
            status: status,
            payment: payment,
            delivery: .init(deliveryPartnerType: .yandex, type: .service, features: [.onDemand]),
            msku: ["1"]
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: resolvers)

        stateManager?.setState(newState: orderState)
    }

    private func setupStories(kind: StoryKind) {
        var storiesState = StoriesState()

        storiesState.setStoriesByIds(with: kind.collections)

        stateManager?.setState(newState: storiesState)
    }

    private func setupSKU() {
        var skuState = SKUInfoState()

        let pickupDelivery = FAPIOffer.Delivery.PickupOptions(dayFrom: 2, dayTo: 5)
        let courierDelivery = FAPIOffer.Delivery.PickupOptions(dayFrom: 2, dayTo: 2)
        let offer = modify(FAPIOffer.default) {
            $0.delivery.pickupOptions = [pickupDelivery]
            $0.delivery.courierOptions = [courierDelivery]
        }

        skuState.setSkuInfoState(offer: offer, model: .default)
        stateManager?.setState(newState: skuState)
    }

    private func setupCheckoutState() {
        var checkoutState = CheckoutState()
        checkoutState.setUserOrderState(orderResponse: OrderResponse(shops: [.delivery]))
        stateManager?.setState(newState: checkoutState)
    }
}

// MARK: - Nested Types

private extension OnDemandOnboardingTests {
    enum StoryKind {
        case long, short

        var collections: StoriesState.StoriesByIdsCollections {
            switch self {
            case .long:
                return .onDemandLongOnboardingStory
            case .short:
                return .onDemandShortOnboardingStory
            }
        }
    }

    enum StoryFinalAction {
        case close, tapISeeButton, tapTellMoreButton
    }
}
