import MarketUITestMocks
import XCTest

final class InstallmentsOrderTests: LocalMockTestCase {

    // MARK: - Public

    func testOrderPaymentMethod() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5251")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Отображение способа оплаты в списке заказов и в деталях заказа")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        "Мокаем состояние".run {
            setupOrdersState()
        }

        "Переходим в список заказов".run {
            ordersListPage = goToOrdersListPage()
        }

        "Проверяем сумму заказа с лейблом рассрочки".run {
            let total = ordersListPage.total(orderId: Constants.orderId)
            ybm_wait(forVisibilityOf: [total])

            XCTAssertEqual(total.label, "Итого 9 081 ₽, в рассрочку от Тинькофф")
        }

        "Переходим в детали заказа".run {
            orderDetailsPage = ordersListPage.detailsButton(orderId: Constants.orderId).tap()
            ybm_wait(forVisibilityOf: [orderDetailsPage.paymentType.element])
        }

        "Проверяем способ оплаты".run {
            XCTAssertEqual(orderDetailsPage.paymentType.value.label, "В рассрочку от Тинькофф")
        }
    }
}

// MARK: - Helper Methods

private extension InstallmentsOrderTests {

    func setupOrdersState() {
        let orderMapper = Order.Mapper(
            id: Constants.orderId,
            status: .delivered,
            substatus: .received,
            payment: .installments,
            delivery: .service
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(
            mapper: OrdersState.UserOrdersHandlerMapper(orders: [orderMapper], sku: .empty),
            for: [.all, .byId]
        )

        stateManager?.setState(newState: orderState)
    }
}

// MARK: - Nested Types

private extension InstallmentsOrderTests {

    enum Constants {
        static let orderId = "4815230"
    }
}
