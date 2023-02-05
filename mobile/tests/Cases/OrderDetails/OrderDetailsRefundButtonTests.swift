import MarketUITestMocks
import XCTest

final class OrderDetailsRefundButtonTests: LocalMockTestCase {

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setState(orderId: String) {
        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .delivered,
                    substatus: .received,
                    rgb: "WHITE",
                    delivery: .init(
                        deliveryPartnerType: .shop,
                        type: .service
                    )
                )
            ]
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])

        stateManager?.setState(newState: orderState)
    }

    // MARK: - Public

    func test_OpenOrderDetailsWithDeliveryUserReceivedStatus_RefundButtonExists() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-4017")
        Allure.addEpic("Возврат заказа")
        Allure.addFeature("Возврат из деталей")
        Allure.addTitle("Кнопка возврата на 49ЧП")

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Листаем вниз до наличия кнопки возврата и убеждаемся что она доступна".ybm_run { _ in
            orderDetailsPage.element.swipe(to: .down, untilVisible: orderDetailsPage.refundButton)
            XCTAssertTrue(orderDetailsPage.refundButton.isEnabled)
        }
    }

}
