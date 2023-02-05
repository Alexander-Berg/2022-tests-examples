import MarketUITestMocks
import XCTest

class CheckoutAviaDeliveryTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCheapAviaDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5256")
        Allure.addEpic("Авиадоставка")
        Allure.addFeature("Выбор самой быстрой доставки")
        Allure.addTitle("Платная доставка")

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            setupState(experiment: .cheapAviaDelivery, options: .freeAvia)
        }

        checkHint(
            initialOption: "чт, 9 декабря, 0 ₽",
            finalOption: "ср, 8 декабря, 100 ₽",
            betterOptionHint: "Можно быстрее — 8 декабря за 100 ₽"
        )
    }

    func testFastFreeAviaDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5259")
        Allure.addEpic("Авиадоставка")
        Allure.addFeature("Выбор самой дешевой доставки")
        Allure.addTitle("Бесплатная доставка")

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            setupState(experiment: .fastAviaDelivery, options: .freeAvia)
        }

        checkHint(
            initialOption: "ср, 8 декабря, 100 ₽",
            finalOption: "чт, 9 декабря, 0 ₽",
            betterOptionHint: "Можно бесплатно с 9 декабря"
        )
    }

    func testFastPaidAviaDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5258")
        Allure.addEpic("Авиадоставка")
        Allure.addFeature("Выбор самой дешевой доставки")
        Allure.addTitle("Платная доставка")

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            setupState(experiment: .fastAviaDelivery, options: .paidAvia)
        }

        checkHint(
            initialOption: "ср, 8 декабря, 150 ₽",
            finalOption: "чт, 9 декабря, 100 ₽",
            betterOptionHint: "Можно дешевле — с 9 декабря за 100 ₽"
        )
    }

    func checkHint(
        initialOption: String,
        finalOption: String,
        betterOptionHint: String
    ) {
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var betterOption: XCUIElement!

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем, что выбран верный тариф доставки (дешёвый / быстрый)".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).defaultServiceSelectorCell.element)
            wait(forVisibilityOf: checkoutPage.dateSelectorCell(at: 0).element)

            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, initialOption)
            XCTAssertTrue(checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.isSelected)
        }

        "Проверяем подсказку под пикером про более быструю / дешёвую опцию доставки".ybm_run { _ in
            betterOption = checkoutPage.betterDeliveryOptionCell
            wait(forVisibilityOf: betterOption)
            XCTAssertEqual(betterOption.label, betterOptionHint)
        }

        "Проверяем, что при нажатии на подсказку дата доставки изменилась".ybm_run { _ in
            betterOption.tap()
            wait(forInvisibilityOf: betterOption)

            wait(forVisibilityOf: checkoutPage.dateSelectorCell(at: 0).element)
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)

            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, finalOption)
            XCTAssertTrue(checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.isSelected)
        }
    }

    // MARK: - Helper Methods

    typealias Experiment = ResolveBlueStartup.Experiment
    typealias UserOrderOptions = ResolveUserOrderOptions.UserOrderOptions

    private func setupState(experiment: Experiment, options: CartItem) {
        setupExperiment(experiment)
        setupUserAddress()
        setupOrderOptions(with: options)
    }

    private func setupExperiment(_ experiment: Experiment) {
        var defaultState = DefaultState()
        enable(toggles: FeatureNames.checkoutDeliverySlotsRedesign)
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        defaultState.setExperiments(experiments: [experiment])
        stateManager?.setState(newState: defaultState)
    }

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions(with cartItem: CartItem) {
        var cartState = CartState()
        let options = UserOrderOptions(
            region: .moscow,
            summary: .basic,
            shops: [cartItem]
        )
        cartState.setUserOrdersState(with: options)
        stateManager?.setState(newState: cartState)
    }
}
