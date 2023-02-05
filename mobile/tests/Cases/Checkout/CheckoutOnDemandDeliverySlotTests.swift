import MarketUITestMocks
import XCTest

class CheckoutOnDemandDeliverySlotTests: LocalMockTestCase {

    override func setUp() {
        super.setUp()
        disable(toggles: FeatureNames.cartRedesign)
    }

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOnDemandPopupChooseOnDemand() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5279")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Ондеманд недоступен (попап)")
        Allure.addTitle("Выбор ондеманда")

        var checkoutPage: CheckoutPage!
        var popup: BarrierViewPage!

        "Мокаем состояние".ybm_run { _ in
            setupState(payment: .cardOnDelivery)
        }

        checkoutPage = openCheckout()
        popup = openPopup(on: checkoutPage)

        "Нажимаем 'Да, получу по клику' и проверяем изменение опции доставки".ybm_run { _ in
            popup.actionButton.tap()
            wait(forInvisibilityOf: popup.element)
            wait(forVisibilityOf: checkoutPage.dateSelectorCell(at: 0).element)
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).element)

            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "чт, 9 декабря, 0 ₽")
            XCTAssertTrue(checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.isSelected)
        }
    }

    func testOnDemandPopupChooseCourier() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5280")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Ондеманд недоступен (попап)")
        Allure.addTitle("Выбор курьера")

        var checkoutPage: CheckoutPage!
        var popup: BarrierViewPage!

        "Мокаем состояние".ybm_run { _ in
            setupState(payment: .applePay)
        }

        checkoutPage = openCheckout()
        popup = openPopup(on: checkoutPage)

        "Нажимаем 'Нет, хочу получить 8 декабря' и проверяем скрытие попапа".ybm_run { _ in
            popup.extraButton.tap()
            wait(forInvisibilityOf: popup.element)
            wait(forVisibilityOf: checkoutPage.dateSelectorCell(at: 0).element)

            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "ср, 8 декабря, 100 ₽")
        }
    }

    func testOnDemandHint() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5281")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Тултип для ондеманда")
        Allure.addTitle("Отображение тултипа")

        var checkoutPage: CheckoutPage!

        "Мокаем состояние".ybm_run { _ in
            setupState()
        }

        checkoutPage = openCheckout()

        "Проверяем наличие тултипа".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.onDemandHintView)
            XCTAssertEqual(checkoutPage.onDemandHintView.label, "Не выбирайте время заранее — решите в день доставки")
        }
    }

    func testOnDemandOnboarding() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5282")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Онбординг для ондеманда")
        Allure.addTitle("Открытие сторис")

        var checkoutPage: CheckoutPage!

        "Мокаем состояние".ybm_run { _ in
            setupState(cartItem: .freeAvia, stories: true)
        }

        checkoutPage = openCheckout()

        "Проверяем открытие сторис по нажатию на ячейку 'По клику'".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)
            checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element.tap()

            let story = StoryPage.current
            wait(forVisibilityOf: story.element)
            story.closeButton.tap()
            wait(forInvisibilityOf: story.element)
        }
    }

    // MARK: - Private

    private func openCheckout() -> CheckoutPage {
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        return checkoutPage
    }

    private func openPopup(on checkoutPage: CheckoutPage) -> BarrierViewPage {
        var popup: BarrierViewPage!

        "Проверяем, что доставка по клику задизейблена".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)
            wait(forVisibilityOf: checkoutPage.dateSelectorCell(at: 0).element)

            XCTAssertTrue(checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.isUnavailable)
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "ср, 8 декабря, 100 ₽")
        }

        "Нажимаем на ячейку 'По клику' и проверяем попап".ybm_run { _ in
            popup = checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.tapUnavailable()
            wait(forVisibilityOf: popup.element)

            XCTAssertEqual(popup.title.label, "Сможем доставить по клику с 9 декабря. Поменять дату?")
            XCTAssertEqual(popup.actionButton.label, "Да, получу по клику")
            XCTAssertEqual(popup.extraButton.label, "Нет, хочу получить 8 декабря")
        }

        return popup
    }

    // MARK: - Helper Methods

    typealias UserOrderOptions = ResolveUserOrderOptions.UserOrderOptions
    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupState(
        payment: Order.Payment? = nil,
        cartItem: CartItem = .unavailableOnDemand,
        stories: Bool = false
    ) {
        enable(toggles: FeatureNames.checkoutDeliverySlotsRedesign, FeatureNames.onDemandOnboarding)
        setupUserAddress()
        setupOrderOptions(cartItem: cartItem)
        if let payment = payment {
            setupOrderDetails(with: payment)
        }
        if stories {
            setupStoriesStates()
        }
    }

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions(cartItem: CartItem) {
        var cartState = CartState()
        let options = UserOrderOptions(
            region: .moscow,
            summary: .basic,
            shops: [cartItem]
        )
        cartState.setUserOrdersState(with: options)
        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails(with payment: Order.Payment) {
        var orderState = OrdersState()
        let order = SimpleOrder(
            status: .delivery,
            payment: payment,
            delivery: .init(deliveryPartnerType: .yandex, type: .service)
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all])
        stateManager?.setState(newState: orderState)
    }

    private func setupStoriesStates() {
        var storiesState = StoriesState()
        storiesState.setStoriesByIds(with: StoriesState.StoriesByIdsCollections.onDemandLongOnboardingStory)
        stateManager?.setState(newState: storiesState)
    }
}
