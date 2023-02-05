import MarketUITestMocks
import XCTest

final class BNPLCheckoutTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testGoToCheckoutFromWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5538")
        Allure.addEpic("BNPL")
        Allure.addFeature("Чекаут")
        Allure.addTitle("Отображение БНПЛ")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".run {
            enable(toggles: FeatureNames.BNPL)
        }

        "Мокаем состояние".run {
            setupUserAddress()
            setupOrdersState()
            setupCartStateForCheckout()
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие виджета и графика платежей".run {
            let bnplPlanConstructor = cartPage.bnpl.bnplPlanConstructor
            let planView = bnplPlanConstructor.planView
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: planView, withVelocity: .slow)

            XCTAssert(planView.isVisible, "График платежей не виден")
        }

        "Переходим в чекаут по кнопке \"К оформлению\"".run {
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем способ оплаты картой".run {
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.paymentMethodCell.element
            )

            XCTAssert(checkoutPage.paymentMethodCell.element.isVisible)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }

        "Проверяем видимость плана платежей".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.bnplPlanCell.element)

            XCTAssert(checkoutPage.bnplPlanCell.planView.isVisible, "План платежей не виден")
        }

        "Проверяем заголовок переключателя BNPL".run {
            let bnplSwitchLabel = checkoutPage.bnplSwitchLabel
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: bnplSwitchLabel)

            XCTAssert(bnplSwitchLabel.isVisible, "Заголовок переключателя BNPL не выден")
            XCTAssertEqual(bnplSwitchLabel.label, "Разбить платёж на части")
        }

        "Проверяем, что переключатель BNPL выключен".run {
            let bnplSwitch = checkoutPage.bnplSwitch

            XCTAssert(bnplSwitch.isVisible, "Переключатель BNPL не выден")
            XCTAssert(bnplSwitch.isEnabled, "Переключатель BNPL неактивен")
            XCTAssertFalse(bnplSwitch.isOn, "Переключатель BNPL включён")
        }
    }

    func testBNPLSwitch() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5540")
        Allure.addEpic("BNPL")
        Allure.addFeature("Чекаут")
        Allure.addTitle("Работа свитчера \"оплата частями\"")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var bnplSwitch: XCUIElement!
        var detailsButton: CheckoutPage.BnplFirstPaymentDetailsButton!
        var paymentButton: CheckoutPage.PaymentButton!

        "Настраиваем FT".run {
            enable(toggles: FeatureNames.BNPL)
        }

        "Мокаем состояние".run {
            setupUserAddress()
            setupOrdersState()
            setupCartStateForCheckout()
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Переходим в чекаут по кнопке \"К оформлению\"".run {
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем способ оплаты картой".run {
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.paymentMethodCell.element
            )

            XCTAssert(checkoutPage.paymentMethodCell.element.isVisible)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }

        "Проверяем видимость плана платежей".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.bnplPlanCell.element)

            XCTAssert(checkoutPage.bnplPlanCell.planView.isVisible, "План платежей не виден")
        }

        "Проверяем, что переключатель BNPL выключен".run {
            bnplSwitch = checkoutPage.bnplSwitch

            XCTAssert(bnplSwitch.isVisible, "Переключатель BNPL не выден")
            XCTAssertFalse(bnplSwitch.isOn, "Переключатель BNPL включён")
        }

        "Проверяем кнопку оформления заказа".run {
            paymentButton = checkoutPage.paymentButton
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: paymentButton.element,
                withVelocity: .slow
            )

            XCTAssertEqual(paymentButton.title.label, "Перейти к оплате")
        }

        "Проверяем, что кнопка \"Как получился первый платёж\" не видна".run {
            detailsButton = checkoutPage.bnplFirstPaymentDetailsButton

            XCTAssertFalse(detailsButton.element.isVisible, "Кнопка \"Как получился первый платёж\" видна")
        }

        "Включаем переключатель BNPL".run {
            bnplSwitch.tap()
            ybm_wait { bnplSwitch.isOn }
        }

        "Проверяем, что появилась кнопка \"Как получился первый платёж\"".run {
            XCTAssert(detailsButton.element.isVisible, "Кнопка \"Как получился первый платёж\" не видна")
            XCTAssertEqual(detailsButton.title.label, "Как получился первый платёж?")
        }

        "Проверяем кнопку оформления оплаты частями".run {
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: paymentButton.element,
                withVelocity: .slow
            )

            XCTAssertEqual(paymentButton.title.label, "Оформить оплату частями")
        }

        "Выключаем переключатель BNPL".run {
            bnplSwitch.tap()
            ybm_wait { !bnplSwitch.isOn }
        }

        "Проверяем, что кнопка \"Как получился первый платёж\" пропала".run {
            XCTAssertFalse(detailsButton.element.isVisible, "Кнопка \"Как получился первый платёж\" видна")
        }

        "Проверяем кнопку оформления заказа".run {
            XCTAssertEqual(paymentButton.title.label, "Перейти к оплате")
        }
    }
}

// MARK: - Helper Methods

private extension BNPLCheckoutTests {

    func setupCartStateForCheckout() {
        let cartItem = modify(CartItem.bnpl) {
            $0.label = Constants.shopLabel
        }

        var cartState = CartState()
        cartState.setCartStrategy(with: [.protein])
        cartState.setUserOrdersState(
            with: .init(region: .moscow, summary: .bnpl, shops: [cartItem])
        )

        stateManager?.setState(newState: cartState)
    }

    func setupUserAddress() {
        var userState = UserAuthState()
        userState.setContactsState(contacts: [.basic])
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

    func setupOrdersState() {
        var ordersState = OrdersState()
        ordersState.setOrdersResolvers(mapper: .prepaid, for: [.all])
        stateManager?.setState(newState: ordersState)
    }
}

// MARK: - Nested Types

private extension BNPLCheckoutTests {

    enum Constants {
        static let shopLabel = "145_0"
    }
}
