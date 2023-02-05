import MarketUITestMocks
import XCTest

final class BNPLSKUTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testBNPLWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4816")
        Allure.addEpic("BNPL")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход в КМ доступного товара")

        var skuPage: SKUPage!
        var bnplPlanConstructor: BNPLPlanConstructorPage!

        "Настраиваем FT".run {
            enable(toggles: FeatureNames.BNPL)
        }

        "Мокаем состояние".run {
            setupSKUInfoState()
        }

        "Открываем КМ".run {
            skuPage = goToDefaultSKUPage()
            wait(forVisibilityOf: skuPage.element)
        }

        "Проверяем наличие лейбла \"или частями\" в байбоксе".run {
            let bnplLabel = skuPage.price.bnplLabel
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: bnplLabel)

            XCTAssert(bnplLabel.isVisible, "Лейбл \"или частями\" не виден")
            XCTAssertTrue(bnplLabel.label.contains("или частями"))
        }

        "Проверяем наличие виджета и графика платежей".run {
            bnplPlanConstructor = skuPage.bnplPlanConstructor
            let planView = bnplPlanConstructor.planView
            skuPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: planView,
                inset: UIEdgeInsets(top: 0, left: 0, bottom: 70, right: 0),
                withVelocity: .slow
            )

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

    func testBNPLInfoButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4818")
        Allure.addEpic("BNPL")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход по ссылке \"Подробнее\"")

        var skuPage: SKUPage!
        var bnplPlanConstructor: BNPLPlanConstructorPage!
        var detailsButton: BNPLPlanConstructorPage.DetailsButton!

        "Настраиваем FT".run {
            enable(toggles: FeatureNames.BNPL)
        }

        "Мокаем состояние".run {
            setupSKUInfoState()
        }

        "Открываем КМ".run {
            skuPage = goToDefaultSKUPage()
            wait(forVisibilityOf: skuPage.element)
        }

        "Проверяем наличие лейбла \"или частями\" в байбоксе".run {
            let bnplLabel = skuPage.price.bnplLabel
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: bnplLabel)

            XCTAssert(bnplLabel.isVisible, "Лейбл \"или частями\" не виден")
            XCTAssertTrue(bnplLabel.label.contains("или частями"))
        }

        "Проверяем наличие виджета и графика платежей".run {
            bnplPlanConstructor = skuPage.bnplPlanConstructor
            let planView = bnplPlanConstructor.planView
            skuPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: planView,
                inset: UIEdgeInsets(top: 0, left: 0, bottom: 70, right: 0),
                withVelocity: .slow
            )

            XCTAssert(planView.isVisible, "График платежей не виден")
        }

        "Проверяем кнопку подробнее о BNPL".run {
            detailsButton = bnplPlanConstructor.detailsButton
            skuPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: detailsButton.element,
                inset: UIEdgeInsets(top: 0, left: 0, bottom: 70, right: 0),
                withVelocity: .slow
            )

            XCTAssert(detailsButton.element.isVisible, "Кнопки подробнее о BNPL не видно")
            XCTAssertEqual(detailsButton.element.label, "Подробнее")
        }

        "Нажимаем кнопку и переходим на экран с подробностями о BNPL".run {
            let webView = detailsButton.tap()
            wait(forVisibilityOf: webView.navigationBar.title)

            XCTAssertEqual(
                webView.navigationBar.title.label,
                "Оплата покупок частями в рассрочку в интернет-магазинах — Яндекс.Сплит"
            )
        }
    }

    func testGoToCheckoutWithoutAddingToCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4814")
        Allure.addEpic("BNPL")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход в чекаут по виджету без добавления в корзину")

        var skuPage: SKUPage!
        var bnplPlanConstructor: BNPLPlanConstructorPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)
            enable(toggles: FeatureNames.BNPL)
        }

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrdersState()
            setupSKUInfoState()
            setupCartState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Проверяем наличие виджета и графика платежей".ybm_run { _ in
            bnplPlanConstructor = skuPage.bnplPlanConstructor
            let planView = bnplPlanConstructor.planView
            skuPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: planView,
                inset: UIEdgeInsets(top: 0, left: 0, bottom: 70, right: 0),
                withVelocity: .slow
            )

            XCTAssert(planView.isVisible, "График платежей не виден")
        }

        "Проверяем первый платеж".ybm_run { _ in
            let deposit = bnplPlanConstructor.deposit

            XCTAssert(deposit.isVisible, "Лейбл первого платежа не виден")
            XCTAssertEqual(deposit.label, "759 ₽ сегодня")
        }

        "Проверяем остальные платежи".ybm_run { _ in
            let payments = bnplPlanConstructor.payments

            XCTAssert(payments.isVisible, "Лейбл остальных платежей не виден")
            XCTAssertEqual(payments.label, "и 2 274 ₽ потом")
        }

        "Проверяем кнопку \"Оформить\"".ybm_run { _ in
            XCTAssert(bnplPlanConstructor.checkoutButton.element.isVisible, "Кнопки \"Оформить\" не видно")
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = bnplPlanConstructor.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты картой".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            XCTAssert(checkoutPage.paymentMethodCell.element.isVisible)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }

        "Проверяем, что переключатель BNPL включён".ybm_run { _ in
            let bnplSwitch = checkoutPage.bnplSwitch
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: bnplSwitch)

            XCTAssert(bnplSwitch.isEnabled, "Переключатель BNPL неактивен")
            XCTAssert(bnplSwitch.isOn, "Переключатель BNPL выключен")
        }

        "Проверяем видимость плана платежей".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.bnplPlanCell.element)

            XCTAssert(checkoutPage.bnplPlanCell.planView.isVisible, "План платежей не виден")
        }

        "Проверяем кнопку оформления оплаты частями".ybm_run { _ in
            let paymentButton = checkoutPage.paymentButton
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentButton.element)

            XCTAssertEqual(paymentButton.title.label, "Оформить оплату частями")
        }
    }
}

// MARK: - Helper Methods

private extension BNPLSKUTests {

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

    func setupSKUInfoState() {
        let bnplOffer = modify(FAPIOffer.default) {
            $0.bnplAvailable = true
            $0.financialProductPriority = ["BNPL"]
        }

        var skuState = SKUInfoState()
        skuState.setSkuInfoState(offer: bnplOffer)
        skuState.setBNPLPlanState(with: .default)
        stateManager?.setState(newState: skuState)
    }

    func setupCartState() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .bnpl)
        stateManager?.setState(newState: cartState)
    }
}
