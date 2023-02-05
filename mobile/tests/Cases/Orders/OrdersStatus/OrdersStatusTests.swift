import MarketUITestMocks
import XCTest

final class OrdersStatusTests: LocalMockTestCase {

    // MARK: - Public

    func testOrderStatusDeliveryUserReceived() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3117")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Уже у вас")
        Allure.addTitle("Статус в трекинге 49 - Уже у вас")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        let orderMapper = HandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .delivered,
                    substatus: .received,
                    delivery: Delivery(
                        type: .service
                    )
                )
            ],
            sku: .empty
        )

        "Мокаем состояния".ybm_run { _ in
            setupState(with: orderMapper)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
        }

        "Проверяем статус заказа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                ordersListPage.status(orderId: orderId).label == "Уже у вас"
            })
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
        }

        "Проверяем статус заказа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { orderDetailsPage.status.label == "Уже у вас" })
        }

        "Проверяем активную кнопку \"Вернуть заказ\"".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.refundButton)
            XCTAssertEqual(orderDetailsPage.refundButton.label, "Вернуть заказ")
            XCTAssertTrue(orderDetailsPage.refundButton.isVisible)
        }
    }

    func testOutletTimeIntervalSubstatus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4862")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Время доставки в ПВЗ")
        Allure.addTitle("Подстатус заказа")

        var ordersListPage: OrdersListPage!
        let orderId = "4815230"

        let todayDate = getDate(withDateFormat: "dd-MM-yyyy", date: Date())
        let orderMapper = HandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .processing,
                delivery: Delivery(
                    deliveryPartnerType: .yandex,
                    fromDate: todayDate,
                    toDate: todayDate,
                    fromTime: "12:00",
                    toTime: "15:00",
                    type: .outlet
                )
            )
        ])

        "Мокаем состояния".ybm_run { _ in
            setupState(with: orderMapper)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Проверяем подстатус заказа".ybm_run { _ in
            let orderSubstatus = ordersListPage.substatus(orderId: orderId)
            let todayDateString = DateFormatter.dayMonth.string(from: Date())
            XCTAssertEqual(orderSubstatus.label, "Доставим в пункт выдачи \(todayDateString) к 15:00")
        }
    }

    func testOrderStatusUnpaidAwaitPayment() {
        Allure.addTestPalmLink("")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Резиновый траст")
        Allure.addTitle("Заказ в статусе 'Ожидает подтверждения оплаты'")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        let orderMapper = HandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .unpaid,
                substatus: .awaitPayment
            )
        ])

        "Мокаем состояния".ybm_run { _ in
            setupState(with: orderMapper)
        }

        "Переходим в список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
        }

        "Проверяем статус заказа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                ordersListPage.status(orderId: orderId).label == "Ожидает подтверждения оплаты"
            })
            XCTAssertFalse(ordersListPage.payButton(orderId: orderId).element.isVisible)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
        }

        "Проверяем статус заказа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { orderDetailsPage.status.label == "Ожидает подтверждения оплаты" })
        }

        "Проверяем активную кнопку \"Вернуть заказ\"".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.cancellationButton.element)
            XCTAssertFalse(orderDetailsPage.payButton(orderId: orderId).isVisible)
        }
    }

    // MARK: - Helper Methods

    typealias HandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

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
