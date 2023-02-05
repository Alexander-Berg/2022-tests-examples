import MarketUITestMocks
import XCTest

class CreditCartToCheckoutTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCartGoToCheckout() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4748")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Виджет в Корзине. Переход в чекаут")

        var cartPage: CartPage!
        var creditInfo: CartPage.CreditInfoCellPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
            disable(toggles: FeatureNames.cartRedesign)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_CartToCheckout")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем, что присутствует кредитный виджет".ybm_run { _ in
            creditInfo = cartPage.summary.creditInfo
            cartPage.collectionView.swipe(to: .down, until: creditInfo.monthlyPayment.isVisible)

            XCTAssert(creditInfo.element.isVisible)
        }

        "Проверяем, что присутствует сумма ежемесячного платежа".ybm_run { _ in
            let monthlyPayment = creditInfo.monthlyPayment

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 1 032 ₽ / мес"))
        }

        "Проверяем, что присутствует кнопка \"Оформить\"".ybm_run { _ in
            XCTAssert(creditInfo.buyInCreditButton.element.isVisible)
        }

        "Нажимаем на кнопку \"Оформить\" и переходим в чекаут".ybm_run { _ in
            checkoutPage = creditInfo.buyInCreditButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты \"В кредит\"".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            XCTAssert(checkoutPage.paymentMethodCell.element.isVisible)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "В кредит")
        }

        "Проверяем саммари на количество товара и сумму".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryItemsCell.element)
            let countFromItems = checkoutPage.summaryItemsCell.title.label.split(separator: " ")

            XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "19 632 ₽")
            XCTAssertEqual(countFromItems.last, "(2)")
        }
    }

    func testMonthlyPaymentRecalculation() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4749")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Виджет в Корзине. Пересчет ежемесячного платежа")

        var cartPage: CartPage!
        var creditInfo: CartPage.CreditInfoCellPage!
        var item: CartPage.CartItem!
        var monthlyPayment: XCUIElement!
        var pickerWheel: XCUIElement!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
            disable(toggles: FeatureNames.cartRedesign)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_Cart")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем, что присутствует кредитный виджет".ybm_run { _ in
            creditInfo = cartPage.summary.creditInfo
            cartPage.collectionView.swipe(to: .down, until: creditInfo.monthlyPayment.isVisible)

            XCTAssert(creditInfo.element.isVisible)
        }

        "Проверяем, что присутствует сумма ежемесячного платежа".ybm_run { _ in
            monthlyPayment = creditInfo.monthlyPayment

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 516 ₽ / мес"))
        }

        "Проверяем, что присутствует кнопка \"Оформить\"".ybm_run { _ in
            XCTAssert(creditInfo.buyInCreditButton.element.isVisible)
        }

        "Открываем пикер количества товара".ybm_run { _ in
            item = cartPage.cartItem(at: 0)
            pickerWheel = cartPage.pickerWheel
            cartPage.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: item.element)
            item.countPicker.tap()
            ybm_wait(forFulfillmentOf: { pickerWheel.isVisible })
        }

        "Мокаем состояние с 2 шт. товара".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_Cart_CounterUp")
        }

        "Выбираем количество товара в 2 шт.".ybm_run { _ in
            pickerWheel.adjust(toPickerWheelValue: "2")
            cartPage.countPickerDoneButton.tap()
            wait(forInvisibilityOf: pickerWheel)
        }

        "Проверяем количество товара".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.element)
            wait(forVisibilityOf: cartPage.summary.element)

            let totalItems = cartPage.summary.totalItems
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: totalItems.element)

            XCTAssertEqual(totalItems.title.label, "Товары (2)")
        }

        "Проверяем, что присутствует кредитный виджет".ybm_run { _ in
            cartPage.collectionView.swipe(to: .down, until: creditInfo.element.isVisible)

            XCTAssert(creditInfo.element.isVisible)
        }

        "Проверяем, что произошел перерасчет суммы ежемесячного платежа - увеличился в два раза".ybm_run { _ in
            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 1 032 ₽ / мес"))
        }
    }

    func testCartGoToCheckoutAndBack() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4831")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Корзина")
        Allure.addTitle("Возврат в корзину из чекаута")

        var cartPage: CartPage!
        var creditInfoPage: CartPage.CreditInfoCellPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".ybm_run { _ in
            enable(toggles: FeatureNames.tinkoffCredit)
            disable(toggles: FeatureNames.cartRedesign)
        }

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrdersState()
            setupCartState()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем, что отображен ежемесячный платеж. Кнопка \"Оформить\" доступна".ybm_run { _ in
            creditInfoPage = cartPage.credit.creditInfo
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: creditInfoPage.element)

            XCTAssertTrue(creditInfoPage.element.exists, "Отсутствует кредитный виджет")
            XCTAssertTrue(creditInfoPage.monthlyPayment.isVisible, "Ежемесячный платёж не отображён")
            XCTAssertTrue(creditInfoPage.buyInCreditButton.element.isVisible, "Кнопка \"Оформить\" не отображена")
        }

        "Нажимаем на кнопку \"Оформить\" и переходим в чекаут".ybm_run { _ in
            checkoutPage = creditInfoPage.buyInCreditButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты \"В кредит\"".ybm_run { _ in
            let paymentMethodCell = checkoutPage.paymentMethodCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentMethodCell.element)

            XCTAssert(paymentMethodCell.element.isVisible, "Кнопка \"Метод оплаты\" не отображена")
            XCTAssertEqual(paymentMethodCell.title.label, "В кредит")
        }

        "Проверяем кнопку перехода к заполнению кредитной заявки".ybm_run { _ in
            let paymentButton = checkoutPage.paymentButton
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentButton.element)

            XCTAssertEqual(paymentButton.title.label, "Перейти к заполнению заявки")
        }

        "Закрываем чекаут".ybm_run { _ in
            NavigationBarPage.current.closeButton.tap()
            wait(forVisibilityOf: cartPage.element)
        }

        "Проверяем, что отображен ежемесячный платеж. Кнопка \"Оформить\" доступна".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: creditInfoPage.element)

            XCTAssertTrue(creditInfoPage.element.exists, "Отсутствует кредитный виджет")
            XCTAssertTrue(creditInfoPage.monthlyPayment.isVisible, "Ежемесячный платёж не отображён")
            XCTAssertTrue(creditInfoPage.buyInCreditButton.element.isVisible, "Кнопка \"Оформить\" не отображена")
        }
    }

    // MARK: - Helper Methods

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setContactsState(contacts: [.basic])
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

    private func setupOrdersState() {
        var ordersState = OrdersState()
        ordersState.setOrdersResolvers(
            mapper: .init(orders: [.init(status: .delivered)]),
            for: [.all]
        )
        stateManager?.setState(newState: ordersState)
    }

    private func setupCartState() {
        var cartState = CartState()

        var itemInCart = CAPIOffer.protein
        itemInCart.cartItemLabel = Constants.itemLabel
        cartState.setCartStrategy(with: [.protein])

        var item = Item.basic
        item.label = Constants.itemLabel
        var cartItem = CartItem.credit
        cartItem.label = Constants.shopLabel
        cartItem.items = [item]
        cartState.setUserOrdersState(
            with: .init(region: .moscow, summary: .credit, shops: [cartItem])
        )

        stateManager?.setState(newState: cartState)
    }
}

// MARK: - Nested Types

private extension CreditCartToCheckoutTests {

    enum Constants {
        static let shopLabel = "145_0"
        static let itemLabel = "g7smldnvvdr"
    }
}
