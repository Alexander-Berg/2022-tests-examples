import MarketUITestMocks
import XCTest

class OrderCourierTrackingTest: LocalMockTestCase {

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias CashbackStatus = Order.Mapper.SimpleCashback.Status

    func getDefaultState(orderId: String) -> OrdersState {
        let orderMapper = OrdersHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .delivery
            )
        ])
        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])
        orderState.setCourierTracking(orderCourierTracking: .init(courierTracking: [
            .init(
                trackingUrl: "https://touch.bluemarket.fslb.beru.ru/tracking/590e760d13f64aa199c7238204f124f6",
                id: "590e760d13f64aa199c7238204f124f6",
                orderId: orderId
            )
        ]))

        return orderState
    }

    // MARK: - Public

    func testOrderCourierTrackingInOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3296")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Отслеживание курьера")
        Allure.addTitle("Из деталей заказа")

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            var state = getDefaultState(orderId: orderId)
            state.setAvailabilityOrderOption(mapper: .makeCourierForOrders(ids: [orderId]))
            stateManager?.setState(newState: state)
        }

        "Переходим в список заказов".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Переходим в детали заказа и проверяем кнопку 'Отследить'".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.courierTrackingButton)
            XCTAssertEqual(orderDetailsPage.courierTrackingButton.label, "Отследить курьера")
        }

        "Нажимаем кнопку 'Отледить' и проверяем открытие WebView".ybm_run { _ in
            orderDetailsPage.courierTrackingButton.tap()
            wait(forVisibilityOf: WebViewPage.current.element)
        }

        "Закрываем WebView и проверяем открытие деталей заказа".ybm_run { _ in
            WebViewPage.current.navigationBar.closeButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }
    }

    func testOrderCourierTrackingAbsenceInOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3304")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Отслеживание курьера")
        Allure.addTitle("Кнопки нет")

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            var state = getDefaultState(orderId: orderId)
            state.setAvailabilityOrderOption(mapper: .makeNoneForOrders(ids: [orderId]))
            stateManager?.setState(newState: state)
        }

        "Открываем детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверяем отсутствие кнопки 'Отследить'".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.courierTrackingButton.exists)
        }
    }

}
