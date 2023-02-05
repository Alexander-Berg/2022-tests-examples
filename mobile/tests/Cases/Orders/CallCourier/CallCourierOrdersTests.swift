import MarketUITestMocks
import XCTest

final class CallCourierOrdersTests: LocalMockTestCase {

    // MARK: - Public

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOutletTimeIntervalSubstatus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5927")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5930")
        Allure.addEpic("Широкие слота")
        Allure.addFeature("Кнопка вызвать курьера")
        Allure.addTitle("Проверяем статус и наличие кнопки")

        var ordersPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        let orderMapper = HandlerMapper(
            orders: [.init(id: orderId, status: .delivery)],
            orderState: [.callCourier(id: orderId)]
        )

        enable(toggles: FeatureNames.orderStatusTextFromFapiFeature, FeatureNames.groupedOrdersFeature)

        "Мокаем состояния".ybm_run { _ in
            setupState(with: orderMapper)
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersPage = goToOrdersListPage()
        }

        "Проверяем статус заказа и кнопку вызова курьера".ybm_run { _ in
            XCTAssertEqual(
                ordersPage.substatus(orderId: orderId).label,
                "Курьер приедет в течении 30 минут"
            )
            XCTAssertEqual(
                ordersPage.status(orderId: orderId).label,
                "Готовы привезти заказ"
            )
            XCTAssertEqual(
                ordersPage.callCourierButton(orderId: orderId).label,
                "Вызвать курьера за 59р"
            )
            XCTAssertTrue(ordersPage.callCourierButton(orderId: orderId).isEnabled)
        }

        "Переходим в детали заказа".ybm_run { _ in
            let detailsButton = ordersPage.detailsButton(orderId: orderId)
            ordersPage.element.ybm_swipe(toFullyReveal: detailsButton.element)
            orderDetailsPage = detailsButton.tap()
        }

        "Проверяем статус заказа и кнопку вызова курьера".ybm_run { _ in
            XCTAssertEqual(
                orderDetailsPage.substatus.label,
                "Курьер приедет в течении 30 минут"
            )
            XCTAssertEqual(
                orderDetailsPage.status.label,
                "Готовы привезти заказ"
            )
            XCTAssertEqual(
                orderDetailsPage.callCourierButton.label,
                "Вызвать курьера за 59р"
            )
            XCTAssertTrue(orderDetailsPage.callCourierButton.isEnabled)
        }

    }

    // MARK: - Helper Methods and Nested Types

    typealias HandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

    private func setupState(with mapper: HandlerMapper) {
        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: mapper, for: [.all, .byId, .recent(withGrades: true)])
        stateManager?.setState(newState: orderState)
    }
}
