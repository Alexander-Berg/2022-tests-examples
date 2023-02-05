import MarketUITestMocks
import XCTest
import YandexPlusHome

final class FinishedMultiorderUniqueOrderTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testEstimatedDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-6029")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Товар с долгой доставкой")
        Allure.addTitle("Отображение ориентировочных сроков доставки")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Мокаем состояние".ybm_run { _ in
            disable(toggles: FeatureNames.checkoutPresetsRedesign)
            enable(toggles: FeatureNames.cartRedesign)
            setupUserState()
            setupOrderOptions()
            setupOrderDetails(estimated: true, cancelPolicy: nil)
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

        "Проверяем текст с датой и временем доставки в блоке Состав заказа".ybm_run { _ in
            XCTAssertEqual(
                finishMultiorderPage.deliveryStatus().label,
                "Ориентировочная дата доставки — 11 июля. Продавец согласует с вами точную дату и время."
            )
        }
    }

    func testUniqueOrderCancelInformation() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-6468")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Товар на заказ с политикой отмены")
        Allure.addTitle("Отображение информации об условиях отмены заказа")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Мокаем состояние".ybm_run { _ in
            disable(toggles: FeatureNames.checkoutPresetsRedesign)
            enable(toggles: FeatureNames.cartRedesign)
            setupUserState()
            setupOrderOptions()
            setupOrderDetails(estimated: false, cancelPolicy: Constants.cancelPolicy)
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

        "Проверяем текст с датой и временем доставки в блоке Состав заказа".ybm_run { _ in
            XCTAssertEqual(
                finishMultiorderPage.deliveryStatus().label,
                """
                Заказ 57104420 11 июля доставим в пункт выдачи к 14:00
                Отменить заказ можно до 6 июня включительно.
                """
            )
        }
    }

    func testUniqueOrderCancelInformationEstimatedDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5964")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Товар на заказ с политикой отмены и долгой доставкой")
        Allure.addTitle("Отображение ориентировочных сроков доставки и информации об условиях отмены заказа")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Мокаем состояние".ybm_run { _ in
            disable(toggles: FeatureNames.checkoutPresetsRedesign)
            enable(toggles: FeatureNames.cartRedesign)
            setupUserState()
            setupOrderOptions()
            setupOrderDetails(estimated: true, cancelPolicy: Constants.cancelPolicy)
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

        "Проверяем текст с датой и временем доставки в блоке Состав заказа".ybm_run { _ in
            XCTAssertEqual(
                finishMultiorderPage.deliveryStatus().label,
                """
                Ориентировочная дата доставки — 11 июля. Продавец согласует с вами точную дату и время.
                Отменить заказ можно до 6 июня включительно.
                """
            )
        }
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupUserState() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        userState.setContactsState(contacts: [.basic])
        userState.setFavoritePickupPoints(favoritePickups: [.rublevskoye])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .estimatedOutlet)
        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails(estimated: Bool, cancelPolicy: Order.CancelPolicy?) {
        var orderState = OrdersState()
        let order = SimpleOrder(
            id: "57104420",
            status: .delivered,
            payment: .cashOnDelivery,
            outlet: .rublevskoye,
            delivery: .init(deliveryPartnerType: .yandex, type: .outlet, estimated: estimated),
            cancelPolicy: cancelPolicy
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byIds, .byId])
        orderState.setOutlet(outlets: [.rublevskoye])
        stateManager?.setState(newState: orderState)
    }

    // MARK: - Constants

    private enum Constants {
        static let cancelPolicy = Order.CancelPolicy(timeUntilExpiration: "2022-06-06")
    }
}
