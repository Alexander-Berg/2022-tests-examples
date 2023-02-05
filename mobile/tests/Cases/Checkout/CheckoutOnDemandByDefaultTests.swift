import MarketUITestMocks
import UIUtils
import XCTest

final class CheckoutOnDemandByDefaultTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOnDemandSelectedByDefault() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4809")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Он-деманд по умолчанию")
        Allure.addTitle("Выбрана доставка по клику")

        var rootPage: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrderOptions()
            setupOrderDetails(with: .applePay)
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

        "Проверяем, что отображаются два способа доставки: по клику и в выбранный интервал".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).defaultServiceSelectorCell.element)
        }

        "Проверяем, что по умолчанию выбрана доставка по клику".ybm_run { _ in
            XCTAssertTrue(checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.isSelected)
            XCTAssertTrue(checkoutPage.dateSelectorCell(at: 0).element.isVisible)
        }
    }

    func testOnDemandNotSelectedByDefault() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4846")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Он-деманд по умолчанию")
        Allure.addTitle("Выбрана курьерская доставка в интервал")

        var rootPage: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrderOptions()
            setupOrderDetails(with: .cardOnDelivery)
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

        "Проверяем, что отображаются два способа доставки: по клику и в выбранный интервал".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).defaultServiceSelectorCell.element)
        }

        "Проверяем, что по умолчанию выбрана доставка в выбранный интервал".ybm_run { _ in
            XCTAssertTrue(checkoutPage.deliverySlotsCell(at: 0).defaultServiceSelectorCell.isSelected)
            XCTAssertTrue(checkoutPage.dateSelectorCell(at: 0).element.isVisible)
        }
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .onDemandAndService)
        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails(with payment: Order.Payment) {
        var orderState = OrdersState()
        let order = SimpleOrder(
            status: .delivery,
            payment: payment,
            delivery: .init(deliveryPartnerType: .yandex, type: .service)
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all])
        stateManager?.setState(newState: orderState)
    }

}
