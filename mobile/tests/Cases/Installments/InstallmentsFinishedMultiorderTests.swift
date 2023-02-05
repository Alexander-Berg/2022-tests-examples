import MarketUITestMocks
import XCTest

final class InstallmentsFinishedMultiorderTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSuccessfulPaymentWithInstallments() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5250")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("Страница Спасибо")
        Allure.addTitle("Успешная оплата в Рассрочку")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var finishPage: FinishMultiorderPage!

        "Настраиваем FT".run {
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".run {
            setupUserAddress()
            setupOrderOptions()
            setupCartState()
            setupOrderDetails()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Переходим в чекаут".run {
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Оформляем рассрочку и переходим на страницу Спасибо".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)

            // Оплата рассрочки происходит через PaymentSDK.
            // Скипаем этот этап через экран ошибки оплаты и переходим сразу на экран Спасибо.
            let barrierViewPage = checkoutPage.paymentButton.tapToBarrierView()
            wait(forVisibilityOf: barrierViewPage.element)

            finishPage = barrierViewPage.toToFinishedMultiorderPage()
            wait(forVisibilityOf: finishPage.element)
        }

        "Проверяем статус заказа".run {
            wait(forVisibilityOf: finishPage.title)

            XCTAssert(finishPage.title.isVisible, "Статус заказа не виден")
            XCTAssertEqual(finishPage.title.label, "Готово! Заказ оформлен")
        }

        "Проверяем заголовок блока информации о рассрочке".run {
            finishPage.element.ybm_swipeCollectionView(toFullyReveal: finishPage.installmentsTitle)

            XCTAssert(finishPage.installmentsTitle.isVisible, "Заголовок блока информации о рассрочке не виден")
            XCTAssertEqual(finishPage.installmentsTitle.label, "Что будет дальше")
        }

        "Проверяем информацию о последующих платежах".run {
            finishPage.element.ybm_swipeCollectionView(toFullyReveal: finishPage.installmentsTinkoffAppPaymentsTitle)

            XCTAssert(
                finishPage.installmentsTinkoffAppPaymentsTitle.isVisible,
                "Информация о последующих платежах не видна"
            )
            XCTAssertEqual(
                finishPage.installmentsTinkoffAppPaymentsTitle.label,
                "Все платежи будут осуществляться через приложение Тинькофф"
            )
        }

        "Проверяем информацию о напоминании перед очередным списанием".run {
            finishPage.element.ybm_swipeCollectionView(toFullyReveal: finishPage.installmentsPaymentReminderTitle)

            XCTAssert(
                finishPage.installmentsPaymentReminderTitle.isVisible,
                "Информация о напоминании перед очередным списанием не видна"
            )
            XCTAssertEqual(
                finishPage.installmentsPaymentReminderTitle.label,
                "За день до списания вам придёт напоминание"
            )
        }

        "Открываем детальную информацию".ybm_run { _ in
            finishPage.element.ybm_swipeCollectionView(toFullyReveal: finishPage.detailSection)
            finishPage.detailSection.tap()
        }

        "Проверяем способ оплаты".ybm_run { _ in
            let paymentMethod = finishPage.paymentMethodOfOrderItem()
            finishPage.element.ybm_swipeCollectionView(toFullyReveal: paymentMethod.element)

            XCTAssert(paymentMethod.element.isVisible, "Способ оплаты не виден")
            XCTAssertEqual(paymentMethod.textView.label, "В рассрочку от Тинькофф")
        }
    }
}

// MARK: - Helper Methods

private extension InstallmentsFinishedMultiorderTests {

    func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        userState.setContactsState(contacts: [.basic])

        stateManager?.setState(newState: userState)
    }

    func setupOrderOptions() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .prepaid)

        stateManager?.setState(newState: cartState)
    }

    func setupOrderDetails() {
        let orderMapper = Order.Mapper(
            status: .delivery,
            payment: .installments,
            delivery: .service
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(
            mapper: OrdersState.UserOrdersHandlerMapper(orders: [orderMapper]),
            for: [.all, .byIds]
        )

        stateManager?.setState(newState: orderState)
    }

    func setupCartState() {
        let cartItem = modify(CartItem.installments) {
            $0.label = Constants.shopLabel
        }

        var cartState = CartState()
        cartState.setCartStrategy(with: [.protein])
        cartState.setUserOrdersState(
            with: .init(region: .moscow, summary: .installments, shops: [cartItem])
        )

        stateManager?.setState(newState: cartState)
    }
}

// MARK: - Nested Types

private extension InstallmentsFinishedMultiorderTests {

    enum Constants {
        static let shopLabel = "145_0"
    }
}
