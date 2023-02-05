import MarketUITestMocks
import XCTest

final class InstallmentsCartTests: LocalMockTestCase {

    func testInstallmentsWidgetVisibility() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5211")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("Корзина")
        Allure.addTitle("Виджет под саммари отображен")

        var cartPage: CartPage!
        var installmentsInfo: CartPage.InstallmentsInfoCellPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupCartState()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие виджета рассрочки".ybm_run { _ in
            installmentsInfo = cartPage.installments.installmentsInfo
            cartPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: installmentsInfo.element,
                withVelocity: .slow
            )

            XCTAssert(installmentsInfo.element.isVisible, "Виджет рассрочки не виден")
        }

        "Проверяем описание рассрочки".ybm_run { _ in
            let title = installmentsInfo.title

            XCTAssert(title.isVisible, "Лейбл описания рассрочки не виден")
            XCTAssertEqual(title.label, "Рассрочка от Тинькофф")
        }

        "Проверяем выбранный срок рассрочки".ybm_run { _ in
            let term = installmentsInfo.selector.selectedCell.term

            XCTAssert(term.isVisible, "Лейбл выбранного срока рассрочки не виден")
            XCTAssertEqual(term.label, "12 мес")
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            let payment = installmentsInfo.monthlyPayment

            XCTAssert(payment.isVisible, "Лейбл ежемесячного платежа не виден")
            XCTAssertEqual(payment.label, "2 333 ₽ / мес")
        }

        "Проверяем кнопку \"Оформить\"".ybm_run { _ in
            XCTAssert(installmentsInfo.checkoutButton.element.isVisible, "Кнопки \"Оформить\" не видно")
        }
    }

    func testMonthlyPaymentChangeWhenSelectorScrolled() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5212")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("Корзина")
        Allure.addTitle("Виджет. Скролл барабана (корзина)")

        var cartPage: CartPage!
        var installmentsInfo: CartPage.InstallmentsInfoCellPage!
        var payment: XCUIElement!
        var selector: CartPage.InstallmentsSelectorPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupCartState()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие виджета рассрочки".ybm_run { _ in
            installmentsInfo = cartPage.installments.installmentsInfo
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: installmentsInfo.element)

            XCTAssert(installmentsInfo.element.isVisible, "Виджет рассрочки не виден")
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            payment = installmentsInfo.monthlyPayment

            XCTAssert(payment.isVisible, "Лейбл ежемесячного платежа не виден")
            XCTAssertEqual(payment.label, "2 333 ₽ / мес")
        }

        "Свайпаем селектор вверх и проверяем ежемесячный платёж".ybm_run { _ in
            selector = installmentsInfo.selector
            selector.element.swipe(from: .zero, to: .init(dx: 0, dy: -20), withVelocity: .slow)
            ybm_wait { payment.label != "2 333 ₽ / мес" }

            XCTAssertEqual(payment.label, "1 167 ₽ / мес")
        }

        "Свайпаем селектор вниз и проверяем ежемесячный платёж".ybm_run { _ in
            selector.element.swipe(from: .zero, to: .init(dx: 0, dy: 20), withVelocity: .slow)
            ybm_wait { payment.label != "1 167 ₽ / мес" }

            XCTAssertEqual(payment.label, "4 665 ₽ / мес")
        }
    }

    func testOpenInstallmentsSelectorPopupAndSelectAnotherTerm() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5213")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("Корзина")
        Allure.addTitle("Виджет. Тап на барабан - изменение срока (корзина)")

        var cartPage: CartPage!
        var installmentsInfo: CartPage.InstallmentsInfoCellPage!
        var selectorPopupPage: InstallmentsSelectorPopupPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupCartState()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие виджета рассрочки".ybm_run { _ in
            installmentsInfo = cartPage.installments.installmentsInfo
            cartPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: installmentsInfo.element,
                withVelocity: .slow
            )

            XCTAssert(installmentsInfo.element.isVisible, "Виджет рассрочки не виден")
            XCTAssertEqual(installmentsInfo.monthlyPayment.label, "2 333 ₽ / мес")
        }

        "Нажимаем на барабан селектора рассрочи и ждём поднятия попапа селектора".ybm_run { _ in
            selectorPopupPage = installmentsInfo.selector.tap()
            wait(forVisibilityOf: selectorPopupPage.element)
        }

        "Проверяем хедер попапа и лейбл кнопки Выбрать".ybm_run { _ in
            XCTAssertEqual(selectorPopupPage.titleLabel.label, "Выберите срок рассрочки")
            XCTAssertEqual(selectorPopupPage.selectButton.label, "Выбрать")
        }

        "Проверяем наличие ячеек со всеми сроками и платежами".ybm_run { _ in
            selectorPopupPage.collectionView.enumerateCells(cellCount: Constants.terms.count) { cell, indexPath in
                XCTAssert(cell.element.isVisible)
                XCTAssertEqual(cell.term.label, Constants.terms[indexPath.item])
                XCTAssertEqual(cell.monthlyPayment.label, Constants.monthlyPayments[indexPath.item])
            }
        }

        "Проверяем, выбран ли срок рассрочки по умолчанию".ybm_run { _ in
            let selectedCell = selectorPopupPage.collectionView.selectedCell

            XCTAssertEqual(selectedCell.term.label, "12 мес")
            XCTAssertEqual(selectedCell.monthlyPayment.label, "по 2 333 ₽")
        }

        "Выбираем новый срок рассрочки".ybm_run { _ in
            selectorPopupPage.collectionView.cellPage(at: IndexPath(item: 2, section: 0)).element.tap()
        }

        "Проверяем, выбран ли новый срок рассрочки".ybm_run { _ in
            let newSelectedCell = selectorPopupPage.collectionView.selectedCell

            XCTAssertEqual(newSelectedCell.term.label, "24 мес")
            XCTAssertEqual(newSelectedCell.monthlyPayment.label, "по 1 167 ₽")
        }

        "Нажимаем кнопку Выбрать".ybm_run { _ in
            selectorPopupPage.selectButton.tap()
        }

        "Проверяем новый ежемесячный платёж на виджете рассрочки".ybm_run { _ in
            wait(forVisibilityOf: installmentsInfo.element)
            ybm_wait { installmentsInfo.monthlyPayment.label != "2 333 ₽ / мес" }

            XCTAssertEqual(installmentsInfo.monthlyPayment.label, "1 167 ₽ / мес")
        }
    }

    func testGoToCheckoutFromWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5215")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("Корзина")
        Allure.addTitle("Переход по виджету в чекаут")

        var cartPage: CartPage!
        var installmentsInfo: CartPage.InstallmentsInfoCellPage!
        var checkoutPage: CheckoutPage!
        var checkoutInstallmentsCell: CheckoutPage.InstallmentsCell!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrdersState()
            setupCartStateForCheckout()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие виджета рассрочки".ybm_run { _ in
            installmentsInfo = cartPage.installments.installmentsInfo
            cartPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: installmentsInfo.element,
                withVelocity: .slow
            )

            XCTAssert(installmentsInfo.element.isVisible, "Виджет рассрочки не виден")
            XCTAssertEqual(installmentsInfo.monthlyPayment.label, "2 333 ₽ / мес")
        }

        "Свайпаем селектор вниз и проверяем новые срок и ежемесячный платёж".ybm_run { _ in
            installmentsInfo.selector.element.swipe(from: .zero, to: .init(dx: 0, dy: 20), withVelocity: .slow)
            ybm_wait { installmentsInfo.monthlyPayment.label != "2 333 ₽ / мес" }

            XCTAssertEqual(installmentsInfo.selector.selectedCell.term.label, "6 мес")
            XCTAssertEqual(installmentsInfo.monthlyPayment.label, "4 665 ₽ / мес")
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = installmentsInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты - в рассрочку от Тинькофф".ybm_run { _ in
            let paymentMethodCell = checkoutPage.paymentMethodCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentMethodCell.element)

            XCTAssert(paymentMethodCell.element.isVisible, "Способ оплаты не виден")
            XCTAssertEqual(paymentMethodCell.title.label, "В рассрочку от Тинькофф")
        }

        "Проверяем лейбл названия срока рассрочки".ybm_run { _ in
            checkoutInstallmentsCell = checkoutPage.installmentsCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutInstallmentsCell.element)
            let periodLabel = checkoutInstallmentsCell.periodTitle

            XCTAssert(periodLabel.isVisible, "Лейбл названия срока рассрочки не виден")
            XCTAssertEqual(periodLabel.label, "Срок рассрочки:")
        }

        "Проверяем выбранный срок рассрочки на селекторе".ybm_run { _ in
            let selectedTerm = checkoutInstallmentsCell.selector.selectedCell.term

            XCTAssert(selectedTerm.isVisible, "Срок рассрочки не виден")
            XCTAssertEqual(selectedTerm.label, "6 мес")
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            let payment = checkoutInstallmentsCell.monthlyPayment

            XCTAssert(payment.isVisible, "Ежемесячный платёж не виден")
            XCTAssertEqual(payment.label, "4 665 ₽ / мес")
        }

        "Проверяем лейбл \"Итого\" в саммари с рассрочкой".ybm_run { _ in
            let summaryTotalCell = checkoutPage.summaryTotalCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: summaryTotalCell.element)

            XCTAssertEqual(summaryTotalCell.details.label, "4 665 ₽ × 6 мес")
            XCTAssertEqual(summaryTotalCell.title.label, "Итого")
        }

        "Проверяем кнопку оформления оплаты в рассрочку".ybm_run { _ in
            let paymentButton = checkoutPage.paymentButton
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentButton.element)

            XCTAssertEqual(paymentButton.title.label, "Оформить рассрочку")
        }

        "Проверяем лейбл юридической информации".ybm_run { _ in
            let termsDisclaimerCell = checkoutPage.termsDisclaimerCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: termsDisclaimerCell.element)

            XCTAssert(termsDisclaimerCell.element.isVisible, "Лейбл юридической информации не виден")
            XCTAssertEqual(
                termsDisclaimerCell.title.label,
                "Нажимая «Оформить рассрочку», вы соглашаетесь с условиями использования сервиса Яндекс.Маркет"
            )
        }
    }
}

// MARK: - Helper Methods

private extension InstallmentsCartTests {

    func setupCartState() {
        var cartState = CartState()
        cartState.setCartStrategy(with: [.protein])
        cartState.setUserOrdersState(with: .installments)
        stateManager?.setState(newState: cartState)
    }

    func setupCartStateForCheckout() {
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

private extension InstallmentsCartTests {

    enum Constants {
        static let shopLabel = "145_0"
        static let terms = ["6 мес", "12 мес", "24 мес"]
        static let monthlyPayments = ["по 4 665 ₽", "по 2 333 ₽", "по 1 167 ₽"]
    }
}
