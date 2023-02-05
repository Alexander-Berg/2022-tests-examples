import MarketUITestMocks
import UIUtils
import XCTest

final class FinishedMultiorderAwaitPaymentTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testAwaitPaymentFinishedMultiorderOpensCorrectly() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4809")
        Allure.addEpic("Спасибка")
        Allure.addFeature("Резиновый траст")
        Allure.addTitle("Заказ в статусе подстверждения оформляется корректно")

        var rootPage: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var finishPage: FinishMultiorderPage!

        disable(toggles: FeatureNames.paymentSDK, FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrderOptions()
            setupOrderDetails()
            setupCheckoutState()
        }

        "Открываем корзину".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: rootPage)
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Подтверждаем заказ".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            finishPage = checkoutPage.paymentButton.tap()
        }

        "Ждем открытия спасибки".ybm_run { _ in
            wait(forVisibilityOf: finishPage.element)
        }

        "Проверяем правильность отображения спасибки".ybm_run { _ in
            XCTAssertEqual(finishPage.title.label, "Заказ ожидает подтверждения оплаты")
            XCTAssertFalse(finishPage.warning.isVisible)
            XCTAssertFalse(finishPage.payButton.isVisible)
            XCTAssertFalse(finishPage.choosePaymentButton.isVisible)
            swipeAndCheck(
                page: finishPage.element,
                element: finishPage.paymentStatus(at: 0),
                check: { XCTAssertEqual($0.label, "Ожидает подтверждения оплаты") }
            )
        }

    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias UserOrderOptions = ResolveUserOrderOptions.UserOrderOptions

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        userState.setContactsState(contacts: [.basic])

        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .prepaid)

        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails() {
        var orderState = OrdersState()

        let order = SimpleOrder(
            status: .unpaid,
            substatus: .awaitPayment,
            delivery: .init(deliveryPartnerType: .yandex, type: .service)
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])

        stateManager?.setState(newState: orderState)
    }

    private func setupCheckoutState() {
        var checkoutState = CheckoutState()
        checkoutState.setUserOrderState(orderResponse: OrderResponse(shops: [.awaitPayment]))
        stateManager?.setState(newState: checkoutState)
    }
}
