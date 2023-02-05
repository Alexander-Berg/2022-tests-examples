import MarketUITestMocks
import XCTest

final class FinishedMultiorderDeliveryTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOutletTimeIntervalDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4860")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Время доставки в ПВЗ")
        Allure.addTitle("Время доставки в составе заказа")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Мокаем состояние".ybm_run { _ in
            setupEnvironment()
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
                "Заказ 57104420 11 июля доставим в пункт выдачи к 14:00"
            )
        }
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupEnvironment() {
        disable(toggles: FeatureNames.checkoutPresetsRedesign, FeatureNames.cartRedesign)
        setupUserState()
        setupOrderOptions()
        setupOrderDetails()
    }

    private func setupUserState() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        userState.setContactsState(contacts: [.basic])
        userState.setFavoritePickupPoints(favoritePickups: [.rublevskoye])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .outlet)
        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails() {
        var orderState = OrdersState()
        let order = SimpleOrder(
            status: .delivered,
            payment: .cashOnDelivery,
            outlet: .rublevskoye,
            delivery: .init(deliveryPartnerType: .yandex, type: .outlet)
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all])
        orderState.setOutlet(outlets: [.rublevskoye])
        stateManager?.setState(newState: orderState)
    }
}
