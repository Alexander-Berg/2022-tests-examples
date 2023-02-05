import MarketUITestMocks
import XCTest

final class BNPLCartTests: LocalMockTestCase {

    func testBNPLWidgetVisibility() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5531")
        Allure.addEpic("BNPL")
        Allure.addFeature("Корзина")
        Allure.addTitle("Отображение виджета")

        var cartPage: CartPage!
        var bnplPlanConstructor: BNPLPlanConstructorPage!

        "Настраиваем FT".run {
            enable(toggles: FeatureNames.BNPL)
        }

        "Мокаем состояние".run {
            setupCartState()
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие виджета и графика платежей".run {
            bnplPlanConstructor = cartPage.bnpl.bnplPlanConstructor
            let planView = bnplPlanConstructor.planView
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: planView, withVelocity: .slow)

            XCTAssert(planView.isVisible, "График платежей не виден")
        }

        "Проверяем первый платеж".run {
            let deposit = bnplPlanConstructor.deposit

            XCTAssert(deposit.isVisible, "Лейбл первого платежа не виден")
            XCTAssertEqual(deposit.label, "759 ₽ сегодня")
        }

        "Проверяем остальные платежи".run {
            let payments = bnplPlanConstructor.payments

            XCTAssert(payments.isVisible, "Лейбл остальных платежей не виден")
            XCTAssertEqual(payments.label, "и 2 274 ₽ потом")
        }

        "Проверяем кнопку \"Оформить\"".run {
            XCTAssert(bnplPlanConstructor.checkoutButton.element.isVisible, "Кнопки \"Оформить\" не видно")
        }

        "Проверяем кнопку подробнее о BNPL".run {
            let detailsButton = bnplPlanConstructor.detailsButton

            XCTAssert(detailsButton.element.isVisible, "Кнопки подробнее о BNPL не видно")
            XCTAssertEqual(detailsButton.element.label, "Подробнее")
        }
    }

    func testGoToCheckoutFromWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5533")
        Allure.addEpic("BNPL")
        Allure.addFeature("Корзина")
        Allure.addTitle("Переход в чекаут по виджету")

        var cartPage: CartPage!
        var bnplPlanConstructor: BNPLPlanConstructorPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".run {
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)
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
            bnplPlanConstructor = cartPage.bnpl.bnplPlanConstructor
            let planView = bnplPlanConstructor.planView
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: planView, withVelocity: .slow)

            XCTAssert(planView.isVisible, "График платежей не виден")
        }

        "Нажимаем кнопку \"Оформить\"".run {
            checkoutPage = bnplPlanConstructor.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты картой".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            XCTAssert(checkoutPage.paymentMethodCell.element.isVisible)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }

        "Проверяем, что переключатель BNPL включён".run {
            let bnplSwitch = checkoutPage.bnplSwitch
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: bnplSwitch)

            XCTAssert(bnplSwitch.isEnabled, "Переключатель BNPL неактивен")
            XCTAssert(bnplSwitch.isOn, "Переключатель BNPL выключен")
        }

        "Проверяем видимость плана платежей".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.bnplPlanCell.element)

            XCTAssert(checkoutPage.bnplPlanCell.planView.isVisible, "План платежей не виден")
        }

        "Проверяем кнопку оформления оплаты частями".run {
            let paymentButton = checkoutPage.paymentButton
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentButton.element)

            XCTAssertEqual(paymentButton.title.label, "Оформить оплату частями")
        }
    }
}

// MARK: - Helper Methods

private extension BNPLCartTests {

    func setupCartState() {
        var cartState = CartState()
        cartState.setCartStrategy(with: [.protein])
        cartState.setUserOrdersState(with: .bnpl)
        stateManager?.setState(newState: cartState)
    }

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
        ordersState.setOrdersResolvers(mapper: .default, for: [.all])
        stateManager?.setState(newState: ordersState)
    }
}

// MARK: - Nested Types

private extension BNPLCartTests {

    enum Constants {
        static let shopLabel = "145_0"
    }
}
