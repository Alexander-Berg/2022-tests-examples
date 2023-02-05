import MarketUITestMocks
import XCTest

final class EstimatedDeliveryOrderTest: LocalMockTestCase {

    // MARK: - Public

    func testOrderCancelationWithCancelAvailable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-6306")
        Allure.addEpic("Товары на заказ. Детали заказа")
        Allure.addFeature("Отображение ориентировочной даты доставки и кнопки отмены заказа.")
        Allure.addTitle("Отмена заказа доступна в политике отмены.")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        let orderMapper = HandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .processing,
                delivery: Delivery(
                    fromDate: getDate(withDateFormat: "dd-MM-yyyy", date: Date()),
                    toDate: getDate(withDateFormat: "dd-MM-yyyy", date: Date()),
                    fromTime: "12:00",
                    toTime: "15:00",
                    estimated: true
                ),
                cancelPolicy: Order.CancelPolicy(notAvailable: false)
            )
        ])

        "Мокаем состояния".ybm_run { _ in
            setupState(with: orderMapper)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Проверяем что кнопки отмены заказа нет".ybm_run { _ in
            XCTAssertFalse(ordersListPage.element.ybm_swipeCollectionView(
                toFullyReveal: ordersListPage.cancellationButton(orderId: orderId),
                maxNumberOfSwipes: 3,
                needCallError: false
            ))
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем 'Ориентировочно' в дате доставке".ybm_run { _ in
            let todayDateString = DateFormatter.dayMonth.string(from: Date())
            XCTAssertEqual(
                orderDetailsPage.deliveryDate.value.label,
                "Ориентировочно \(todayDateString)"
            )
        }

        "Проверяем что кнопка отмены заказа есть".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.cancellationButton.element)
        }
    }

    func testOrderCancelationWithCancelUnavailable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-6306")
        Allure.addEpic("Товары на заказ. Детали заказа")
        Allure.addFeature("Отображение ориентировочной даты доставки и кнопки отмены заказа.")
        Allure.addTitle("Отмена заказа недоступна в политике отмены.")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        let orderMapper = HandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .processing,
                delivery: Delivery(
                    fromDate: getDate(withDateFormat: "dd-MM-yyyy", date: Date()),
                    toDate: getDate(withDateFormat: "dd-MM-yyyy", date: Date()),
                    fromTime: "12:00",
                    toTime: "15:00",
                    estimated: true
                ),
                cancelPolicy: Order.CancelPolicy(notAvailable: true)
            )
        ])

        "Мокаем состояния".ybm_run { _ in
            setupState(with: orderMapper)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Проверяем что кнопки отмены заказа нет".ybm_run { _ in
            XCTAssertFalse(ordersListPage.element.ybm_swipeCollectionView(
                toFullyReveal: ordersListPage.cancellationButton(orderId: orderId),
                maxNumberOfSwipes: 3,
                needCallError: false
            ))
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем 'Ориентировочно' в дате доставке".ybm_run { _ in
            let todayDateString = DateFormatter.dayMonth.string(from: Date())
            XCTAssertEqual(
                orderDetailsPage.deliveryDate.value.label,
                "Ориентировочно \(todayDateString)"
            )
        }

        "Проверяем что кнопки отмены заказа нет".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.element.ybm_swipeCollectionView(
                toFullyReveal: orderDetailsPage.cancellationButton.element,
                maxNumberOfSwipes: 5,
                needCallError: false
            ))
        }
    }

    // MARK: - Helper Methods

    private typealias HandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    private typealias SimpleOrder = Order.Mapper

    private func setupState(with mapper: HandlerMapper) {
        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: mapper, for: [.all, .byId])

        stateManager?.setState(newState: orderState)
    }

    private func getDate(withDateFormat dateFormat: String, date: Date) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = dateFormat
        dateFormatter.locale = Locale(identifier: "ru")
        return dateFormatter.string(from: date)
    }
}
